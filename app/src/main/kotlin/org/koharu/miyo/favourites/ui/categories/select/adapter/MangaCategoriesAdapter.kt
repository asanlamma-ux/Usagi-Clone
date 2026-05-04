package org.koharu.miyo.favourites.ui.categories.select.adapter

import org.koharu.miyo.core.ui.BaseListAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.favourites.ui.categories.select.model.MangaCategoryItem
import org.koharu.miyo.list.ui.adapter.ListItemType
import org.koharu.miyo.list.ui.adapter.emptyStateListAD
import org.koharu.miyo.list.ui.adapter.loadingStateAD
import org.koharu.miyo.list.ui.model.ListModel

class MangaCategoriesAdapter(
	clickListener: OnListItemClickListener<MangaCategoryItem>,
) : BaseListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.NAV_ITEM, mangaCategoryAD(clickListener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(null))
	}
}
