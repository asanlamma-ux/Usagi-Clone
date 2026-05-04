package org.koharu.miyo.local.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import org.koharu.miyo.core.util.ext.printStackTraceDebug
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * High-performance page file cache using direct file storage with LRU eviction.
 * Replaces DiskLruCache for page storage — faster random access, simpler eviction.
 */
class PageFileCache(
    private val directory: File,
    private val maxSize: Long,
) {
    private val fileSystem = FileSystem.SYSTEM
    private val dirPath = directory.absolutePath.toPath()

    private val accessTimes = ConcurrentHashMap<String, Long>()
    private val totalSize = AtomicLong(0L)
    private val mutex = Mutex()

    init {
        directory.mkdirs()
        // Scan existing files to compute initial size
        directory.listFiles()?.forEach { file ->
            if (file.isFile) {
                totalSize.addAndGet(file.length())
                accessTimes[file.name] = file.lastModified()
            }
        }
    }

    suspend fun get(key: String): File? {
        val file = File(directory, key.toSafeFileName())
        if (!file.exists() || !file.isFile) return null
        accessTimes[key] = System.currentTimeMillis()
        file.setLastModified(System.currentTimeMillis())
        return file
    }

    suspend fun put(key: String, data: okio.Source, type: String? = null): File = mutex.withLock {
        val file = File(directory, key.toSafeFileName())
        val sink = fileSystem.sink(file.absolutePath.toPath()).buffer()
        sink.use { it.writeAll(data) }
        totalSize.addAndGet(file.length())
        accessTimes[key] = System.currentTimeMillis()
        evictIfNeeded()
        file
    }

    suspend fun putBytes(key: String, bytes: ByteArray): File = mutex.withLock {
        val file = File(directory, key.toSafeFileName())
        withContext(Dispatchers.IO) {
            file.writeBytes(bytes)
        }
        totalSize.addAndGet(file.length())
        accessTimes[key] = System.currentTimeMillis()
        evictIfNeeded()
        file
    }

    suspend fun clear() = mutex.withLock {
        accessTimes.clear()
        totalSize.set(0L)
        withContext(Dispatchers.IO) {
            directory.listFiles()?.forEach { it.delete() }
        }
    }

    suspend fun remove(key: String) = mutex.withLock {
        val file = File(directory, key.toSafeFileName())
        if (file.exists()) {
            totalSize.addAndGet(-file.length())
        }
        accessTimes.remove(key)
        withContext(Dispatchers.IO) {
            file.delete()
        }
    }

    val size: Long get() = totalSize.get()

    private suspend fun evictIfNeeded() {
        while (totalSize.get() > maxSize) {
            val oldest = accessTimes.entries
                .minByOrNull { it.value }
                ?: break
            val file = File(directory, oldest.key)
            if (file.exists()) {
                withContext(Dispatchers.IO) { file.delete() }
                totalSize.addAndGet(-file.length())
            }
            accessTimes.remove(oldest.key)
        }
    }

    companion object {
        private fun String.toSafeFileName(): String {
            return this.hashCode().toString(36) + "_" +
                this.takeLast(64).replace(Regex("[^a-zA-Z0-9._-]"), "_")
        }
    }
}