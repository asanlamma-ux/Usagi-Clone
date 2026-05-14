package org.koharu.miyo.browser.cloudflare

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import org.koharu.miyo.browser.BrowserClient
import org.koharu.miyo.core.network.cookies.MutableCookieJar
import org.koharu.miyo.core.network.webview.CaptchaNavigationGuard
import org.koharu.miyo.core.network.webview.adblock.AdBlock
import org.koitharu.kotatsu.parsers.network.CloudFlareHelper

class CloudFlareClient(
	private val cookieJar: MutableCookieJar,
	private val callback: CloudFlareCallback,
	adBlock: AdBlock,
	private val targetUrl: String,
) : BrowserClient(callback, adBlock) {

	private val oldClearance = getClearance()

	override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
		super.onPageStarted(view, url, favicon)
		checkClearance()
	}

	override fun onPageCommitVisible(view: WebView, url: String) {
		super.onPageCommitVisible(view, url)
		callback.onPageLoaded()
	}

	override fun onPageFinished(webView: WebView, url: String) {
		super.onPageFinished(webView, url)
		callback.onPageLoaded()
		checkClearance()
	}

	@Deprecated("Deprecated in Java")
	override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
		if (CaptchaNavigationGuard.shouldBlockMainFrameNavigation(url, targetUrl)) {
			callback.onPageLoaded()
			return true
		}
		return super.shouldOverrideUrlLoading(view, url)
	}

	override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
		val isMainFrame = request?.isForMainFrame ?: true
		if (isMainFrame && CaptchaNavigationGuard.shouldBlockMainFrameNavigation(request?.url?.toString(), targetUrl)) {
			callback.onPageLoaded()
			return true
		}
		return super.shouldOverrideUrlLoading(view, request)
	}

	private fun checkClearance() {
		val clearance = getClearance()
		if (clearance != null && clearance != oldClearance) {
			callback.onCheckPassed()
		}
	}

	private fun getClearance() = CloudFlareHelper.getClearanceCookie(cookieJar, targetUrl)
}
