package org.draken.usagi.download.domain.analytics

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.parsers.model.MangaSource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadAnalytics @Inject constructor() {

	private val sourceStats = ConcurrentHashMap<String, SourceStats>()
	private val globalBytesDownloaded = LongAdder()
	private val globalBytesTotal = LongAdder()
	private val globalRequestsTotal = LongAdder()
	private val globalRequestsFailed = LongAdder()
	private val global429Count = LongAdder()
	private val globalDownloadTimeMs = LongAdder()
	private val mutex = Mutex()

	fun recordPageRequest(source: MangaSource) {
		globalRequestsTotal.increment()
		statsFor(source).requestsTotal.incrementAndGet()
	}

	fun recordPageSuccess(source: MangaSource, bytes: Long, elapsedMs: Long) {
		globalBytesDownloaded.add(bytes)
		globalDownloadTimeMs.add(elapsedMs)
		val stats = statsFor(source)
		stats.bytesDownloaded.add(bytes)
		stats.requestsSucceeded.incrementAndGet()
		stats.totalDownloadTimeMs.add(elapsedMs)
	}

	fun recordPageFailure(source: MangaSource) {
		globalRequestsFailed.increment()
		statsFor(source).requestsFailed.incrementAndGet()
	}

	fun record429(source: MangaSource) {
		global429Count.increment()
		statsFor(source).rateLimit429Count.incrementAndGet()
	}

	fun recordChapterComplete(source: MangaSource, pages: Int, bytes: Long) {
		globalBytesTotal.add(bytes)
		val stats = statsFor(source)
		stats.totalBytes.add(bytes)
		stats.chaptersCompleted.incrementAndGet()
		stats.totalPages.add(pages.toLong())
	}

	fun getGlobalSpeedMbps(): Double {
		val totalMs = globalDownloadTimeMs.sum()
		if (totalMs == 0L) return 0.0
		val totalBytes = globalBytesDownloaded.sum()
		return (totalBytes * 8.0) / (totalMs / 1000.0) / 1_000_000.0
	}

	fun getGlobalSuccessRate(): Double {
		val total = globalRequestsTotal.sum()
		if (total == 0L) return 1.0
		return (total - globalRequestsFailed.sum()).toDouble() / total
	}

	fun getGlobal429Count(): Long = global429Count.sum()

	suspend fun getSourceStats(): Map<String, SourceStatsSnapshot> = mutex.withLock {
		sourceStats.mapValues { (_, stats) -> stats.snapshot() }
	}

	suspend fun reset() = mutex.withLock {
		sourceStats.clear()
		globalBytesDownloaded.reset()
		globalBytesTotal.reset()
		globalRequestsTotal.reset()
		globalRequestsFailed.reset()
		global429Count.reset()
		globalDownloadTimeMs.reset()
	}

	private fun statsFor(source: MangaSource): SourceStats =
		sourceStats.getOrPut(source.name) { SourceStats() }

	class SourceStats {
		var requestsTotal = AtomicLong(0)
		var requestsSucceeded = AtomicLong(0)
		var requestsFailed = AtomicLong(0)
		var rateLimit429Count = AtomicLong(0)
		var chaptersCompleted = AtomicLong(0)
		var totalPages = LongAdder()
		var bytesDownloaded = LongAdder()
		var totalBytes = LongAdder()
		var totalDownloadTimeMs = LongAdder()

		fun snapshot() = SourceStatsSnapshot(
			requestsTotal = requestsTotal.get(),
			requestsSucceeded = requestsSucceeded.get(),
			requestsFailed = requestsFailed.get(),
			rateLimit429Count = rateLimit429Count.get(),
			chaptersCompleted = chaptersCompleted.get(),
			totalPages = totalPages.sum(),
			bytesDownloaded = bytesDownloaded.sum(),
			totalBytes = totalBytes.sum(),
			totalDownloadTimeMs = totalDownloadTimeMs.sum(),
		)
	}

	data class SourceStatsSnapshot(
		val requestsTotal: Long,
		val requestsSucceeded: Long,
		val requestsFailed: Long,
		val rateLimit429Count: Long,
		val chaptersCompleted: Long,
		val totalPages: Long,
		val bytesDownloaded: Long,
		val totalBytes: Long,
		val totalDownloadTimeMs: Long,
	) {
		val successRate: Double
			get() = if (requestsTotal == 0L) 1.0 else requestsSucceeded.toDouble() / requestsTotal

		val averageSpeedMbps: Double
			get() {
				if (totalDownloadTimeMs == 0L) return 0.0
				return (bytesDownloaded * 8.0) / (totalDownloadTimeMs / 1000.0) / 1_000_000.0
			}
	}
}