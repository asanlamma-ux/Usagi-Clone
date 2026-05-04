package org.koharu.miyo.widget.shelf.adapter

import org.koharu.miyo.core.ui.BaseListAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.widget.shelf.model.CategoryItem

class CategorySelectAdapter(
	clickListener: OnListItemClickListener<CategoryItem>
) : BaseListAdapter<CategoryItem>() {

	init {
		delegatesManager.addDelegate(categorySelectItemAD(clickListener))
	}
}
