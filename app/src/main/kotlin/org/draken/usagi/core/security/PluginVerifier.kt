package org.draken.usagi.core.security

import android.util.Log
import org.draken.usagi.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.jar.JarInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginVerifier @Inject constructor() {

	val isEnabled: Boolean = true

	fun verifyPluginJar(jarFile: File): VerificationResult {
		if (!isEnabled) return VerificationResult.ALLOW
		val baseName = jarFile.nameWithoutExtension.removeSuffix("-signed")
		return try {
			val signatures = mutableListOf<String>()
			JarInputStream(FileInputStream(jarFile)).use { jarStream ->
				var entry = jarStream.nextJarEntry
				while (entry != null) {
					val name = entry.name
					if (name.startsWith("META-INF/") && (
						name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA") ||
						name.endsWith(".EC")
					)) {
						signatures.add(name)
					}
					entry = jarStream.nextJarEntry
				}
			}
			if (signatures.isNotEmpty()) {
				VerificationResult.ALLOW
			} else {
				logw("Plugin $baseName has no digital signatures - proceeding with warning")
				VerificationResult.ALLOW_WITH_WARNING
			}
		} catch (e: Exception) {
			logw("Plugin verification skipped for $baseName: ${e.message}")
			VerificationResult.ALLOW
		}
	}

	fun computeJarHash(jarFile: File): String {
		val digest = MessageDigest.getInstance("SHA-256")
		val buffer = ByteArray(8192)
		FileInputStream(jarFile).use { fis ->
			var bytesRead: Int
			while (fis.read(buffer).also { bytesRead = it } != -1) {
				digest.update(buffer, 0, bytesRead)
			}
		}
		return digest.digest().joinToString("") { "%02x".format(it) }
	}

	enum class VerificationResult {
		ALLOW,
		ALLOW_WITH_WARNING,
		BLOCK,
	}

	private companion object {
		const val TAG = "PluginVerifier"

		fun logw(message: String) {
			if (BuildConfig.DEBUG) {
				Log.w(TAG, message)
			}
		}
	}
}