package org.koharu.miyo.core.network

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class CacheLimitInterceptor : Interceptor {

	private val imageMaxAge = TimeUnit.HOURS.toSeconds(4)
	private val defaultMaxAge = TimeUnit.HOURS.toSeconds(24)

	private val imageCacheControl = CacheControl.Builder()
		.maxAge(imageMaxAge.toInt(), TimeUnit.SECONDS)
		.build()
		.toString()

	private val defaultCacheControl = CacheControl.Builder()
		.maxAge(defaultMaxAge.toInt(), TimeUnit.SECONDS)
		.build()
		.toString()

	private val staticAssetDomains = setOf(
		"github.com",
		"githubusercontent.com",
		"gitlab.com",
	)

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()
		val response = chain.proceed(request)
		val responseCacheControl = CacheControl.parse(response.headers)
		if (responseCacheControl.noStore) {
			return response
		}

		val host = request.url.host
		val isStaticAsset = staticAssetDomains.any { host.endsWith(it) }
		if (isStaticAsset) {
			return response
		}

		val isImage = response.header(CommonHeaders.CONTENT_TYPE)?.startsWith("image/") == true
		val maxAge = if (isImage) imageMaxAge else defaultMaxAge

		if (responseCacheControl.maxAgeSeconds in 0..maxAge) {
			return response
		}

		val newCacheControl = if (isImage) imageCacheControl else defaultCacheControl
		return response.newBuilder()
			.header(CommonHeaders.CACHE_CONTROL, newCacheControl)
			.build()
	}
}
