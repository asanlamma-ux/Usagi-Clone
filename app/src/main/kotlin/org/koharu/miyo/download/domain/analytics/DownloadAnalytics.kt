package org.koharu.miyo.download.domain.analytics

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.parsers.model.MangaSource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadAnalytics @Inject constructor() {

	private val sourceStats = ConcurrentHashMap<String, SourceStats>()
	private val globalBytesDownloaded = AtomicLong(0L)
	private val globalBytesTotal = AtomicLong(0L)
	private val globalRequestsTotal = AtomicLong(0L)
	private val globalRequestsFailed = AtomicLong(0L)
	private val global429Count = AtomicLong(0L)
	private val globalDownloadTimeMs = AtomicLong(0L)
	private val mutex = Mutex()

	fun recordPageRequest(source: MangaSource) {
		globalRequestsTotal.incrementAndGet()
		statsFor(source).requestsTotal.incrementAndGet()
	}

	fun recordPageSuccess(source: MangaSource, bytes: Long, elapsedMs: Long) {
		globalBytesDownloaded.addAndGet(bytes)
		globalDownloadTimeMs.addAndGet(elapsedMs)
		val stats = statsFor(source)
		stats.bytesDownloaded.addAndGet(bytes)
		stats.requestsSucceeded.incrementAndGet()
		stats.totalDownloadTimeMs.addAndGet(elapsedMs)
	}

	fun recordPageFailure(source: MangaSource) {
		globalRequestsFailed.incrementAndGet()
		statsFor(source).requestsFailed.incrementAndGet()
	}

	fun record429(source: MangaSource) {
		global429Count.incrementAndGet()
		statsFor(source).rateLimit429Count.incrementAndGet()
	}

	fun recordChapterComplete(source: MangaSource, pages: Int, bytes: Long) {
		globalBytesTotal.addAndGet(bytes)
		val stats = statsFor(source)
		stats.totalBytes.addAndGet(bytes)
		stats.chaptersCompleted.incrementAndGet()
		stats.totalPages.addAndGet(pages.toLong())
	}

	fun getGlobalSpeedMbps(): Double {
		val totalMs = globalDownloadTimeMs.get()
		if (totalMs == 0L) return 0.0
		val totalBytes = globalBytesDownloaded.get()
		return (totalBytes * 8.0) / (totalMs / 1000.0) / 1_000_000.0
	}

	fun getGlobalSuccessRate(): Double {
		val total = globalRequestsTotal.get()
		if (total == 0L) return 1.0
		return (total - globalRequestsFailed.get()).toDouble() / total
	}

	fun getGlobal429Count(): Long = global429Count.get()

	suspend fun getSourceStats(): Map<String, SourceStatsSnapshot> = mutex.withLock {
		sourceStats.mapValues { (_, stats) -> stats.snapshot() }
	}

	suspend fun reset() = mutex.withLock {
		sourceStats.clear()
		globalBytesDownloaded.set(0L)
		globalBytesTotal.set(0L)
		globalRequestsTotal.set(0L)
		globalRequestsFailed.set(0L)
		global429Count.set(0L)
		globalDownloadTimeMs.set(0L)
	}

	private fun statsFor(source: MangaSource): SourceStats =
		sourceStats.getOrPut(source.name) { SourceStats() }

	class SourceStats {
		val requestsTotal = AtomicLong(0L)
		val requestsSucceeded = AtomicLong(0L)
		val requestsFailed = AtomicLong(0L)
		val rateLimit429Count = AtomicLong(0L)
		val chaptersCompleted = AtomicLong(0L)
		val totalPages = AtomicLong(0L)
		val bytesDownloaded = AtomicLong(0L)
		val totalBytes = AtomicLong(0L)
		val totalDownloadTimeMs = AtomicLong(0L)

		fun snapshot() = SourceStatsSnapshot(
			requestsTotal = requestsTotal.get(),
			requestsSucceeded = requestsSucceeded.get(),
			requestsFailed = requestsFailed.get(),
			rateLimit429Count = rateLimit429Count.get(),
			chaptersCompleted = chaptersCompleted.get(),
			totalPages = totalPages.get(),
			bytesDownloaded = bytesDownloaded.get(),
			totalBytes = totalBytes.get(),
			totalDownloadTimeMs = totalDownloadTimeMs.get(),
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
