package org.koharu.miyo.favourites.ui

import android.os.Bundle
import org.koharu.miyo.core.nav.AppRouter
import org.koharu.miyo.core.ui.FragmentContainerActivity
import org.koharu.miyo.favourites.ui.list.FavouritesListFragment

class FavouritesActivity : FragmentContainerActivity(FavouritesListFragment::class.java) {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val categoryTitle = intent.getStringExtra(AppRouter.KEY_TITLE)
		if (categoryTitle != null) {
			title = categoryTitle
		}
	}
}
