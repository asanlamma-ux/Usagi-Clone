package org.koharu.miyo.core.exceptions

class SyncApiException(
	message: String,
	val code: Int,
) : RuntimeException(message)
