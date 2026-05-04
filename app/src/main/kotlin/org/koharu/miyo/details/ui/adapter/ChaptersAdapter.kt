package org.koharu.miyo.details.ui.adapter

import android.content.Context
import org.koharu.miyo.core.ui.BaseListAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.core.ui.list.fastscroll.FastScroller
import org.koharu.miyo.details.ui.model.ChapterListItem
import org.koharu.miyo.list.ui.adapter.ListItemType
import org.koharu.miyo.list.ui.adapter.listHeaderAD
import org.koharu.miyo.list.ui.model.ListHeader
import org.koharu.miyo.list.ui.model.ListModel

class ChaptersAdapter(
	onItemClickListener: OnListItemClickListener<ChapterListItem>,
) : BaseListAdapter<ListModel>(), FastScroller.SectionIndexer {

	private var hasVolumes = false

	init {
		addDelegate(ListItemType.HEADER, listHeaderAD(null))
		addDelegate(ListItemType.CHAPTER_LIST, chapterListItemAD(onItemClickListener))
		addDelegate(ListItemType.CHAPTER_GRID, chapterGridItemAD(onItemClickListener))
	}

	override suspend fun emit(value: List<ListModel>?) {
		super.emit(value)
		hasVolumes = value != null && value.any { it is ListHeader }
	}

	override fun getSectionText(context: Context, position: Int): CharSequence? {
		return if (hasVolumes) {
			findHeader(position)?.getText(context)
		} else {
			val chapter = (items.getOrNull(position) as? ChapterListItem)?.chapter ?: return null
			chapter.numberString()
		}
	}
}
