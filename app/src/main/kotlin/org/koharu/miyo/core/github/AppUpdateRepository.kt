package org.koharu.miyo.core.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.koharu.miyo.BuildConfig
import org.koharu.miyo.core.network.BaseHttpClient
import org.koharu.miyo.core.os.AppValidator
import org.koharu.miyo.core.prefs.AppSettings
import org.koharu.miyo.core.util.ext.asArrayList
import org.koharu.miyo.core.util.ext.printStackTraceDebug
import org.koitharu.kotatsu.parsers.util.await
import org.koitharu.kotatsu.parsers.util.json.mapJSONNotNull
import org.koitharu.kotatsu.parsers.util.parseJsonArray
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.parsers.util.suspendlazy.getOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val CONTENT_TYPE_APK = "application/vnd.android.package-archive"
private const val BUILD_TYPE_RELEASE = "release"

@Singleton
class AppUpdateRepository @Inject constructor(
	private val appValidator: AppValidator,
	private val settings: AppSettings,
	@BaseHttpClient private val okHttp: OkHttpClient,
) {

	private val availableUpdate = MutableStateFlow<AppVersion?>(null)
	private val releasesUrl = buildString {
		append("https://api.github.com/repos/")
		append(BuildConfig.UPDATES_REPO)
		append("/releases?page=1&per_page=10")
	}

	val isUpdateAvailable: Boolean
		get() = availableUpdate.value != null

	fun observeAvailableUpdate() = availableUpdate.asStateFlow()

	suspend fun getAvailableVersions(): List<AppVersion> {
		val request = Request.Builder()
			.get()
			.url(releasesUrl)
		val jsonArray = okHttp.newCall(request.build()).await().parseJsonArray()
		return jsonArray.mapJSONNotNull { json ->
			val asset = json.optJSONArray("assets")?.findBestApkAsset() ?: return@mapJSONNotNull null
			AppVersion(
				id = json.getLong("id"),
				url = json.getString("html_url"),
				name = json.optString("name").ifBlank { json.getString("tag_name") }.removePrefix("v"),
				apkSize = asset.optLong("size"),
				apkUrl = asset.getString("browser_download_url"),
				description = json.optString("body"),
			)
		}
	}

	suspend fun fetchUpdate(): AppVersion? = withContext(Dispatchers.Default) {
		if (!isUpdateSupported()) {
			return@withContext null
		}
		runCatchingCancellable {
			val currentVersion = VersionId(BuildConfig.VERSION_NAME)
			val available = getAvailableVersions().asArrayList()
			available.sortBy { it.versionId }
			if (currentVersion.isStable && !settings.isUnstableUpdatesAllowed) {
				available.retainAll { it.versionId.isStable }
			}
			available.maxByOrNull { it.versionId }
				?.takeIf { it.versionId > currentVersion }
		}.onFailure {
			it.printStackTraceDebug()
		}.onSuccess {
			availableUpdate.value = it
		}.getOrNull()
	}

	@Suppress("KotlinConstantConditions")
	suspend fun isUpdateSupported(): Boolean {
		return BuildConfig.BUILD_TYPE != BUILD_TYPE_RELEASE || appValidator.isReleaseSignatureTrusted.getOrNull() == true
	}

	private fun JSONObject.isApkAsset(): Boolean {
		val name = optString("name")
		val url = optString("browser_download_url")
		return optString("content_type") == CONTENT_TYPE_APK ||
			name.endsWith(".apk", ignoreCase = true) ||
			url.substringBefore('?').endsWith(".apk", ignoreCase = true)
	}

	private fun JSONObject.apkAssetScore(): Int {
		if (!isApkAsset()) {
			return -1
		}
		val name = optString("name").lowercase(Locale.ROOT)
		var score = 0
		if (optString("content_type") == CONTENT_TYPE_APK) {
			score += 8
		}
		if ("universal" in name) {
			score += 4
		}
		if ("release" in name) {
			score += 2
		}
		if ("debug" in name) {
			score -= 16
		}
		return score
	}

	private fun JSONArray.findBestApkAsset(): JSONObject? {
		var best: JSONObject? = null
		var bestScore = -1
		for (i in 0 until length()) {
			val asset = getJSONObject(i)
			val score = asset.apkAssetScore()
			if (score > bestScore) {
				best = asset
				bestScore = score
			}
		}
		return best
	}
}
