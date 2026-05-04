package org.koharu.miyo.favourites.ui.categories.select

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.koharu.miyo.R
import org.koharu.miyo.core.nav.router
import org.koharu.miyo.core.ui.AlertDialogFragment
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.core.util.ext.getDisplayMessage
import org.koharu.miyo.core.util.ext.joinToStringWithLimit
import org.koharu.miyo.core.util.ext.observe
import org.koharu.miyo.core.util.ext.observeEvent
import org.koharu.miyo.databinding.DialogFavoriteBinding
import org.koharu.miyo.favourites.ui.categories.select.adapter.MangaCategoriesAdapter
import org.koharu.miyo.favourites.ui.categories.select.model.MangaCategoryItem

@AndroidEntryPoint
class FavoriteDialog : AlertDialogFragment<DialogFavoriteBinding>(),
	OnListItemClickListener<MangaCategoryItem>, DialogInterface.OnClickListener {

	private val viewModel by viewModels<FavoriteDialogViewModel>()

	override fun onCreateViewBinding(
		inflater: LayoutInflater,
		container: ViewGroup?,
	) = DialogFavoriteBinding.inflate(inflater, container, false)

	override fun onBuildDialog(builder: MaterialAlertDialogBuilder): MaterialAlertDialogBuilder {
		return super.onBuildDialog(builder)
			.setPositiveButton(R.string.done, null)
			.setNeutralButton(R.string.manage, this)
	}

	override fun onViewBindingCreated(
		binding: DialogFavoriteBinding,
		savedInstanceState: Bundle?,
	) {
		super.onViewBindingCreated(binding, savedInstanceState)
		val adapter = MangaCategoriesAdapter(this)
		binding.recyclerViewCategories.adapter = adapter
		viewModel.content.observe(viewLifecycleOwner, adapter)
		viewModel.onError.observeEvent(viewLifecycleOwner, ::onError)
		bindHeader()
	}

	override fun onItemClick(item: MangaCategoryItem, view: View) {
		viewModel.setChecked(item.category.id, item.checkedState != MaterialCheckBox.STATE_CHECKED)
	}

	override fun onClick(dialog: DialogInterface?, which: Int) {
		router.openFavoriteCategories()
	}

	private fun onError(e: Throwable) {
		Toast.makeText(context ?: return, e.getDisplayMessage(resources), Toast.LENGTH_SHORT).show()
	}

	private fun bindHeader() {
		val manga = viewModel.manga
		val binding = viewBinding ?: return
		binding.textViewTitle.text = manga.joinToStringWithLimit(binding.root.context, 92) { it.title }
		binding.coversStack.setCoversAsync(manga)
	}
}
