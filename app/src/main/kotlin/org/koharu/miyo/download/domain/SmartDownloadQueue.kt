package org.koharu.miyo.download.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartDownloadQueue @Inject constructor() {

	private val queue = ArrayList<QueueEntry>()
	private val queueComparator = compareBy<QueueEntry> {
		when (it.priority) {
			Priority.READING_NOW -> 0
			Priority.FAVORITE_RECENT -> 1
			Priority.FAVORITE_OTHER -> 2
			Priority.DEFAULT -> 3
		}
	}.thenByDescending { it.addedAtMs }

	private val _currentReading = MutableStateFlow<Long?>(null)
	val currentReading: Flow<Long?> = _currentReading.asStateFlow()

	private val mutex = Mutex()

	fun setCurrentReading(mangaId: Long?) {
		_currentReading.value = mangaId
	}

	suspend fun enqueue(task: QueueEntry) = mutex.withLock {
		queue.add(task)
	}

	suspend fun enqueueAll(tasks: Collection<QueueEntry>) = mutex.withLock {
		queue.addAll(tasks)
	}

	suspend fun dequeue(): QueueEntry? = mutex.withLock {
		val index = queue.nextIndexOrNull() ?: return@withLock null
		queue.removeAt(index)
	}

	suspend fun peek(): QueueEntry? = mutex.withLock {
		val index = queue.nextIndexOrNull() ?: return@withLock null
		queue[index]
	}

	suspend fun remove(mangaId: Long) = mutex.withLock {
		queue.removeAll { it.mangaId == mangaId }
	}

	suspend fun isEmpty(): Boolean = mutex.withLock {
		queue.isEmpty()
	}

	suspend fun size(): Int = mutex.withLock {
		queue.size
	}

	suspend fun clear() = mutex.withLock {
		queue.clear()
	}

	suspend fun getPendingIds(): List<Long> = mutex.withLock {
		queue.sortedWith(queueComparator).map { it.mangaId }
	}

	private fun List<QueueEntry>.nextIndexOrNull(): Int? {
		if (isEmpty()) return null
		var bestIndex = 0
		for (i in 1 until size) {
			if (queueComparator.compare(this[i], this[bestIndex]) < 0) {
				bestIndex = i
			}
		}
		return bestIndex
	}

	data class QueueEntry(
		val mangaId: Long,
		val manga: Manga? = null,
		val priority: Priority = Priority.DEFAULT,
		val addedAtMs: Long = System.currentTimeMillis(),
		val workId: UUID? = null,
	)

	enum class Priority {
		READING_NOW,
		FAVORITE_RECENT,
		FAVORITE_OTHER,
		DEFAULT,
	}
}
