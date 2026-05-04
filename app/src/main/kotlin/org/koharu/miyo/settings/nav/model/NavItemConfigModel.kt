package org.koharu.miyo.settings.nav.model

import androidx.annotation.StringRes
import org.koharu.miyo.core.prefs.NavItem
import org.koharu.miyo.list.ui.model.ListModel

data class NavItemConfigModel(
	val item: NavItem,
	@StringRes val disabledHintResId: Int,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is NavItemConfigModel && other.item == item
	}
}
