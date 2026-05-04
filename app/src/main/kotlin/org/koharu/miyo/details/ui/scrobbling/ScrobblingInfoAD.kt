package org.koharu.miyo.details.ui.scrobbling

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.R
import org.koharu.miyo.core.nav.AppRouter
import org.koharu.miyo.databinding.ItemScrobblingInfoBinding
import org.koharu.miyo.list.ui.model.ListModel
import org.koharu.miyo.scrobbling.common.domain.model.ScrobblingInfo

fun scrobblingInfoAD(
	router: AppRouter,
) = adapterDelegateViewBinding<ScrobblingInfo, ListModel, ItemScrobblingInfoBinding>(
	{ layoutInflater, parent -> ItemScrobblingInfoBinding.inflate(layoutInflater, parent, false) },
) {
	binding.root.setOnClickListener {
		router.showScrobblingInfoSheet(bindingAdapterPosition)
	}

	bind {
		binding.imageViewCover.setImageAsync(item.coverUrl)
		binding.textViewTitle.setText(item.scrobbler.titleResId)
		binding.imageViewIcon.setImageResource(item.scrobbler.iconResId)
		binding.ratingBar.rating = item.rating * binding.ratingBar.numStars
		binding.textViewStatus.text = item.status?.let {
			context.resources.getStringArray(R.array.scrobbling_statuses).getOrNull(it.ordinal)
		}
	}
}
