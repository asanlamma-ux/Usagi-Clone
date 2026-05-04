package org.koharu.miyo.details.ui.pager.pages

import android.content.Context
import org.koharu.miyo.core.ui.BaseListAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.core.ui.list.fastscroll.FastScroller
import org.koharu.miyo.list.ui.adapter.ListItemType
import org.koharu.miyo.list.ui.adapter.listHeaderAD
import org.koharu.miyo.list.ui.model.ListModel

class PageThumbnailAdapter(
	clickListener: OnListItemClickListener<PageThumbnail>,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	init {
		addDelegate(ListItemType.PAGE_THUMB, pageThumbnailAD(clickListener))
		addDelegate(ListItemType.HEADER, listHeaderAD(null))
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return findHeader(position)?.getText(context)
	}
}
