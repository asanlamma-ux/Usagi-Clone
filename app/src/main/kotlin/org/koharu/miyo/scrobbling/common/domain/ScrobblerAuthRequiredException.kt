package org.koharu.miyo.scrobbling.common.domain

import okio.IOException
import org.koharu.miyo.scrobbling.common.domain.model.ScrobblerService

class ScrobblerAuthRequiredException(
	val scrobbler: ScrobblerService,
) : IOException()
