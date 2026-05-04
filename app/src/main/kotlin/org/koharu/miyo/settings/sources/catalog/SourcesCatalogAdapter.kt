package org.koharu.miyo.settings.sources.catalog

import android.content.Context
import org.koharu.miyo.core.model.getTitle
import org.koharu.miyo.core.ui.BaseListAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.core.ui.list.fastscroll.FastScroller
import org.koharu.miyo.list.ui.adapter.ListItemType
import org.koharu.miyo.list.ui.adapter.loadingStateAD
import org.koharu.miyo.list.ui.model.ListModel

class SourcesCatalogAdapter(
	listener: OnListItemClickListener<SourceCatalogItem.Source>,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.CHAPTER_LIST, sourceCatalogItemSourceAD(listener))
		addDelegate(ListItemType.HINT_EMPTY, sourceCatalogItemHintAD())
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return (items.getOrNull(position) as? SourceCatalogItem.Source)?.source?.getTitle(context)?.take(1)
	}
}
