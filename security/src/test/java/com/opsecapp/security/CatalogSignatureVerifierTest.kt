package com.opsecapp.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.util.Base64

class CatalogSignatureVerifierTest {

  @Test
  fun verifies_valid_signature_and_rejects_tampered_payload() {
    val keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    val publicKeyPem = toPublicPem(keyPair.public.encoded)
    val fingerprint = sha256Hex(keyPair.public.encoded)

    val verifier = CatalogSignatureVerifier(
      pinnedPublicKeyPem = publicKeyPem,
      pinnedFingerprintSha256 = fingerprint
    )

    val payload = "{\"schemaVersion\":\"1.0.0\"}\n".toByteArray()
    val signature = sign(payload, keyPair.private)

    val trusted = verifier.verify(payload, signature)
    assertThat(trusted.trusted).isTrue()

    val tampered = "{\"schemaVersion\":\"2.0.0\"}\n".toByteArray()
    val rejected = verifier.verify(tampered, signature)
    assertThat(rejected.trusted).isFalse()
    assertThat(rejected.failure).isEqualTo(CatalogTrustFailure.INVALID_SIGNATURE)
  }

  private fun sign(payload: ByteArray, privateKey: PrivateKey): String {
    val signer = Signature.getInstance("Ed25519")
    signer.initSign(privateKey)
    signer.update(payload)

    return Base64.getEncoder().encodeToString(signer.sign())
  }

  private fun toPublicPem(encoded: ByteArray): String {
    val base64 = Base64.getEncoder().encodeToString(encoded)
    return "-----BEGIN PUBLIC KEY-----\n$base64\n-----END PUBLIC KEY-----"
  }

  private fun sha256Hex(input: ByteArray): String {
    return MessageDigest
      .getInstance("SHA-256")
      .digest(input)
      .joinToString(separator = "") { byte -> "%02x".format(byte) }
  }
}
