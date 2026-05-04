package org.koharu.miyo.settings.storage

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.koharu.miyo.R
import org.koharu.miyo.core.prefs.AppSettings
import org.koharu.miyo.core.ui.BaseViewModel
import org.koharu.miyo.core.util.ext.MutableEventFlow
import org.koharu.miyo.core.util.ext.call
import org.koharu.miyo.core.util.ext.isWriteable
import org.koharu.miyo.local.data.LocalStorageManager
import javax.inject.Inject

@HiltViewModel
class MangaDirectorySelectViewModel @Inject constructor(
	private val storageManager: LocalStorageManager,
	private val settings: AppSettings,
) : BaseViewModel() {

	val items = MutableStateFlow(emptyList<DirectoryModel>())
	val onDismissDialog = MutableEventFlow<Unit>()
	val onPickDirectory = MutableEventFlow<Unit>()

	init {
		refresh()
	}

	fun onItemClick(item: DirectoryModel) {
		if (item.file != null) {
			settings.mangaStorageDir = item.file
			onDismissDialog.call(Unit)
		} else {
			onPickDirectory.call(Unit)
		}
	}

	fun onCustomDirectoryPicked(uri: Uri) {
		launchJob(Dispatchers.Default) {
			storageManager.takePermissions(uri)
			val dir = storageManager.resolveUri(uri)
			if (!dir.isWriteable()) {
				throw AccessDeniedException(dir)
			}
			if (dir !in storageManager.getApplicationStorageDirs()) {
				settings.mangaStorageDir = dir
				storageManager.setDirIsNoMedia(dir)
			}
			onDismissDialog.call(Unit)
		}
	}

	fun refresh() {
		launchJob(Dispatchers.Default) {
			val defaultValue = storageManager.getDefaultWriteableDir()
			val available = storageManager.getWriteableDirs()
			items.value = buildList(available.size + 1) {
				available.mapTo(this) { dir ->
					DirectoryModel(
						title = storageManager.getDirectoryDisplayName(dir, isFullPath = false),
						titleRes = 0,
						file = dir,
						isChecked = dir == defaultValue,
						isAvailable = true,
						isRemovable = false,
					)
				}
				this += DirectoryModel(
					title = null,
					titleRes = R.string.pick_custom_directory,
					file = null,
					isChecked = false,
					isAvailable = true,
					isRemovable = false,
				)
			}
		}
	}
}
