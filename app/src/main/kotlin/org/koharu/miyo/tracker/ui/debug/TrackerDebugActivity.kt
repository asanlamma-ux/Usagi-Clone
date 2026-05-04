package org.koharu.miyo.tracker.ui.debug

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import org.koharu.miyo.core.nav.router
import org.koharu.miyo.core.ui.BaseActivity
import org.koharu.miyo.core.ui.BaseListAdapter
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.core.util.ext.consumeAllSystemBarsInsets
import org.koharu.miyo.core.util.ext.observe
import org.koharu.miyo.core.util.ext.systemBarsInsets
import org.koharu.miyo.databinding.ActivityTrackerDebugBinding
import org.koharu.miyo.list.ui.adapter.ListItemType
import org.koharu.miyo.list.ui.adapter.TypedListSpacingDecoration

@AndroidEntryPoint
class TrackerDebugActivity : BaseActivity<ActivityTrackerDebugBinding>(), OnListItemClickListener<TrackDebugItem> {

	private val viewModel by viewModels<TrackerDebugViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityTrackerDebugBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)
		val tracksAdapter = BaseListAdapter<TrackDebugItem>()
			.addDelegate(ListItemType.FEED, trackDebugAD(this))
		with(viewBinding.recyclerView) {
			setHasFixedSize(true)
			adapter = tracksAdapter
			addItemDecoration(TypedListSpacingDecoration(context, false))
		}
		viewModel.content.observe(this, tracksAdapter)
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.systemBarsInsets
		viewBinding.recyclerView.updatePadding(
			left = barsInsets.left,
			right = barsInsets.right,
			bottom = barsInsets.bottom,
		)
		viewBinding.appbar.updatePadding(
			left = barsInsets.left,
			right = barsInsets.right,
			top = barsInsets.top,
		)
		return insets.consumeAllSystemBarsInsets()
	}

	override fun onItemClick(item: TrackDebugItem, view: View) {
		router.openDetails(item.manga)
	}
}
