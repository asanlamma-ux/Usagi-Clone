package org.koharu.miyo.history.ui

import android.content.Context
import org.koharu.miyo.core.ui.list.fastscroll.FastScroller
import org.koharu.miyo.list.ui.adapter.MangaListAdapter
import org.koharu.miyo.list.ui.adapter.MangaListListener
import org.koharu.miyo.list.ui.size.ItemSizeResolver

class HistoryListAdapter(
	listener: MangaListListener,
	sizeResolver: ItemSizeResolver,
) : MangaListAdapter(listener, sizeResolver), FastScroller.SectionIndexer {

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return findHeader(position)?.getText(context)
	}
}
