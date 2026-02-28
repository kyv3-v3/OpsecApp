package com.opsecapp.security

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object HashVerifier {
  fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { input ->
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
      var read: Int
      while (input.read(buffer).also { read = it } != -1) {
        digest.update(buffer, 0, read)
      }
    }

    return digest.digest().joinToString(separator = "") { "%02x".format(it) }
  }

  fun verifySha256(file: File, expectedHex: String): Boolean {
    return sha256(file).equals(expectedHex, ignoreCase = true)
  }
}
