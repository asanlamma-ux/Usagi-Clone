package org.koharu.miyo.history.data

import dagger.Reusable
import org.koharu.miyo.core.db.MangaDatabase
import org.koharu.miyo.core.db.entity.toManga
import org.koharu.miyo.core.db.entity.toMangaTags
import org.koharu.miyo.history.domain.model.MangaWithHistory
import org.koharu.miyo.list.domain.ListFilterOption
import org.koharu.miyo.list.domain.ListSortOrder
import org.koharu.miyo.local.data.index.LocalMangaIndex
import org.koharu.miyo.local.domain.LocalObserveMapper
import org.koitharu.kotatsu.parsers.model.Manga
import javax.inject.Inject

@Reusable
class HistoryLocalObserver @Inject constructor(
	localMangaIndex: LocalMangaIndex,
	private val db: MangaDatabase,
) : LocalObserveMapper<HistoryWithManga, MangaWithHistory>(localMangaIndex) {

	fun observeAll(
		order: ListSortOrder,
		filterOptions: Set<ListFilterOption>,
		limit: Int
	) = db.getHistoryDao().observeAll(order, filterOptions, limit).mapToLocal()

	override fun toManga(e: HistoryWithManga) = e.manga.toManga(e.tags.toMangaTags(), null)

	override fun toResult(e: HistoryWithManga, manga: Manga) = MangaWithHistory(
		manga = manga,
		history = e.history.toMangaHistory(),
	)
}
