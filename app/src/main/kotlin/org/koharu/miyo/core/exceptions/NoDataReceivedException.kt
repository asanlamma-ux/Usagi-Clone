package org.koharu.miyo.core.exceptions

import okio.IOException

class NoDataReceivedException(
	val url: String,
) : IOException("No data has been received from $url")
