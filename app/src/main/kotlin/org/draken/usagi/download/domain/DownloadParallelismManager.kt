package org.draken.usagi.download.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadParallelismManager @Inject constructor(
	@ApplicationContext private val context: Context,
) {
	fun resolveParallelism(sourceOverride: Int?): Int {
		if (sourceOverride != null && sourceOverride in MIN_PARALLELISM..MAX_PARALLELISM) {
			return sourceOverride
		}
		val base = when {
			isWiFi() -> WIFI_BASE
			isCellular() -> CELLULAR_BASE
			else -> FALLBACK_BASE
		}
		val cores = Runtime.getRuntime().availableProcessors()
		return when {
			cores >= HIGH_CORE_COUNT -> (base * 2).coerceAtMost(MAX_PARALLELISM)
			cores >= MEDIUM_CORE_COUNT -> base
			else -> base.coerceAtMost(MIN_PARALLELISM)
		}
	}

	fun describeDeviceTier(): DeviceTier {
		val cores = Runtime.getRuntime().availableProcessors()
		val memMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
		return when {
			cores >= HIGH_CORE_COUNT && memMb >= HIGH_MEM_MB -> DeviceTier.HIGH
			cores >= MEDIUM_CORE_COUNT -> DeviceTier.MEDIUM
			else -> DeviceTier.LOW
		}
	}

	private fun isWiFi(): Boolean {
		val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
		val network = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			cm.activeNetwork
		} else {
			null
		}
		val caps = cm.getNetworkCapabilities(network) ?: return false
		return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
			caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
	}

	private fun isCellular(): Boolean {
		val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
		val network = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			cm.activeNetwork
		} else {
			null
		}
		val caps = cm.getNetworkCapabilities(network) ?: return false
		return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
	}

	enum class DeviceTier { LOW, MEDIUM, HIGH }

	companion object {
		const val MIN_PARALLELISM = 2
		const val MAX_PARALLELISM = 8
		private const val WIFI_BASE = 6
		private const val CELLULAR_BASE = 4
		private const val FALLBACK_BASE = 3
		private const val HIGH_CORE_COUNT = 8
		private const val MEDIUM_CORE_COUNT = 4
		private const val HIGH_MEM_MB = 2048
	}
}