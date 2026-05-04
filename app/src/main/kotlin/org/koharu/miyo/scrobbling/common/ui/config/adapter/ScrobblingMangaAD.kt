package org.koharu.miyo.scrobbling.common.ui.config.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.core.ui.list.AdapterDelegateClickListenerAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.databinding.ItemScrobblingMangaBinding
import org.koharu.miyo.list.ui.model.ListModel
import org.koharu.miyo.scrobbling.common.domain.model.ScrobblingInfo

fun scrobblingMangaAD(
	clickListener: OnListItemClickListener<ScrobblingInfo>,
) = adapterDelegateViewBinding<ScrobblingInfo, ListModel, ItemScrobblingMangaBinding>(
	{ layoutInflater, parent -> ItemScrobblingMangaBinding.inflate(layoutInflater, parent, false) },
) {

	AdapterDelegateClickListenerAdapter(this, clickListener).attach(itemView)

	bind {
		binding.imageViewCover.setImageAsync(item.coverUrl, null)
		binding.textViewTitle.text = item.title
		binding.ratingBar.rating = item.rating * binding.ratingBar.numStars
	}
}
