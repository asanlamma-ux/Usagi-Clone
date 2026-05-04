package org.koharu.miyo.explore.ui

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import org.koharu.miyo.explore.ui.adapter.ExploreAdapter
import org.koharu.miyo.list.ui.adapter.ListItemType

class ExploreGridSpanSizeLookup(
	private val adapter: ExploreAdapter,
	private val layoutManager: GridLayoutManager,
) : SpanSizeLookup() {

	override fun getSpanSize(position: Int): Int {
		val itemType = adapter.getItemViewType(position)
		return if (itemType == ListItemType.EXPLORE_SOURCE_GRID.ordinal) 1 else layoutManager.spanCount
	}
}
