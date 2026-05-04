package org.koharu.miyo.local.data.output

import androidx.core.net.toFile
import androidx.core.net.toUri
import org.koharu.miyo.core.model.isLocal
import org.koitharu.kotatsu.parsers.model.Manga

class LocalMangaUtil(
	private val manga: Manga,
) {

	init {
		require(manga.isLocal) { "Expected LOCAL source but ${manga.source} found" }
	}

	suspend fun deleteChapters(ids: Set<Long>) {
		val file = manga.url.toUri().toFile()
		if (file.isDirectory) {
			LocalMangaDirOutput(file, manga).use { output ->
				output.deleteChapters(ids)
				output.finish()
			}
		} else {
			LocalMangaZipOutput.filterChapters(file, manga, ids)
		}
	}
}
