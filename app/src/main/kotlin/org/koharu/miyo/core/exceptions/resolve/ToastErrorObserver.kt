package org.koharu.miyo.core.exceptions.resolve

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.koharu.miyo.core.util.ext.getDisplayMessage

class ToastErrorObserver(
	host: View,
	fragment: Fragment?,
) : ErrorObserver(host, fragment, null, null) {

	override suspend fun emit(value: Throwable) {
		val toast = Toast.makeText(host.context.applicationContext, value.getDisplayMessage(host.context.resources), Toast.LENGTH_SHORT)
		toast.show()
	}
}
