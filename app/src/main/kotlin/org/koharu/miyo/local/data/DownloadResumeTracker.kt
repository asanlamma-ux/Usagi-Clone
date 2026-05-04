package org.koharu.miyo.local.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koharu.miyo.core.util.ext.printStackTraceDebug
import java.io.File

/**
 * Tracks download progress to enable resume after interruption.
 * Writes a .download_progress.json file in the output directory with completed chapter IDs.
 */
@Serializable
data class DownloadProgressState(
    val mangaId: Long,
    val completedChapterIds: MutableSet<Long> = mutableSetOf(),
    val currentChapterId: Long = 0,
    val completedPageIndex: Int = 0,
)

object DownloadResumeTracker {
    private const val PROGRESS_FILE = ".download_progress.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun load(outputDir: File): DownloadProgressState? {
        val file = File(outputDir, PROGRESS_FILE)
        if (!file.exists()) return null
        return try {
            json.decodeFromString<DownloadProgressState>(file.readText())
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    fun save(outputDir: File, state: DownloadProgressState) {
        val file = File(outputDir, PROGRESS_FILE)
        try {
            file.writeText(json.encodeToString(DownloadProgressState.serializer(), state))
        } catch (e: Exception) {
            e.printStackTraceDebug()
        }
    }

    fun markChapterComplete(outputDir: File, chapterId: Long) {
        val state = load(outputDir) ?: return
        state.completedChapterIds.add(chapterId)
        save(outputDir, state)
    }

    fun remove(outputDir: File) {
        File(outputDir, PROGRESS_FILE).delete()
    }
}