package org.koharu.miyo.reader.ui.pager.webtoon

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import org.koharu.miyo.core.exceptions.resolve.ExceptionResolver
import org.koharu.miyo.core.os.NetworkState
import org.koharu.miyo.databinding.ItemPageWebtoonBinding
import org.koharu.miyo.reader.domain.PageLoader
import org.koharu.miyo.reader.ui.config.ReaderSettings
import org.koharu.miyo.reader.ui.pager.BaseReaderAdapter

class WebtoonAdapter(
	private val lifecycleOwner: LifecycleOwner,
	loader: PageLoader,
	readerSettingsProducer: ReaderSettings.Producer,
	networkState: NetworkState,
	exceptionResolver: ExceptionResolver,
) : BaseReaderAdapter<WebtoonHolder>(loader, readerSettingsProducer, networkState, exceptionResolver) {

	override fun onCreateViewHolder(
		parent: ViewGroup,
		loader: PageLoader,
		readerSettingsProducer: ReaderSettings.Producer,
		networkState: NetworkState,
		exceptionResolver: ExceptionResolver,
	) = WebtoonHolder(
		owner = lifecycleOwner,
		binding = ItemPageWebtoonBinding.inflate(
			LayoutInflater.from(parent.context),
			parent,
			false,
		),
		loader = loader,
		readerSettingsProducer = readerSettingsProducer,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
	)
}
