package org.koharu.miyo.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koharu.miyo.core.ui.BaseViewModel
import org.koharu.miyo.explore.data.MangaSourcesRepository
import javax.inject.Inject

@HiltViewModel
class RootSettingsViewModel @Inject constructor(
	private val sourcesRepository: MangaSourcesRepository,
) : BaseViewModel() {

	val totalSourcesCount: Int
		get() = sourcesRepository.allMangaSources.size

	val enabledSourcesCount = sourcesRepository.observeEnabledSourcesCount()
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, -1)
}
