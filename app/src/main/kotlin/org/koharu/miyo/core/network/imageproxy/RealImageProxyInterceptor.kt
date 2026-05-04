package org.koharu.miyo.core.network.imageproxy

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koharu.miyo.core.prefs.AppSettings
import org.koharu.miyo.core.prefs.observeAsStateFlow
import org.koharu.miyo.core.util.ext.processLifecycleScope
import org.koitharu.kotatsu.parsers.util.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealImageProxyInterceptor @Inject constructor(
	private val settings: AppSettings,
) : ImageProxyInterceptor {

	private val delegates = settings.observeAsStateFlow(
		scope = processLifecycleScope + Dispatchers.Default,
		key = AppSettings.KEY_IMAGES_PROXY,
		valueProducer = { createDelegates() },
	)

	override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
		return delegates.value.firstOrNull()?.intercept(chain) ?: chain.proceed()
	}

	override suspend fun interceptPageRequest(request: Request, okHttp: OkHttpClient): Response {
		val currentDelegates = delegates.value
		if (currentDelegates.isEmpty()) {
			return okHttp.newCall(request).await()
		}
		for (delegate in currentDelegates) {
			try {
				return delegate.interceptPageRequest(request, okHttp)
			} catch (e: CancellationException) {
				throw e
			} catch (_: Exception) {
				// Try the next configured proxy before falling back to a direct request.
			}
		}
		return okHttp.newCall(request).await()
	}

	private fun createDelegates(): List<ImageProxyInterceptor> {
		val selectedProxy = settings.imagesProxy
		if (selectedProxy == PROXY_DISABLED) {
			return emptyList()
		}
		return (listOf(selectedProxy) + SUPPORTED_PROXIES.filterNot { it == selectedProxy })
			.map { createDelegate(it) }
	}

	private fun createDelegate(proxy: Int): ImageProxyInterceptor = when (proxy) {
		PROXY_WSRV_NL -> WsrvNlProxyInterceptor()
		PROXY_ZERO_MS -> ZeroMsProxyInterceptor()
		else -> error("Unsupported images proxy $proxy")
	}

	private companion object {

		const val PROXY_DISABLED = -1
		const val PROXY_WSRV_NL = 0
		const val PROXY_ZERO_MS = 1
		val SUPPORTED_PROXIES = listOf(PROXY_WSRV_NL, PROXY_ZERO_MS)
	}
}
