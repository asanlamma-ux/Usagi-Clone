package org.draken.usagi.download.ui.worker

import android.os.SystemClock
import androidx.collection.MutableObjectLongMap
import kotlinx.coroutines.delay
import org.draken.usagi.core.parser.MangaRepository
import org.draken.usagi.core.parser.ParserMangaRepository
<<<<<<< HEAD
import org.koitharu.kotatsu.parsers.model.MangaSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
=======
import org.draken.usagi.download.domain.AdaptiveTokenBucket
import org.koitharu.kotatsu.parsers.model.MangaSource
import javax.inject.Inject
import javax.inject.Singleton
>>>>>>> abd49974e6e6c21783ada6501e12b3446c988ec6

@Singleton
class DownloadSlowdownDispatcher @Inject constructor(
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {
<<<<<<< HEAD
	private data class SourceState(
		val lastRequestTime: Long = 0L,
		val currentDelay: Long = INITIAL_DELAY_MS,
		val consecutiveFailures: Int = 0,
	)

	private val stateMap = mutableMapOf<MangaSource, SourceState>()
	private val lock = Any()

	suspend fun delay(source: MangaSource) {
		val repo = mangaRepositoryFactory.create(source) as? ParserMangaRepository ?: return
		if (!repo.isSlowdownEnabled()) {
			return
		}

		val (lastRequest, delayMs) = synchronized(lock) {
			val state = stateMap.getOrPut(source) { SourceState() }
			val elapsed = SystemClock.elapsedRealtime()
			val lastReq = state.lastRequestTime
			stateMap[source] = state.copy(lastRequestTime = elapsed)
			Pair(lastReq, state.currentDelay)
		}

		if (lastRequest != 0L) {
			delay(max(0L, lastRequest + delayMs - SystemClock.elapsedRealtime()))
		}
	}

	fun onSuccess(source: MangaSource) {
		synchronized(lock) {
			val state = stateMap[source] ?: return
			stateMap[source] = state.copy(
				currentDelay = max(MIN_DELAY_MS, (state.currentDelay * 0.9).toLong()),
				consecutiveFailures = 0,
			)
		}
	}

	fun onRateLimited(source: MangaSource) {
		synchronized(lock) {
			val state = stateMap[source] ?: return
			val failures = state.consecutiveFailures + 1
			val multiplier = min(4.0, 1.0 + failures * 0.5)
			stateMap[source] = state.copy(
				currentDelay = min(MAX_DELAY_MS, (state.currentDelay * multiplier).toLong()),
				consecutiveFailures = failures,
			)
=======
	private val adaptiveBucket = AdaptiveTokenBucket(maxTokensPerSecond = DEFAULT_RATE)
	private val timeMap = MutableObjectLongMap<MangaSource>()

	suspend fun getDelayMs(source: MangaSource): Long {
		val repo = mangaRepositoryFactory.create(source) as? ParserMangaRepository
		if (repo?.isSlowdownEnabled() != true) {
			return 0L
		}
		val adaptiveWait = adaptiveBucket.acquire(source.name)
		if (adaptiveWait > 0L) {
			return adaptiveWait
		}
		val lastRequest = synchronized(timeMap) {
			val res = timeMap.getOrDefault(source, 0L)
			timeMap[source] = SystemClock.elapsedRealtime()
			res
		}
		if (lastRequest != 0L) {
			val pacingDelay = lastRequest + MIN_PACING_MS - SystemClock.elapsedRealtime()
			if (pacingDelay > 0) {
				return pacingDelay
			}
		}
		return 0L
	}

	suspend fun recordRateLimit(source: MangaSource) {
		adaptiveBucket.recordRateLimit(source.name)
	}

	suspend fun recordSlowResponse(source: MangaSource) {
		adaptiveBucket.recordSlowResponse(source.name)
	}

	@Deprecated("Use getDelayMs instead", ReplaceWith("getDelayMs(source)"))
	suspend fun delay(source: MangaSource) {
		val delayMs = getDelayMs(source)
		if (delayMs > 0L) {
			delay(delayMs)
>>>>>>> abd49974e6e6c21783ada6501e12b3446c988ec6
		}
	}

	companion object {
<<<<<<< HEAD
		private const val INITIAL_DELAY_MS = 800L
		private const val MIN_DELAY_MS = 200L
		private const val MAX_DELAY_MS = 3000L
	}
}
=======
		private const val DEFAULT_RATE = 8.0
		private const val MIN_PACING_MS = 200L
	}
}
>>>>>>> abd49974e6e6c21783ada6501e12b3446c988ec6
