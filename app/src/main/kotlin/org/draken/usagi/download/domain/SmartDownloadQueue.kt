package org.draken.usagi.download.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koitharu.kotatsu.parsers.model.Manga
import java.util.PriorityQueue
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartDownloadQueue @Inject constructor() {

	private val queue = PriorityQueue<QueueEntry>(compareBy<QueueEntry> {
		when (it.priority) {
			Priority.READING_NOW -> 0
			Priority.FAVORITE_RECENT -> 1
			Priority.FAVORITE_OTHER -> 2
			Priority.DEFAULT -> 3
		}
	}.thenByDescending { it.addedAtMs })

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
		queue.poll()
	}

	suspend fun peek(): QueueEntry? = mutex.withLock {
		queue.peek()
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
		queue.map { it.mangaId }
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