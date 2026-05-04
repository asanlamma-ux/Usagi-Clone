package org.koharu.miyo.settings.storage

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.core.util.ext.textAndVisible
import org.koharu.miyo.databinding.ItemStorageBinding

fun directoryAD(
	clickListener: OnListItemClickListener<DirectoryModel>,
) = adapterDelegateViewBinding<DirectoryModel, DirectoryModel, ItemStorageBinding>(
	{ layoutInflater, parent -> ItemStorageBinding.inflate(layoutInflater, parent, false) },
) {

	binding.root.setOnClickListener { v -> clickListener.onItemClick(item, v) }

	bind {
		binding.textViewTitle.text = item.title ?: getString(item.titleRes)
		binding.textViewSubtitle.textAndVisible = item.file?.absolutePath
		binding.imageViewIndicator.isChecked = item.isChecked
	}
}
