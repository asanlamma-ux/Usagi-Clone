package org.koharu.miyo.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.core.util.ext.getDisplayMessage
import org.koharu.miyo.databinding.ItemErrorFooterBinding
import org.koharu.miyo.list.ui.model.ErrorFooter
import org.koharu.miyo.list.ui.model.ListModel

fun errorFooterAD(
	listener: ListStateHolderListener?,
) = adapterDelegateViewBinding<ErrorFooter, ListModel, ItemErrorFooterBinding>(
	{ inflater, parent -> ItemErrorFooterBinding.inflate(inflater, parent, false) },
) {

	if (listener != null) {
		binding.root.setOnClickListener {
			listener.onRetryClick(item.exception)
		}
	}

	bind {
		binding.textViewTitle.text = item.exception.getDisplayMessage(context.resources)
	}
}
