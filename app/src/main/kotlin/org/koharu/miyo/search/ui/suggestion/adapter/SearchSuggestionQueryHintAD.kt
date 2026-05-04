package org.koharu.miyo.search.ui.suggestion.adapter

import android.view.View
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.databinding.ItemSearchSuggestionQueryHintBinding
import org.koharu.miyo.search.domain.SearchKind
import org.koharu.miyo.search.ui.suggestion.SearchSuggestionListener
import org.koharu.miyo.search.ui.suggestion.model.SearchSuggestionItem

fun searchSuggestionQueryHintAD(
	listener: SearchSuggestionListener,
) = adapterDelegateViewBinding<SearchSuggestionItem.Hint, SearchSuggestionItem, ItemSearchSuggestionQueryHintBinding>(
	{ inflater, parent -> ItemSearchSuggestionQueryHintBinding.inflate(inflater, parent, false) },
) {

	val viewClickListener = View.OnClickListener { _ ->
		listener.onQueryClick(item.query, SearchKind.SIMPLE, true)
	}

	binding.root.setOnClickListener(viewClickListener)

	bind {
		binding.root.text = item.query
	}
}
