package org.koharu.miyo.list.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import org.koharu.miyo.R
import org.koharu.miyo.list.ui.model.ListModel
import org.koharu.miyo.list.ui.model.LoadingFooter

fun loadingFooterAD() = adapterDelegate<LoadingFooter, ListModel>(R.layout.item_loading_footer) {
}