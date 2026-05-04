package org.koharu.miyo.favourites.ui.categories.select.model

import com.google.android.material.checkbox.MaterialCheckBox.CheckedState
import org.koharu.miyo.core.model.FavouriteCategory
import org.koharu.miyo.list.ui.ListModelDiffCallback
import org.koharu.miyo.list.ui.model.ListModel

data class MangaCategoryItem(
	val category: FavouriteCategory,
	@CheckedState val checkedState: Int,
	val isTrackerEnabled: Boolean,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is MangaCategoryItem && other.category.id == category.id
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is MangaCategoryItem && previousState.checkedState != checkedState) {
			ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}
}
