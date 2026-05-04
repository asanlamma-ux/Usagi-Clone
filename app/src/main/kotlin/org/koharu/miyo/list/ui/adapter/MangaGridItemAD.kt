package org.koharu.miyo.list.ui.adapter

import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.R
import org.koharu.miyo.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.core.util.ext.setTooltipCompat
import org.koharu.miyo.databinding.ItemMangaGridBinding
import org.koharu.miyo.list.ui.ListModelDiffCallback.Companion.PAYLOAD_PROGRESS_CHANGED
import org.koharu.miyo.list.ui.model.ListModel
import org.koharu.miyo.list.ui.model.MangaGridModel
import org.koharu.miyo.list.ui.model.MangaListModel
import org.koharu.miyo.list.ui.size.ItemSizeResolver

fun mangaGridItemAD(
	sizeResolver: ItemSizeResolver,
	clickListener: OnListItemClickListener<MangaListModel>,
) = adapterDelegateViewBinding<MangaGridModel, ListModel, ItemMangaGridBinding>(
	{ inflater, parent -> ItemMangaGridBinding.inflate(inflater, parent, false) },
) {

	AdapterDelegateClickListenerAdapter(this, clickListener).attach(itemView)
	sizeResolver.attachToView(itemView, binding.textViewTitle, binding.progressView)

	bind { payloads ->
		itemView.setTooltipCompat(item.getSummary(context))
		binding.textViewTitle.text = item.title
		binding.textViewTitle.isVisible = !item.isTitleHidden
		binding.progressView.setProgress(item.progress, PAYLOAD_PROGRESS_CHANGED in payloads)
		with(binding.iconsView) {
			clearIcons()
			if (item.isSaved) addIcon(R.drawable.ic_storage)
			if (item.isFavorite) addIcon(R.drawable.ic_heart_outline)
			isVisible = iconsCount > 0
		}
		binding.imageViewCover.setImageAsync(item.coverUrl, item.manga)
		binding.badge.number = item.counter
		binding.badge.isVisible = item.counter > 0
	}
}
