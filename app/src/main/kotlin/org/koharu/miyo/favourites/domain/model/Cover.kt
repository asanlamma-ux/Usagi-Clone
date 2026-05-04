package org.koharu.miyo.favourites.domain.model

import org.koharu.miyo.core.model.MangaSource

data class Cover(
	val url: String?,
	val source: String,
) {
	val mangaSource by lazy { MangaSource(source) }
}
