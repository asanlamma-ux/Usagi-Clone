package org.draken.usagi.core.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.koitharu.kotatsu.parsers.exception.TooManyRequestExceptions
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class RateLimitInterceptor : Interceptor {
	override fun intercept(chain: Interceptor.Chain): Response {
		val response = chain.proceed(chain.request())
		if (response.code == 429) {
			val request = response.request
			response.closeQuietly()
			throw TooManyRequestExceptions(
				url = request.url.toString(),
				retryAfter = response.header(CommonHeaders.RETRY_AFTER)?.parseRetryAfter() ?: 0L,
			)
		}
		return response
	}

	private fun String.parseRetryAfter(): Long {
		return toLongOrNull()?.let { TimeUnit.SECONDS.toMillis(it) } ?: runCatching {
			ZonedDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
		}.getOrDefault(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(DEFAULT_RETRY_SECONDS))
	}

	private companion object {
		const val DEFAULT_RETRY_SECONDS = 5L
	}
}
