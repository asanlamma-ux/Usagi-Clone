package org.koharu.miyo.core.ui.util

import androidx.appcompat.view.ActionMode

interface ActionModeListener {

	fun onActionModeStarted(mode: ActionMode)

	fun onActionModeFinished(mode: ActionMode)
}
