package org.koharu.miyo.favourites.data

import org.koharu.miyo.core.db.entity.toManga
import org.koharu.miyo.core.db.entity.toMangaTags
import org.koharu.miyo.core.model.FavouriteCategory
import org.koharu.miyo.list.domain.ListSortOrder
import java.time.Instant

fun FavouriteCategoryEntity.toFavouriteCategory(id: Long = categoryId.toLong()) = FavouriteCategory(
	id = id,
	title = title,
	sortKey = sortKey,
	order = ListSortOrder(order, ListSortOrder.NEWEST),
	createdAt = Instant.ofEpochMilli(createdAt),
	isTrackingEnabled = track,
	isVisibleInLibrary = isVisibleInLibrary,
)

fun FavouriteManga.toManga() = manga.toManga(tags.toMangaTags(), null)

fun Collection<FavouriteManga>.toMangaList() = map { it.toManga() }
