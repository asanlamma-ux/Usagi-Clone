package org.draken.usagi.core.network

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class CacheLimitInterceptor : Interceptor {

<<<<<<< HEAD
	private val imageMaxAge = TimeUnit.HOURS.toSeconds(4)
	private val defaultMaxAge = TimeUnit.HOURS.toSeconds(24)

	private val imageCacheControl = CacheControl.Builder()
		.maxAge(imageMaxAge.toInt(), TimeUnit.SECONDS)
		.build()
		.toString()

=======
	private val defaultMaxAge = TimeUnit.HOURS.toSeconds(1)
>>>>>>> abd49974e6e6c21783ada6501e12b3446c988ec6
	private val defaultCacheControl = CacheControl.Builder()
		.maxAge(defaultMaxAge.toInt(), TimeUnit.SECONDS)
		.build()
		.toString()

<<<<<<< HEAD
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
=======
	override fun intercept(chain: Interceptor.Chain): Response {
		val response = chain.proceed(chain.request())
		val responseCacheControl = CacheControl.parse(response.headers)
		if (responseCacheControl.noStore || responseCacheControl.maxAgeSeconds <= defaultMaxAge) {
			return response
		}
		return response.newBuilder()
			.header(CommonHeaders.CACHE_CONTROL, defaultCacheControl)
>>>>>>> abd49974e6e6c21783ada6501e12b3446c988ec6
			.build()
	}
}
