package com.opsecapp.app.install

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.model.UpstreamLink
import com.opsecapp.domain.model.UpstreamLinkType
import com.opsecapp.security.ApkSignerVerifier
import com.opsecapp.security.HashVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

sealed interface InstallResult {
  data object FdroidLaunched : InstallResult
  data object FdroidMissing : InstallResult
  data object InstallerLaunched : InstallResult
  data class NeedsUserAction(val message: String, val actionIntent: Intent? = null) : InstallResult
  data class Warning(val message: String) : InstallResult
  data class Error(val message: String) : InstallResult
}

class InstallManager(
  private val context: Context,
  private val client: OkHttpClient = OkHttpClient()
) {
  fun isFdroidInstalled(): Boolean {
    return runCatching {
      context.packageManager.getPackageInfo(FDROID_PACKAGE, 0)
      true
    }.getOrDefault(false)
  }

  fun launchFdroid(item: CatalogItem): InstallResult {
    if (!isFdroidInstalled()) {
      return InstallResult.FdroidMissing
    }

    val fdroidLink = item.upstreamLinks.firstOrNull { it.type == UpstreamLinkType.FDROID }
    val packageId = item.packageId
    val uri = when {
      fdroidLink != null -> Uri.parse(fdroidLink.url)
      !packageId.isNullOrBlank() -> Uri.parse("fdroid.app://details?id=$packageId")
      else -> return InstallResult.Error("No F-Droid package reference is available for this item.")
    }

    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
      context.startActivity(intent)
      InstallResult.FdroidLaunched
    } catch (_: ActivityNotFoundException) {
      InstallResult.Error("Unable to launch F-Droid app.")
    }
  }

  fun promptInstallFdroidIntent(): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org"))
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }

  suspend fun downloadVerifyAndInstall(
    link: UpstreamLink
  ): InstallResult = withContext(Dispatchers.IO) {
    val parsed = runCatching { Uri.parse(link.url) }.getOrNull()
      ?: return@withContext InstallResult.Error("Invalid install URL")

    if (parsed.scheme != "https") {
      return@withContext InstallResult.Error("Blocked non-HTTPS install URL.")
    }

    if (link.type == UpstreamLinkType.OTHER) {
      return@withContext InstallResult.Error("Blocked untrusted mirror URL.")
    }

    val apkFile = File(context.cacheDir.resolve("apks"), "download.apk").apply {
      parentFile?.mkdirs()
      if (exists()) delete()
    }

    try {
      downloadFile(link.url, apkFile)
    } catch (error: IOException) {
      return@withContext InstallResult.Error("Download failed: ${error.message}")
    }

    link.expectedSha256?.let { expected ->
      if (!HashVerifier.verifySha256(apkFile, expected)) {
        apkFile.delete()
        return@withContext InstallResult.Error("APK hash verification failed.")
      }
    }

    link.expectedSignerSha256?.let { expectedSigner ->
      if (!ApkSignerVerifier.verifySignerSha256(context, apkFile, expectedSigner)) {
        apkFile.delete()
        return@withContext InstallResult.Error("APK signer fingerprint mismatch.")
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
      val settingsIntent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}")
      ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

      return@withContext InstallResult.NeedsUserAction(
        message = "Allow this app to request package installs, then retry.",
        actionIntent = settingsIntent
      )
    }

    val packageUri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      apkFile
    )

    val installIntent = Intent(Intent.ACTION_VIEW).apply {
      data = packageUri
      flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
      setDataAndType(packageUri, "application/vnd.android.package-archive")
    }

    return@withContext try {
      context.startActivity(installIntent)
      if (link.expectedSha256 == null && link.expectedSignerSha256 == null) {
        InstallResult.Warning(
          "No expected hash/certificate metadata available for this APK. Verify source before continuing."
        )
      } else {
        InstallResult.InstallerLaunched
      }
    } catch (_: ActivityNotFoundException) {
      InstallResult.Error("No package installer available on this device.")
    }
  }

  private fun downloadFile(url: String, destination: File) {
    val request = Request.Builder().url(url).get().build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw IOException("HTTP ${response.code}")
      }
      val body = response.body ?: throw IOException("Empty response body")
      destination.outputStream().use { output ->
        body.byteStream().copyTo(output)
      }
    }
  }

  companion object {
    private const val FDROID_PACKAGE = "org.fdroid.fdroid"
  }
}
