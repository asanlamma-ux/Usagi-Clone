package org.koharu.miyo.explore.ui.adapter

import android.view.View
import org.koharu.miyo.list.ui.adapter.ListHeaderClickListener
import org.koharu.miyo.list.ui.adapter.ListStateHolderListener

interface ExploreListEventListener : ListStateHolderListener, View.OnClickListener, ListHeaderClickListener
