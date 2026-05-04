package org.koharu.miyo.list.ui.adapter

import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.core.util.ext.setTooltipCompat
import org.koharu.miyo.core.util.ext.textAndVisible
import org.koharu.miyo.databinding.ItemMangaListBinding
import org.koharu.miyo.list.ui.model.ListModel
import org.koharu.miyo.list.ui.model.MangaCompactListModel
import org.koharu.miyo.list.ui.model.MangaListModel

fun mangaListItemAD(
	clickListener: OnListItemClickListener<MangaListModel>,
) = adapterDelegateViewBinding<MangaCompactListModel, ListModel, ItemMangaListBinding>(
	{ inflater, parent -> ItemMangaListBinding.inflate(inflater, parent, false) },
) {

	AdapterDelegateClickListenerAdapter(this, clickListener).attach(itemView)

	bind {
		itemView.setTooltipCompat(item.getSummary(context))
		binding.textViewTitle.text = item.title
		binding.textViewSubtitle.textAndVisible = item.subtitle
		binding.imageViewCover.setImageAsync(item.coverUrl, item.manga)
		binding.badge.number = item.counter
		binding.badge.isVisible = item.counter > 0
	}
}
