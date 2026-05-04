package org.koharu.miyo.scrobbling.common.ui.config.adapter

import org.koharu.miyo.core.ui.BaseListAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.list.ui.adapter.ListItemType
import org.koharu.miyo.list.ui.adapter.emptyStateListAD
import org.koharu.miyo.list.ui.model.ListModel
import org.koharu.miyo.scrobbling.common.domain.model.ScrobblingInfo

class ScrobblingMangaAdapter(
	clickListener: OnListItemClickListener<ScrobblingInfo>,
) : BaseListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.HEADER, scrobblingHeaderAD())
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(null))
		addDelegate(ListItemType.MANGA_SCROBBLING, scrobblingMangaAD(clickListener))
	}
}
