package org.koharu.miyo.download.ui.worker

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import dagger.Reusable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.internal.closeQuietly
import okio.IOException
import okio.buffer
import okio.sink
import okio.use
import org.koharu.miyo.R
import org.koharu.miyo.core.image.BitmapDecoderCompat
import org.koharu.miyo.core.model.ids
import org.koharu.miyo.core.model.isLocal
import org.koharu.miyo.core.nativeio.NativeImageProbe
import org.koharu.miyo.core.network.MangaHttpClient
import org.koharu.miyo.core.network.imageproxy.ImageProxyInterceptor
import org.koharu.miyo.core.parser.MangaDataRepository
import org.koharu.miyo.core.parser.MangaRepository
import org.koharu.miyo.core.parser.MirrorSwitcher
import org.koharu.miyo.core.prefs.AppSettings
import org.koharu.miyo.core.util.MimeTypes
import org.koharu.miyo.core.util.Throttler
import org.koharu.miyo.core.util.ext.MimeType
import org.koharu.miyo.core.util.ext.awaitFinishedWorkInfosByTag
import org.koharu.miyo.core.util.ext.awaitUpdateWork
import org.koharu.miyo.core.util.ext.awaitWorkInfosByTag
import org.koharu.miyo.core.util.ext.deleteAwait
import org.koharu.miyo.core.util.ext.deleteWork
import org.koharu.miyo.core.util.ext.deleteWorks
import org.koharu.miyo.core.util.ext.ensureSuccess
import org.koharu.miyo.core.util.ext.getDisplayMessage
import org.koharu.miyo.core.util.ext.getWorkInputData
import org.koharu.miyo.core.util.ext.getWorkSpec
import org.koharu.miyo.core.util.ext.openSource
import org.koharu.miyo.core.util.ext.printStackTraceDebug
import org.koharu.miyo.core.util.ext.toFileOrNull
import org.koharu.miyo.core.util.ext.toMimeType
import org.koharu.miyo.core.util.ext.toMimeTypeOrNull
import org.koharu.miyo.core.util.ext.withTicker
import org.koharu.miyo.core.util.ext.writeAllCancellable
import org.koharu.miyo.core.util.progress.RealtimeEtaEstimator
import org.koharu.miyo.download.domain.DownloadProgress
import org.koharu.miyo.download.domain.DownloadState
import org.koharu.miyo.local.data.LocalMangaRepository
import org.koharu.miyo.local.data.LocalStorageCache
import org.koharu.miyo.local.data.LocalStorageChanges
import org.koharu.miyo.local.data.PageCache
import org.koharu.miyo.local.data.TempFileFilter
import org.koharu.miyo.local.data.input.LocalMangaParser
import org.koharu.miyo.local.data.output.LocalMangaOutput
import org.koharu.miyo.local.domain.MangaLock
import org.koharu.miyo.local.domain.model.LocalManga
import org.koitharu.kotatsu.parsers.exception.TooManyRequestExceptions
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.ifNullOrEmpty
import org.koitharu.kotatsu.parsers.util.mapToSet
import org.koitharu.kotatsu.parsers.util.requireBody
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koharu.miyo.download.domain.DownloadParallelismManager
import org.koharu.miyo.download.domain.SmartDownloadQueue
import org.koharu.miyo.download.domain.analytics.DownloadAnalytics
import org.koharu.miyo.reader.domain.PageLoader
import org.jsoup.HttpStatusException
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@HiltWorker
class DownloadWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	@MangaHttpClient private val okHttp: OkHttpClient,
	@PageCache private val cache: LocalStorageCache,
	private val localMangaRepository: LocalMangaRepository,
	private val mangaLock: MangaLock,
	private val mangaDataRepository: MangaDataRepository,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val settings: AppSettings,
	@LocalStorageChanges private val localStorageChanges: MutableSharedFlow<LocalManga?>,
	private val slowdownDispatcher: DownloadSlowdownDispatcher,
	private val imageProxyInterceptor: ImageProxyInterceptor,
	private val mirrorSwitcher: MirrorSwitcher,
	private val parallelismManager: DownloadParallelismManager,
	private val analytics: DownloadAnalytics,
	private val smartQueue: SmartDownloadQueue,
	private val nativeImageProbe: NativeImageProbe,
	notificationFactoryFactory: DownloadNotificationFactory.Factory,
) : CoroutineWorker(appContext, params) {

	private val task = DownloadTask(params.inputData)
	private val notificationFactory = notificationFactoryFactory.create(uuid = params.id, isSilent = task.isSilent)
	private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

	@Volatile
	private var lastPublishedState: DownloadState? = null
	private val currentState: DownloadState
		get() = checkNotNull(lastPublishedState)

	private val etaEstimator = RealtimeEtaEstimator()
	private val notificationThrottler = Throttler(400)

	override suspend fun doWork(): Result {
		setForeground(getForegroundInfo())
		val manga = mangaDataRepository.findMangaById(task.mangaId, withChapters = true) ?: return Result.failure()
		publishState(DownloadState(manga = manga, isIndeterminate = true).also { lastPublishedState = it })
		var keepCancellationNotification = false
		val downloadedIds = getDoneChapters(manga)
		return try {
			val pausingHandle = PausingHandle()
			if (task.isPaused) {
				pausingHandle.pause()
			}
			withContext(pausingHandle) {
				downloadMangaImpl(manga, task, downloadedIds)
			}
			Result.success(currentState.toWorkData())
		} catch (_: CancellationException) {
			keepCancellationNotification = true
			smartQueue.remove(task.mangaId)
			withContext(NonCancellable) {
				val notification = notificationFactory.create(currentState.copy(isStopped = true))
				notificationManager.notify(id.hashCode(), notification)
			}
			Result.failure(
				currentState.copy(eta = -1L, isStuck = false).toWorkData(),
			)
		} catch (e: Exception) {
			e.printStackTraceDebug()
			Result.failure(
				currentState.copy(
					error = e,
					errorMessage = e.getDisplayMessage(applicationContext.resources),
					eta = -1L,
					isStuck = false,
				).toWorkData(),
			)
		} finally {
			if (!keepCancellationNotification) {
				notificationManager.cancel(id.hashCode())
			}
		}
	}

	override suspend fun getForegroundInfo() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		ForegroundInfo(
			id.hashCode(),
			notificationFactory.create(lastPublishedState),
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
		)
	} else {
		ForegroundInfo(
			id.hashCode(),
			notificationFactory.create(lastPublishedState),
		)
	}

	private suspend fun downloadMangaImpl(
		subject: Manga,
		task: DownloadTask,
		excludedIds: Set<Long>,
	) {
		var manga = subject
		val chaptersToSkip = excludedIds.toMutableSet()
		val pausingReceiver = PausingReceiver(id, PausingHandle.current())
		mangaLock.withLock(manga) {
			ContextCompat.registerReceiver(
				applicationContext,
				pausingReceiver,
				PausingReceiver.createIntentFilter(id),
				ContextCompat.RECEIVER_NOT_EXPORTED,
			)
			val destination = localMangaRepository.getOutputDir(manga, task.destination)
			checkNotNull(destination) { applicationContext.getString(R.string.cannot_find_available_storage) }
			var output: LocalMangaOutput? = null
			try {
				if (manga.isLocal) {
					manga = localMangaRepository.getRemoteManga(manga)
						?: error("Cannot obtain remote manga instance")
				}
				val repo = mangaRepositoryFactory.create(manga.source)
				val mangaDetails = if (manga.chapters.isNullOrEmpty() || manga.description.isNullOrEmpty()) repo.getDetails(manga) else manga
				output = LocalMangaOutput.getOrCreate(
					root = destination,
					manga = mangaDetails,
					format = task.format ?: settings.preferredDownloadFormat,
				)
				val coverUrl = mangaDetails.largeCoverUrl.ifNullOrEmpty { mangaDetails.coverUrl }
				if (!coverUrl.isNullOrEmpty()) {
					downloadFile(coverUrl, destination, repo.source).let { file ->
						output.addCover(file, getMediaType(coverUrl, file))
						file.deleteAwait()
					}
				}
				val chapters = getChapters(mangaDetails, task)
				for ((chapterIndex, chapter) in chapters.withIndex()) {
					checkIsPaused()
					if (chaptersToSkip.remove(chapter.value.id)) {
						publishState(currentState.copy(downloadedChapters = currentState.downloadedChapters + 1))
						continue
					}
					val pages = runFailsafe {
						repo.getPages(chapter.value)
					} ?: continue
					val pageCounter = AtomicInteger(0)
					val chapterBytes = AtomicLong(0L)
					channelFlow {
						val pageParallelism = parallelismManager.resolveParallelism(
							sourceOverride = settings.downloadParallelism,
							isHighSpeedModeEnabled = settings.isHighSpeedModeEnabled,
						)
						val pageQueue = Channel<IndexedValue<MangaPage>>(capacity = pageParallelism)
						val workers = List(pageParallelism) {
							launch {
								for ((pageIndex, page) in pageQueue) {
									checkIsPaused()
									runFailsafe {
										val url = repo.getPageUrl(page)
										val file = cache[url]
											?: downloadFile(url, destination, repo.source)
										output.addPage(
											chapter = chapter,
											file = file,
											pageNumber = pageIndex,
											type = getMediaType(url, file),
										)
										chapterBytes.addAndGet(file.length())
										if (file.extension == "tmp") {
											file.deleteAwait()
										}
									}
									send(pageIndex)
								}
							}
						}
						try {
							for ((pageIndex, page) in pages.withIndex()) {
								checkIsPaused()
								pageQueue.send(IndexedValue(pageIndex, page))
							}
							pageQueue.close()
							workers.joinAll()
						} finally {
							pageQueue.close()
						}
					}.map {
						DownloadProgress(
							totalChapters = chapters.size,
							currentChapter = chapterIndex,
							totalPages = pages.size,
							currentPage = pageCounter.getAndIncrement(),
						)
					}.withTicker(2L, TimeUnit.SECONDS).collect { progress ->
						publishState(
							currentState.copy(
								totalChapters = progress.totalChapters,
								currentChapter = progress.currentChapter,
								totalPages = progress.totalPages,
								currentPage = progress.currentPage,
								isIndeterminate = false,
								eta = etaEstimator.getEta(),
								isStuck = etaEstimator.isStuck(),
							),
						)
					}
					analytics.recordChapterComplete(repo.source, pages.size, chapterBytes.get())
					if (output.flushChapter(chapter.value)) {
						runCatchingCancellable {
							localStorageChanges.emit(LocalMangaParser(output.rootFile).getManga(withDetails = false))
						}.onFailure(Throwable::printStackTraceDebug)
					}
					publishState(currentState.copy(downloadedChapters = currentState.downloadedChapters + 1))
					}
					publishState(currentState.copy(isIndeterminate = true, eta = -1L, isStuck = false))
					output.mergeWithExisting()
					output.finish()
					val localManga = LocalMangaParser(output.rootFile).getManga(withDetails = false)
					localStorageChanges.emit(localManga)
					publishState(currentState.copy(localManga = localManga, eta = -1L, isStuck = false))
					smartQueue.remove(manga.id)
				} catch (e: Exception) {
					if (e !is CancellationException) {
						publishState(
							currentState.copy(
								error = e,
								errorMessage = e.getDisplayMessage(applicationContext.resources),
							),
						)
					}
				throw e
			} finally {
				withContext(NonCancellable) {
					applicationContext.unregisterReceiver(pausingReceiver)
					output?.closeQuietly()
					output?.cleanup()
					destination.listFiles(TempFileFilter())?.forEach {
						it.deleteAwait()
					}
				}
			}
		}
	}

	private suspend fun <R> runFailsafe(
		block: suspend () -> R,
	): R? {
		checkIsPaused()
		var countDown = MAX_FAILSAFE_ATTEMPTS
		failsafe@ while (true) {
			try {
				return block()
			} catch (e: IOException) {
				val retryDelay = if (e is TooManyRequestExceptions) {
					e.getRetryDelay()
				} else {
					DOWNLOAD_ERROR_DELAY
				}
				if (countDown <= 0 || retryDelay < 0 || retryDelay > MAX_RETRY_DELAY) {
					val pausingHandle = PausingHandle.current()
					if (pausingHandle.skipAllErrors()) {
						return null
					}
					publishState(
						currentState.copy(
							isPaused = true,
							error = e,
							errorMessage = e.getDisplayMessage(applicationContext.resources),
							eta = -1L,
							isStuck = false,
						),
					)
					countDown = MAX_FAILSAFE_ATTEMPTS
					pausingHandle.pause()
					try {
						pausingHandle.awaitResumed()
						if (pausingHandle.skipCurrentError()) {
							return null
						}
					} finally {
						publishState(currentState.copy(isPaused = false, error = null, errorMessage = null))
					}
				} else {
					countDown--
					delay(retryDelay)
				}
			}
		}
	}

	private suspend fun checkIsPaused() {
		val pausingHandle = PausingHandle.current()
		if (pausingHandle.isPaused) {
			publishState(currentState.copy(isPaused = true, eta = -1L, isStuck = false))
			try {
				pausingHandle.awaitResumed()
			} finally {
				publishState(currentState.copy(isPaused = false))
			}
		}
	}

	private suspend fun getMediaType(url: String, file: File): MimeType? = runInterruptible(Dispatchers.IO) {
		MimeTypes.probeMimeType(file)?.let {
			return@runInterruptible it
		}
		if (nativeImageProbe.isAvailable) {
			nativeImageProbe.probeFormat(file).toMimeTypeOrNull()?.let {
				return@runInterruptible it
			}
		}
		BitmapDecoderCompat.probeMimeType(file)?.let {
			return@runInterruptible it
		}
		MimeTypes.getMimeTypeFromUrl(url)
	}

	private suspend fun downloadFile(
		url: String,
		destination: File,
		source: MangaSource,
	): File {
		if (url.startsWith("content:", ignoreCase = true) || url.startsWith("file:", ignoreCase = true)) {
			val uri = url.toUri()
			val cr = applicationContext.contentResolver
			val ext = uri.toFileOrNull()?.let {
				MimeTypes.getNormalizedExtension(it.name)
			} ?: cr.getType(uri)?.toMimeTypeOrNull()?.let { MimeTypes.getExtension(it) }
			val file = destination.createTempFile(ext)
			try {
				cr.openSource(uri).use { input ->
					file.sink(append = false).buffer().use {
						it.writeAllCancellable(input)
					}
				}
			} catch (e: Exception) {
				file.delete()
				throw e
			}
			return file
		}
		val request = PageLoader.createPageRequest(url, source)
		analytics.recordPageRequest(source)
		slowdownDispatcher.delay(source)
		val startedAt = SystemClock.elapsedRealtime()
		return try {
			imageProxyInterceptor.interceptPageRequest(request, okHttp)
				.ensureSuccess()
				.use { response ->
					var file: File? = null
					try {
						response.requireBody().use { body ->
							file = destination.createTempFile(
								ext = MimeTypes.getExtension(body.contentType()?.toMimeType())
							)
							file.sink(append = false).buffer().use {
								it.writeAllCancellable(body.source())
							}
						}
					} catch (e: Exception) {
						file?.delete()
						throw e
					}
					val result = checkNotNull(file)
					val elapsedMs = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(1L)
					analytics.recordPageSuccess(source, result.length(), elapsedMs)
					if (elapsedMs >= SLOW_RESPONSE_THRESHOLD_MS) {
						slowdownDispatcher.recordSlowResponse(source)
					}
					result
				}
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			analytics.recordPageFailure(source)
			if (e.isRateLimit()) {
				analytics.record429(source)
				slowdownDispatcher.recordRateLimit(source)
			}
			throw e
		}
	}

	private fun Throwable.isRateLimit(): Boolean = when (this) {
		is TooManyRequestExceptions -> true
		is HttpStatusException -> statusCode == HTTP_TOO_MANY_REQUESTS
		else -> false
	}

	private fun File.createTempFile(ext: String?) = File(
		this,
		buildString {
			append(UUID.randomUUID().toString())
			if (!ext.isNullOrEmpty()) {
				append('.')
				append(ext)
			}
			append(".tmp")
		},
	)

	private suspend fun publishState(state: DownloadState) {
		val previousState = currentState
		lastPublishedState = state
		if (previousState.isParticularProgress && state.isParticularProgress) {
			etaEstimator.onProgressChanged(state.progress, state.max)
		} else {
			etaEstimator.reset()
			notificationThrottler.reset()
		}
		val notification = notificationFactory.create(state)
		if (state.isFinalState) {
			if (!notificationFactory.isSilent) {
				notificationManager.notify(id.toString(), id.hashCode(), notification)
			}
		} else if (notificationThrottler.throttle()) {
			notificationManager.notify(id.hashCode(), notification)
		} else {
			return
		}
		setProgress(state.toWorkData())
	}

	private suspend fun getDoneChapters(manga: Manga) = runCatchingCancellable {
		localMangaRepository.getDetails(manga).chapters?.ids()
	}.getOrNull().orEmpty()

	private fun getChapters(
		manga: Manga,
		task: DownloadTask,
	): List<IndexedValue<MangaChapter>> {
		val chapters = checkNotNull(manga.chapters) { "Chapters list must not be null" }
		val chaptersIdsSet = task.chaptersIds?.toMutableSet()
		val result = ArrayList<IndexedValue<MangaChapter>>((chaptersIdsSet ?: chapters).size)
		val counters = HashMap<String?, Int>()
		for (chapter in chapters) {
			val index = counters[chapter.branch] ?: 0
			counters[chapter.branch] = index + 1
			if (chaptersIdsSet != null && !chaptersIdsSet.remove(chapter.id)) {
				continue
			}
			result.add(IndexedValue(index, chapter))
		}
		if (chaptersIdsSet != null) {
			check(chaptersIdsSet.isEmpty()) {
				"${chaptersIdsSet.size} of ${task.chaptersIds.size} requested chapters not found in manga"
			}
		}
		check(result.isNotEmpty()) { "Chapters list must not be empty" }
		return result
	}

	@Reusable
		class Scheduler @Inject constructor(
			@ApplicationContext private val context: Context,
			private val mangaDataRepository: MangaDataRepository,
			private val workManager: WorkManager,
			private val smartQueue: SmartDownloadQueue,
		) {

		fun observeWorks(): Flow<List<WorkInfo>> = workManager
			.getWorkInfosByTagFlow(TAG)

		@SuppressLint("RestrictedApi")
		suspend fun getInputData(id: UUID): Data? {
			val spec = workManager.getWorkSpec(id) ?: return null
			return Data.Builder()
				.putAll(spec.input)
				.putLong(DownloadState.DATA_TIMESTAMP, spec.scheduleRequestedAt)
				.build()
		}

		suspend fun getTask(workId: UUID): DownloadTask? {
			return workManager.getWorkInputData(workId)?.let { DownloadTask(it) }
		}

		suspend fun cancel(id: UUID) {
			val task = getTask(id)
			workManager.cancelWorkById(id).await()
			task?.let { smartQueue.remove(it.mangaId) }
		}

		suspend fun cancelAll() {
			workManager.cancelAllWorkByTag(TAG).await()
			smartQueue.clear()
		}

		fun pause(id: UUID) = context.sendBroadcast(
			PausingReceiver.getPauseIntent(context, id),
		)

		fun resume(id: UUID) = context.sendBroadcast(
			PausingReceiver.getResumeIntent(context, id),
		)

		fun skip(id: UUID) = context.sendBroadcast(
			PausingReceiver.getSkipIntent(context, id),
		)

		fun skipAll(id: UUID) = context.sendBroadcast(
			PausingReceiver.getSkipAllIntent(context, id),
		)

		suspend fun delete(id: UUID) {
			val task = getTask(id)
			workManager.deleteWork(id)
			task?.let { smartQueue.remove(it.mangaId) }
		}

		suspend fun delete(ids: Collection<UUID>) {
			val wm = workManager
			ids.forEach { id ->
				getTask(id)?.let { smartQueue.remove(it.mangaId) }
				wm.cancelWorkById(id).await()
			}
			workManager.deleteWorks(ids)
		}

		suspend fun removeCompleted() {
			val finishedWorks = workManager.awaitFinishedWorkInfosByTag(TAG)
			workManager.deleteWorks(finishedWorks.mapToSet { it.id })
		}

		suspend fun updateConstraints(allowMeteredNetwork: Boolean) {
			val constraints = createConstraints(allowMeteredNetwork)
			val works = workManager.awaitWorkInfosByTag(TAG)
			for (work in works) {
				if (work.state.isFinished) {
					continue
				}
				val inputData = workManager.getWorkInputData(work.id) ?: continue
				val request = OneTimeWorkRequestBuilder<DownloadWorker>()
					.setConstraints(constraints)
					.addTag(TAG)
					.setId(work.id)
					.keepResultsForAtLeast(30, TimeUnit.DAYS)
					.setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
					.setInputData(inputData)
					.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
					.build()
				workManager.awaitUpdateWork(request)
			}
		}

		suspend fun schedule(tasks: Collection<Pair<Manga, DownloadTask>>) {
			if (tasks.isEmpty()) {
				return
			}
			val requests = tasks.map { (manga, task) ->
				mangaDataRepository.storeManga(manga, replaceExisting = true)
				val request = OneTimeWorkRequestBuilder<DownloadWorker>()
					.setConstraints(createConstraints(task.allowMeteredNetwork))
					.addTag(TAG)
					.keepResultsForAtLeast(30, TimeUnit.DAYS)
					.setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
					.setInputData(task.toData())
					.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
					.build()
				manga.id to request
			}
			workManager.enqueue(requests.map { it.second }).await()
			smartQueue.enqueueAll(
				requests.map { (mangaId, request) ->
					SmartDownloadQueue.QueueEntry(
						mangaId = mangaId,
						priority = SmartDownloadQueue.Priority.DEFAULT,
						workId = request.id,
					)
				},
			)
		}

		private fun createConstraints(allowMeteredNetwork: Boolean) = Constraints.Builder()
			.setRequiredNetworkType(if (allowMeteredNetwork) NetworkType.CONNECTED else NetworkType.UNMETERED)
			.build()
	}

	private companion object {

		const val MAX_FAILSAFE_ATTEMPTS = 2
		// Adaptive: resolved at runtime via DownloadParallelismManager
		const val DOWNLOAD_ERROR_DELAY = 2_000L
		const val MAX_RETRY_DELAY = 7_200_000L // 2 hours
		const val HTTP_TOO_MANY_REQUESTS = 429
		const val SLOW_RESPONSE_THRESHOLD_MS = 8_000L
		const val TAG = "download"
	}
}
