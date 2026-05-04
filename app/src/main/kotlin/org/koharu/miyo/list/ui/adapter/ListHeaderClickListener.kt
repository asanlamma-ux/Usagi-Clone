package org.koharu.miyo.list.ui.adapter

import android.view.View
import org.koharu.miyo.list.ui.model.ListHeader

interface ListHeaderClickListener {

	fun onListHeaderClick(item: ListHeader, view: View)
}
