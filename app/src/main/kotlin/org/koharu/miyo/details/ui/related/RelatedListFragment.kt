package org.koharu.miyo.details.ui.related

import android.view.Menu
import android.view.MenuInflater
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.koharu.miyo.R
import org.koharu.miyo.core.ui.list.ListSelectionController
import org.koharu.miyo.list.ui.MangaListFragment

@AndroidEntryPoint
class RelatedListFragment : MangaListFragment() {

	override val viewModel by viewModels<RelatedListViewModel>()
	override val isSwipeRefreshEnabled = false

	override fun onScrolledToEnd() = Unit

	override fun onCreateActionMode(
		controller: ListSelectionController,
		menuInflater: MenuInflater,
		menu: Menu
	): Boolean {
		menuInflater.inflate(R.menu.mode_remote, menu)
		return super.onCreateActionMode(controller, menuInflater, menu)
	}
}

