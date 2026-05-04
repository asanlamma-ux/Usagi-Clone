package org.koharu.miyo.core.ui

import android.view.View

fun interface OnContextClickListenerCompat {

	fun onContextClick(v: View): Boolean
}
