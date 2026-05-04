package org.koharu.miyo.favourites.ui.categories.adapter

import org.koharu.miyo.core.ui.ReorderableListAdapter
import org.koharu.miyo.favourites.ui.categories.FavouriteCategoriesListListener
import org.koharu.miyo.list.ui.adapter.ListItemType
import org.koharu.miyo.list.ui.adapter.ListStateHolderListener
import org.koharu.miyo.list.ui.adapter.emptyStateListAD
import org.koharu.miyo.list.ui.adapter.loadingStateAD
import org.koharu.miyo.list.ui.model.ListModel

class CategoriesAdapter(
	onItemClickListener: FavouriteCategoriesListListener,
	listListener: ListStateHolderListener,
) : ReorderableListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.CATEGORY_LARGE, categoryAD(onItemClickListener))
		addDelegate(ListItemType.NAV_ITEM, allCategoriesAD(onItemClickListener))
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(listListener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
	}
}
