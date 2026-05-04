package org.koharu.miyo.stats.ui

import android.content.res.ColorStateList
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import org.koharu.miyo.R
import org.koharu.miyo.core.ui.list.OnListItemClickListener
import org.koharu.miyo.core.util.MiyoColors
import org.koharu.miyo.databinding.ItemStatsBinding
import org.koitharu.kotatsu.parsers.model.Manga
import org.koharu.miyo.stats.domain.StatsRecord

fun statsAD(
	listener: OnListItemClickListener<Manga>,
) = adapterDelegateViewBinding<StatsRecord, StatsRecord, ItemStatsBinding>(
	{ layoutInflater, parent -> ItemStatsBinding.inflate(layoutInflater, parent, false) },
) {

	binding.root.setOnClickListener { v ->
		listener.onItemClick(item.manga ?: return@setOnClickListener, v)
	}

	bind {
		binding.textViewTitle.text = item.manga?.title ?: getString(R.string.other_manga)
		binding.textViewSummary.text = item.time.format(context.resources)
		binding.imageViewBadge.imageTintList = ColorStateList.valueOf(MiyoColors.ofManga(context, item.manga))
		binding.root.isClickable = item.manga != null
	}
}
