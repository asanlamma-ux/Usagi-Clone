package org.koharu.miyo.explore.ui.adapter

import org.koharu.miyo.core.ui.BaseListAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.explore.ui.model.MangaSourceItem
import org.koharu.miyo.list.ui.adapter.ListItemType
import org.koharu.miyo.list.ui.adapter.emptyHintAD
import org.koharu.miyo.list.ui.adapter.listHeaderAD
import org.koharu.miyo.list.ui.adapter.loadingStateAD
import org.koharu.miyo.list.ui.model.ListModel
import org.koitharu.kotatsu.parsers.model.Manga

class ExploreAdapter(
	listener: ExploreListEventListener,
	clickListener: OnListItemClickListener<MangaSourceItem>,
	mangaClickListener: OnListItemClickListener<Manga>,
) : BaseListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.EXPLORE_BUTTONS, exploreButtonsAD(listener))
		addDelegate(
			ListItemType.EXPLORE_SUGGESTION,
			exploreRecommendationItemAD(mangaClickListener),
		)
		addDelegate(ListItemType.HEADER, listHeaderAD(listener))
		addDelegate(ListItemType.EXPLORE_SOURCE_LIST, exploreSourceListItemAD(clickListener))
		addDelegate(ListItemType.EXPLORE_SOURCE_GRID, exploreSourceGridItemAD(clickListener))
		addDelegate(ListItemType.HINT_EMPTY, emptyHintAD(listener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
	}
}
