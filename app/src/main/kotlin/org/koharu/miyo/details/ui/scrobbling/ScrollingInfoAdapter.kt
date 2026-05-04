package org.koharu.miyo.details.ui.scrobbling

import org.koharu.miyo.core.nav.AppRouter
import org.koharu.miyo.core.ui.BaseListAdapter
import org.koharu.miyo.list.ui.model.ListModel

class ScrollingInfoAdapter(
	router: AppRouter,
) : BaseListAdapter<ListModel>() {

	init {
		delegatesManager.addDelegate(scrobblingInfoAD(router))
	}
}
