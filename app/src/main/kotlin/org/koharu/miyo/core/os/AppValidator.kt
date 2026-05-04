package org.koharu.miyo.core.os

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import org.koharu.miyo.BuildConfig
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppValidator @Inject constructor(
	@ApplicationContext private val context: Context,
) {
	@SuppressLint("InlinedApi")
	val isReleaseSignatureTrusted = suspendLazy(Dispatchers.Default) {
		val certSha256 = BuildConfig.RELEASE_CERT_SHA256
			.filterNot { it == ':' || it.isWhitespace() }
			.uppercase(Locale.ROOT)
		val certBytes = runCatching {
			certSha256.takeIf { it.length == SHA256_HEX_LENGTH }?.hexToByteArray()
		}.getOrNull() ?: return@suspendLazy false
		val certificates = mapOf(certBytes to PackageManager.CERT_INPUT_SHA256)
		PackageInfoCompat.hasSignatures(context.packageManager, context.packageName, certificates, false)
	}

	private companion object {
		private const val SHA256_HEX_LENGTH = 64
	}
}
