package org.koharu.miyo.browser

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.koharu.miyo.core.network.webview.adblock.AdBlock
import org.koharu.miyo.core.ui.CoroutineIntentService
import javax.inject.Inject

@AndroidEntryPoint
class AdListUpdateService : CoroutineIntentService() {

	@Inject
	lateinit var updater: AdBlock.Updater

	override suspend fun IntentJobContext.processIntent(intent: Intent) {
		updater.updateList()
	}

	override fun IntentJobContext.onError(error: Throwable) = Unit
}
