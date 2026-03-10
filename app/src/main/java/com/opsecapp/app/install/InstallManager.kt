package com.opsecapp.app.install

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.opsecapp.app.BuildConfig
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
import java.security.MessageDigest

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
    val packageId = item.packageId ?: extractPackageIdFromFdroidUrl(fdroidLink?.url)
    val uri = when {
      !packageId.isNullOrBlank() -> Uri.parse("fdroid.app://details?id=$packageId")
      fdroidLink != null -> Uri.parse(fdroidLink.url)
      else -> return InstallResult.Error("No F-Droid package reference is available for this item.")
    }

    val intent = Intent(Intent.ACTION_VIEW, uri)
      .setPackage(FDROID_PACKAGE)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
    link: UpstreamLink,
    expectedPackageName: String? = null
  ): InstallResult = withContext(Dispatchers.IO) {
    val parsed = runCatching { Uri.parse(link.url) }.getOrNull()
      ?: return@withContext InstallResult.Error("Invalid install URL")

    if (parsed.scheme != "https") {
      return@withContext InstallResult.Error("Blocked non-HTTPS install URL.")
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

    val apkPackageInfo = runCatching {
      readPackageInfo(apkFile)
    }.getOrNull()

    if (apkPackageInfo == null) {
      apkFile.delete()
      return@withContext InstallResult.Error("Downloaded file is not a valid APK package.")
    }

    val apkPackageName = apkPackageInfo.packageName
    if (apkPackageName.isNullOrBlank()) {
      apkFile.delete()
      return@withContext InstallResult.Error("Downloaded APK package name is missing.")
    }

    if (!expectedPackageName.isNullOrBlank() && apkPackageName != expectedPackageName) {
      apkFile.delete()
      return@withContext InstallResult.Error(
        "Downloaded APK package '$apkPackageName' does not match expected package '$expectedPackageName'."
      )
    }

    if (!expectedPackageName.isNullOrBlank()
      && expectedPackageName == context.packageName
      && !isSignedWithCurrentPackage(apkPackageInfo)
    ) {
      apkFile.delete()
      return@withContext InstallResult.Error(
        "Downloaded APK is signed with a different certificate than the currently installed app."
      )
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
    } catch (_: SecurityException) {
      apkFile.delete()
      InstallResult.Error("Installer permissions are missing on this device.")
    } catch (_: ActivityNotFoundException) {
      apkFile.delete()
      InstallResult.Error("No package installer available on this device.")
    } catch (_: Exception) {
      apkFile.delete()
      InstallResult.Error("Unable to launch package installer.")
    }
  }

  fun openUpstreamUrl(url: String, message: String): InstallResult {
    val parsed = runCatching { Uri.parse(url) }.getOrNull()
      ?: return InstallResult.Error("Invalid upstream URL.")

    if (parsed.scheme != "https") {
      return InstallResult.Error("Blocked non-HTTPS upstream URL.")
    }

    val intent = Intent(Intent.ACTION_VIEW, parsed).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return InstallResult.NeedsUserAction(message = message, actionIntent = intent)
  }

  private fun downloadFile(url: String, destination: File) {
    val request = Request.Builder()
      .url(url)
      .get()
      .header("User-Agent", "OpsecApp/${BuildConfig.VERSION_NAME}")
      .build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw IOException("HTTP ${response.code}")
      }
      val body = response.body ?: throw IOException("Empty response body")
      destination.outputStream().use { output ->
        val bytesCopied = body.byteStream().copyTo(output)
        if (bytesCopied <= 0L) {
          throw IOException("Downloaded APK is empty.")
        }
      }
    }
  }

  private fun extractPackageIdFromFdroidUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val match = FDROID_PACKAGE_REGEX.find(url) ?: return null
    return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
  }

  private fun readPackageInfo(apkFile: File): PackageInfo? {
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      PackageManager.GET_SIGNING_CERTIFICATES
    } else {
      PackageManager.GET_SIGNATURES
    }
    return context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
  }

  private fun isSignedWithCurrentPackage(candidatePackageInfo: PackageInfo): Boolean {
    val installedPackageInfo = getInstalledPackageInfo() ?: return false
    val installedSignatures = signaturesForPackage(installedPackageInfo)
    if (installedSignatures.isEmpty()) return false

    val candidateSignatures = signaturesForPackage(candidatePackageInfo)
    if (candidateSignatures.isEmpty()) return false

    return installedSignatures.intersect(candidateSignatures).isNotEmpty()
  }

  private fun getInstalledPackageInfo(): PackageInfo? {
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      PackageManager.GET_SIGNING_CERTIFICATES
    } else {
      PackageManager.GET_SIGNATURES
    }
    return runCatching {
      context.packageManager.getPackageInfo(context.packageName, flags)
    }.getOrNull()
  }

  private fun signaturesForPackage(packageInfo: PackageInfo): Set<String> {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      packageInfo.signingInfo?.apkContentsSigners
    } else {
      packageInfo.signatures
    } ?: return emptySet()

    return signatures.map { signature ->
      MessageDigest.getInstance("SHA-256")
        .digest(signature.toByteArray())
        .joinToString(separator = "") { b -> "%02x".format(b.toInt() and 0xFF) }
    }.toSet()
  }

  companion object {
    private const val FDROID_PACKAGE = "org.fdroid.fdroid"
    private val FDROID_PACKAGE_REGEX =
      Regex("""https://f-droid\.org/(?:[a-z]{2}/)?packages/([^/]+)/?""", RegexOption.IGNORE_CASE)
  }
}
