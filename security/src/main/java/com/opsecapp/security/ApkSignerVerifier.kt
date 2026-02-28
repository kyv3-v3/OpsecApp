package com.opsecapp.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

object ApkSignerVerifier {
  fun verifySignerSha256(
    context: Context,
    apkFile: File,
    expectedSignerSha256: String
  ): Boolean {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      context.packageManager.getPackageArchiveInfo(
        apkFile.absolutePath,
        PackageManager.GET_SIGNING_CERTIFICATES
      )
    } else {
      @Suppress("DEPRECATION")
      context.packageManager.getPackageArchiveInfo(
        apkFile.absolutePath,
        PackageManager.GET_SIGNATURES
      )
    } ?: return false

    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      packageInfo.signingInfo?.apkContentsSigners?.toList().orEmpty()
    } else {
      @Suppress("DEPRECATION")
      packageInfo.signatures?.toList().orEmpty()
    }

    if (signatures.isEmpty()) return false

    val expected = expectedSignerSha256.lowercase()
    return signatures.any { signature ->
      val digest = MessageDigest
        .getInstance("SHA-256")
        .digest(signature.toByteArray())
        .joinToString(separator = "") { "%02x".format(it) }
      digest == expected
    }
  }
}
