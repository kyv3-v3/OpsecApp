package com.opsecapp.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AppReleaseInfo(
  val tagName: String,
  val htmlUrl: String,
  val publishedAt: String?,
  val notes: String?,
  val apkDownloadUrl: String?
)

sealed interface AppUpdateCheckResult {
  data class UpdateAvailable(
    val currentVersion: String,
    val release: AppReleaseInfo
  ) : AppUpdateCheckResult

  data class UpToDate(
    val currentVersion: String,
    val latestVersion: String
  ) : AppUpdateCheckResult

  data class Error(val message: String) : AppUpdateCheckResult
}

class AppUpdateChecker(
  private val latestReleaseApiUrl: String,
  private val currentVersionName: String,
  private val fallbackReleasesPageUrl: String,
  private val client: OkHttpClient = OkHttpClient.Builder()
    .callTimeout(20, TimeUnit.SECONDS)
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build(),
  private val json: Json = Json { ignoreUnknownKeys = true }
) {
  suspend fun checkLatestRelease(): AppUpdateCheckResult = withContext(Dispatchers.IO) {
    if (!latestReleaseApiUrl.startsWith("https://")) {
      return@withContext AppUpdateCheckResult.Error("Release API URL must use HTTPS.")
    }

    val request = Request.Builder()
      .url(latestReleaseApiUrl)
      .get()
      .header("Accept", "application/vnd.github+json")
      .header("User-Agent", "OpsecApp/$currentVersionName")
      .header("X-GitHub-Api-Version", "2022-11-28")
      .build()

    val release = try {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          return@withContext AppUpdateCheckResult.Error("Release check failed (HTTP ${response.code}).")
        }
        val body = response.body?.string().orEmpty()
        json.decodeFromString<GitHubReleaseDto>(body)
      }
    } catch (ioe: IOException) {
      return@withContext AppUpdateCheckResult.Error("Release check failed: ${ioe.message ?: "network error"}")
    } catch (_: Exception) {
      return@withContext AppUpdateCheckResult.Error("Unable to parse release metadata from GitHub.")
    }

    val latest = normalizeVersion(release.tagName)
    val current = normalizeVersion(currentVersionName)
    val apkAsset = release.assets.firstOrNull {
      it.browserDownloadUrl.endsWith(".apk", ignoreCase = true) ||
        it.contentType?.contains("android.package-archive", ignoreCase = true) == true
    }

    val releaseInfo = AppReleaseInfo(
      tagName = release.tagName,
      htmlUrl = release.htmlUrl.ifBlank { fallbackReleasesPageUrl },
      publishedAt = release.publishedAt,
      notes = release.body,
      apkDownloadUrl = apkAsset?.browserDownloadUrl
    )

    if (isVersionNewer(latest, current)) {
      return@withContext AppUpdateCheckResult.UpdateAvailable(
        currentVersion = currentVersionName,
        release = releaseInfo
      )
    }

    AppUpdateCheckResult.UpToDate(
      currentVersion = currentVersionName,
      latestVersion = release.tagName
    )
  }

  private fun normalizeVersion(raw: String): List<Int> {
    return VERSION_REGEX.findAll(raw.removePrefix("v").removePrefix("V"))
      .mapNotNull { match -> match.value.toIntOrNull() }
      .toList()
      .ifEmpty { listOf(0) }
  }

  private fun isVersionNewer(latest: List<Int>, current: List<Int>): Boolean {
    val max = maxOf(latest.size, current.size)
    for (index in 0 until max) {
      val latestPart = latest.getOrElse(index) { 0 }
      val currentPart = current.getOrElse(index) { 0 }
      if (latestPart != currentPart) {
        return latestPart > currentPart
      }
    }
    return false
  }

  @Serializable
  private data class GitHubReleaseDto(
    @SerialName("tag_name")
    val tagName: String,
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("published_at")
    val publishedAt: String? = null,
    val body: String? = null,
    val assets: List<GitHubAssetDto> = emptyList()
  )

  @Serializable
  private data class GitHubAssetDto(
    val name: String? = null,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    @SerialName("content_type")
    val contentType: String? = null
  )

  private companion object {
    val VERSION_REGEX = Regex("\\d+")
  }
}
