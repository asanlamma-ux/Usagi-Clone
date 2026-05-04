package org.koharu.miyo.settings.about

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.koharu.miyo.core.github.AppUpdateRepository
import org.koharu.miyo.core.github.AppVersion
import org.koharu.miyo.core.ui.BaseViewModel
import org.koharu.miyo.core.util.ext.MutableEventFlow
import org.koharu.miyo.core.util.ext.call
import javax.inject.Inject

@HiltViewModel
class AboutSettingsViewModel @Inject constructor(
	private val appUpdateRepository: AppUpdateRepository,
) : BaseViewModel() {

	val isUpdateSupported = flow {
		emit(appUpdateRepository.isUpdateSupported())
	}.stateIn(viewModelScope, SharingStarted.Eagerly, false)

	val onUpdateAvailable = MutableEventFlow<AppVersion?>()

	fun checkForUpdates() {
		launchLoadingJob {
			val update = appUpdateRepository.fetchUpdate()
			onUpdateAvailable.call(update)
		}
	}
}
