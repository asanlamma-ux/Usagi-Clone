package org.koharu.miyo.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.core.util.ext.setTextAndVisible
import org.koharu.miyo.databinding.ItemInfoBinding
import org.koharu.miyo.list.ui.model.InfoModel
import org.koharu.miyo.list.ui.model.ListModel

fun infoAD() = adapterDelegateViewBinding<InfoModel, ListModel, ItemInfoBinding>(
	{ layoutInflater, parent -> ItemInfoBinding.inflate(layoutInflater, parent, false) },
) {

	bind {
		binding.textViewTitle.setText(item.title)
		binding.textViewBody.setTextAndVisible(item.text)
		binding.textViewTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
			item.icon, 0, 0, 0,
		)
	}
}
