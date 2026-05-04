package org.koharu.miyo.tracker.domain

import org.koharu.miyo.core.prefs.AppSettings
import org.koharu.miyo.favourites.domain.FavouritesRepository
import org.koharu.miyo.list.domain.ListFilterOption
import org.koharu.miyo.list.domain.MangaListQuickFilter
import javax.inject.Inject

class UpdatesListQuickFilter @Inject constructor(
	private val favouritesRepository: FavouritesRepository,
	settings: AppSettings,
) : MangaListQuickFilter(settings) {

	override suspend fun getAvailableFilterOptions(): List<ListFilterOption> =
		favouritesRepository.getMostUpdatedCategories(
			limit = 4,
		).map {
			ListFilterOption.Favorite(it)
		}
}
