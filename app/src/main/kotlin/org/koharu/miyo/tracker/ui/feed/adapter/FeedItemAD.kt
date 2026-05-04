package org.koharu.miyo.tracker.ui.feed.adapter

import androidx.core.content.ContextCompat
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.R
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.core.util.ext.drawableStart
import org.koharu.miyo.core.util.ext.getQuantityStringSafe
import org.koharu.miyo.databinding.ItemFeedBinding
import org.koharu.miyo.list.ui.model.ListModel
import org.koharu.miyo.tracker.ui.feed.model.FeedItem

fun feedItemAD(
	clickListener: OnListItemClickListener<FeedItem>,
) = adapterDelegateViewBinding<FeedItem, ListModel, ItemFeedBinding>(
	{ inflater, parent -> ItemFeedBinding.inflate(inflater, parent, false) },
) {
	val indicatorNew = ContextCompat.getDrawable(context, R.drawable.ic_new)

	itemView.setOnClickListener {
		clickListener.onItemClick(item, it)
	}

	bind {
		binding.imageViewCover.setImageAsync(item.imageUrl, item.manga.source)
		binding.textViewTitle.text = item.title
		binding.textViewSummary.text = context.resources.getQuantityStringSafe(
			R.plurals.new_chapters,
			item.count,
			item.count,
		)
		binding.textViewSummary.drawableStart = if (item.isNew) {
			indicatorNew
		} else {
			null
		}
	}
}
