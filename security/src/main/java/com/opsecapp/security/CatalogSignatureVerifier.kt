package com.opsecapp.security

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

enum class CatalogTrustFailure {
  INVALID_PUBLIC_KEY_FINGERPRINT,
  INVALID_SIGNATURE,
  INVALID_SIGNATURE_ENCODING
}

data class CatalogTrustResult(
  val trusted: Boolean,
  val failure: CatalogTrustFailure? = null
)

class CatalogSignatureVerifier(
  private val pinnedPublicKeyPem: String,
  private val pinnedFingerprintSha256: String
) {
  private val publicKey: PublicKey by lazy {
    val keyBytes = decodePem(pinnedPublicKeyPem)
    val keySpec = X509EncodedKeySpec(keyBytes)
    KeyFactory.getInstance("Ed25519").generatePublic(keySpec)
  }

  fun verify(catalogBytes: ByteArray, signatureBase64: String): CatalogTrustResult {
    val keyBytes = decodePem(pinnedPublicKeyPem)
    val fingerprint = sha256Hex(keyBytes)
    if (!fingerprint.equals(pinnedFingerprintSha256, ignoreCase = true)) {
      return CatalogTrustResult(
        trusted = false,
        failure = CatalogTrustFailure.INVALID_PUBLIC_KEY_FINGERPRINT
      )
    }

    val signatureBytes = try {
      Base64.getDecoder().decode(signatureBase64)
    } catch (_: IllegalArgumentException) {
      return CatalogTrustResult(
        trusted = false,
        failure = CatalogTrustFailure.INVALID_SIGNATURE_ENCODING
      )
    }

    val verifier = Signature.getInstance("Ed25519")
    verifier.initVerify(publicKey)
    verifier.update(catalogBytes)

    return if (verifier.verify(signatureBytes)) {
      CatalogTrustResult(trusted = true)
    } else {
      CatalogTrustResult(trusted = false, failure = CatalogTrustFailure.INVALID_SIGNATURE)
    }
  }

  private fun decodePem(publicKeyPem: String): ByteArray {
    val base64 = publicKeyPem
      .replace("-----BEGIN PUBLIC KEY-----", "")
      .replace("-----END PUBLIC KEY-----", "")
      .replace("\n", "")
      .trim()

    return Base64.getDecoder().decode(base64)
  }

  private fun sha256Hex(input: ByteArray): String {
    return MessageDigest
      .getInstance("SHA-256")
      .digest(input)
      .joinToString(separator = "") { b -> "%02x".format(b) }
  }
}
