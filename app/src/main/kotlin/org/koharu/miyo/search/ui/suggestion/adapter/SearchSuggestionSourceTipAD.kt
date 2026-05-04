package org.koharu.miyo.search.ui.suggestion.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.core.model.getSummary
import org.koharu.miyo.core.model.getTitle
import org.koharu.miyo.databinding.ItemSearchSuggestionSourceTipBinding
import org.koharu.miyo.search.ui.suggestion.SearchSuggestionListener
import org.koharu.miyo.search.ui.suggestion.model.SearchSuggestionItem

fun searchSuggestionSourceTipAD(
	listener: SearchSuggestionListener,
) =
	adapterDelegateViewBinding<SearchSuggestionItem.SourceTip, SearchSuggestionItem, ItemSearchSuggestionSourceTipBinding>(
		{ inflater, parent -> ItemSearchSuggestionSourceTipBinding.inflate(inflater, parent, false) },
	) {

		binding.root.setOnClickListener {
			listener.onSourceClick(item.source)
		}

		bind {
			binding.textViewTitle.text = item.source.getTitle(context)
			binding.textViewSubtitle.text = item.source.getSummary(context)
			binding.imageViewCover.setImageAsync(item.source)
		}
	}
