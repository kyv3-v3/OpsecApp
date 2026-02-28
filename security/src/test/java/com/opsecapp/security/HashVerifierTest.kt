package com.opsecapp.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class HashVerifierTest {
  @Test
  fun verifies_sha256_file_hash() {
    val temp = File.createTempFile("hash", ".txt")
    temp.writeText("hello")

    val expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
    assertThat(HashVerifier.verifySha256(temp, expected)).isTrue()
  }
}
