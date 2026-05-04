package org.koharu.miyo.bookmarks.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.bookmarks.domain.Bookmark
import org.koharu.miyo.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.databinding.ItemBookmarkLargeBinding
import org.koharu.miyo.list.ui.model.ListModel

fun bookmarkLargeAD(
	clickListener: OnListItemClickListener<Bookmark>,
) = adapterDelegateViewBinding<Bookmark, ListModel, ItemBookmarkLargeBinding>(
	{ inflater, parent -> ItemBookmarkLargeBinding.inflate(inflater, parent, false) },
) {
	AdapterDelegateClickListenerAdapter(this, clickListener).attach(itemView)

	bind {
		binding.imageViewThumb.setImageAsync(item)
		binding.progressView.setProgress(item.percent, false)
	}
}
