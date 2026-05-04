package org.koharu.miyo.search.ui.multi.adapter

import android.content.Context
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import org.koharu.miyo.core.ui.BaseListAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.core.ui.list.fastscroll.FastScroller
import org.koharu.miyo.list.ui.MangaSelectionDecoration
import org.koharu.miyo.list.ui.adapter.ListItemType
import org.koharu.miyo.list.ui.adapter.MangaListListener
import org.koharu.miyo.list.ui.adapter.buttonFooterAD
import org.koharu.miyo.list.ui.adapter.emptyStateListAD
import org.koharu.miyo.list.ui.adapter.errorStateListAD
import org.koharu.miyo.list.ui.adapter.loadingFooterAD
import org.koharu.miyo.list.ui.adapter.loadingStateAD
import org.koharu.miyo.list.ui.model.ListModel
import org.koharu.miyo.list.ui.size.ItemSizeResolver
import org.koharu.miyo.search.ui.multi.SearchResultsListModel

class SearchAdapter(
	listener: MangaListListener,
	itemClickListener: OnListItemClickListener<SearchResultsListModel>,
	sizeResolver: ItemSizeResolver,
	selectionDecoration: MangaSelectionDecoration,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		val pool = RecycledViewPool()
		addDelegate(
			ListItemType.MANGA_NESTED_GROUP,
			searchResultsAD(
				sharedPool = pool,
				sizeResolver = sizeResolver,
				selectionDecoration = selectionDecoration,
				listener = listener,
				itemClickListener = itemClickListener,
			),
		)
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.FOOTER_LOADING, loadingFooterAD())
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(listener))
		addDelegate(ListItemType.STATE_ERROR, errorStateListAD(listener))
		addDelegate(ListItemType.FOOTER_BUTTON, buttonFooterAD(listener))
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return (items.getOrNull(position) as? SearchResultsListModel)?.getTitle(context)
	}
}
