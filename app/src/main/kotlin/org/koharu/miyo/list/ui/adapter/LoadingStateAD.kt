package org.koharu.miyo.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import org.koharu.miyo.R
import org.koharu.miyo.list.ui.model.ListModel
import org.koharu.miyo.list.ui.model.LoadingState

fun loadingStateAD() = adapterDelegate<LoadingState, ListModel>(R.layout.item_loading_state) {
}