package com.opsecapp.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

class CatalogSignatureVerifierTest {

  private val publicKeyPem = """
    -----BEGIN PUBLIC KEY-----
    MCowBQYDK2VwAyEAIa3HLalRyhxAxmYitD8cha8J7LxhmSQaGH2bgXlMvZw=
    -----END PUBLIC KEY-----
  """.trimIndent()

  private val privateKeyPem = """
    -----BEGIN PRIVATE KEY-----
    MC4CAQAwBQYDK2VwBCIEIG5vI23N8c95gvuGw8Zv1K1EZcsPuPaIYiQAI1Q/f40L
    -----END PRIVATE KEY-----
  """.trimIndent()

  @Test
  fun verifies_valid_signature_and_rejects_tampered_payload() {
    val verifier = CatalogSignatureVerifier(
      pinnedPublicKeyPem = publicKeyPem,
      pinnedFingerprintSha256 = "eca188f20889dbd9466fdabdc14a5836fb9cfe3b9328842dcffb1d299d9a2099"
    )

    val payload = "{\"schemaVersion\":\"1.0.0\"}\n".toByteArray()
    val signature = sign(payload)

    val trusted = verifier.verify(payload, signature)
    assertThat(trusted.trusted).isTrue()

    val tampered = "{\"schemaVersion\":\"2.0.0\"}\n".toByteArray()
    val rejected = verifier.verify(tampered, signature)
    assertThat(rejected.trusted).isFalse()
    assertThat(rejected.failure).isEqualTo(CatalogTrustFailure.INVALID_SIGNATURE)
  }

  private fun sign(payload: ByteArray): String {
    val privateBytes = Base64.getDecoder().decode(
      privateKeyPem
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("\n", "")
        .trim()
    )

    val keyFactory = KeyFactory.getInstance("Ed25519")
    val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateBytes))

    val signer = Signature.getInstance("Ed25519")
    signer.initSign(privateKey)
    signer.update(payload)

    return Base64.getEncoder().encodeToString(signer.sign())
  }
}
