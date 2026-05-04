package org.koharu.miyo.download.ui.worker

import android.os.SystemClock
import androidx.collection.MutableObjectLongMap
import kotlinx.coroutines.delay
import org.koharu.miyo.core.parser.MangaRepository
import org.koharu.miyo.core.parser.ParserMangaRepository
import org.koharu.miyo.download.domain.AdaptiveTokenBucket
import org.koitharu.kotatsu.parsers.model.MangaSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadSlowdownDispatcher @Inject constructor(
	private val mangaRepositoryFactory: MangaRepository.Factory,
) {
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
		}
	}

	companion object {
		private const val DEFAULT_RATE = 8.0
		private const val MIN_PACING_MS = 200L
	}
}
