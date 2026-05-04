package org.koharu.miyo.explore.ui.model

import org.koharu.miyo.list.ui.model.ListModel
import org.koharu.miyo.list.ui.model.MangaCompactListModel

data class RecommendationsItem(
	val manga: List<MangaCompactListModel>
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is RecommendationsItem
	}
}
