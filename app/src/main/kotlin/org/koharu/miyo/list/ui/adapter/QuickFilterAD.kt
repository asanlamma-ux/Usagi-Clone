package org.koharu.miyo.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.core.ui.widgets.ChipsView
import org.koharu.miyo.databinding.ItemQuickFilterBinding
import org.koharu.miyo.list.domain.ListFilterOption
import org.koharu.miyo.list.ui.model.ListModel
import org.koharu.miyo.list.ui.model.QuickFilter

fun quickFilterAD(
	listener: QuickFilterClickListener,
) = adapterDelegateViewBinding<QuickFilter, ListModel, ItemQuickFilterBinding>(
	{ layoutInflater, parent -> ItemQuickFilterBinding.inflate(layoutInflater, parent, false) }
) {

	binding.chipsTags.onChipClickListener = ChipsView.OnChipClickListener { chip, data ->
		if (data is ListFilterOption) {
			listener.onFilterOptionClick(data)
		}
	}

	bind {
		binding.chipsTags.setChips(item.items)
	}
}
