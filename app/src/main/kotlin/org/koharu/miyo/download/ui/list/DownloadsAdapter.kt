package org.koharu.miyo.download.ui.list

import androidx.lifecycle.LifecycleOwner
import org.koharu.miyo.core.ui.BaseListAdapter
import org.koharu.miyo.list.ui.adapter.ListItemType
import org.koharu.miyo.list.ui.adapter.emptyStateListAD
import org.koharu.miyo.list.ui.adapter.listHeaderAD
import org.koharu.miyo.list.ui.adapter.loadingStateAD
import org.koharu.miyo.list.ui.model.ListModel

class DownloadsAdapter(
	lifecycleOwner: LifecycleOwner,
	listener: DownloadItemListener,
) : BaseListAdapter<ListModel>() {

	init {
		addDelegate(ListItemType.DOWNLOAD, downloadItemAD(lifecycleOwner, listener))
		addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
		addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(null))
		addDelegate(ListItemType.HEADER, listHeaderAD(null))
	}
}
