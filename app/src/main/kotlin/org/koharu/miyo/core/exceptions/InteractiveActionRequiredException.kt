package org.koharu.miyo.core.exceptions

import okio.IOException

class InteractiveActionRequiredException(
	val sourceName: String,
	val url: String,
) : IOException("Interactive action is required for $sourceName")
