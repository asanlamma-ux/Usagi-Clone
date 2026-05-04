package org.koharu.miyo.favourites.ui.categories

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.koharu.miyo.core.model.FavouriteCategory
import org.koharu.miyo.core.ui.list.OnListItemClickListener

interface FavouriteCategoriesListListener : OnListItemClickListener<FavouriteCategory?> {

	fun onDragHandleTouch(holder: RecyclerView.ViewHolder): Boolean

	fun onEditClick(item: FavouriteCategory, view: View)

	fun onShowAllClick(isChecked: Boolean)
}
