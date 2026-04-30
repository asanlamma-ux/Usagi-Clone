package org.draken.usagi.core.ui.util

import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuProvider
<<<<<<< HEAD
import org.draken.usagi.core.ui.OnContextClickListenerCompat
import org.draken.usagi.core.util.ext.setOnContextClickListenerCompat

class PopupMenuMediator(
	private val provider: MenuProvider,
) : View.OnLongClickListener, OnContextClickListenerCompat, PopupMenu.OnMenuItemClickListener,
=======

class PopupMenuMediator(
	private val provider: MenuProvider,
) : View.OnLongClickListener, View.OnContextClickListener, PopupMenu.OnMenuItemClickListener,
>>>>>>> abd49974e6e6c21783ada6501e12b3446c988ec6
	PopupMenu.OnDismissListener {

	override fun onContextClick(v: View): Boolean = onLongClick(v)

	override fun onLongClick(v: View): Boolean {
		val menu = PopupMenu(v.context, v)
		provider.onCreateMenu(menu.menu, menu.menuInflater)
		provider.onPrepareMenu(menu.menu)
		if (!menu.menu.hasVisibleItems()) {
			return false
		}
		menu.setOnMenuItemClickListener(this)
		menu.setOnDismissListener(this)
		menu.show()
		return true
	}

	override fun onMenuItemClick(item: MenuItem): Boolean {
		return provider.onMenuItemSelected(item)
	}

	override fun onDismiss(menu: PopupMenu) {
		provider.onMenuClosed(menu.menu)
	}

	fun attach(view: View) {
		view.setOnLongClickListener(this)
<<<<<<< HEAD
		view.setOnContextClickListenerCompat(this)
=======
		view.setOnContextClickListener(this)
>>>>>>> abd49974e6e6c21783ada6501e12b3446c988ec6
	}
}
