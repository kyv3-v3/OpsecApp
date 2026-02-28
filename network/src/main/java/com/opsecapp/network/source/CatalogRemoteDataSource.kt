package com.opsecapp.network.source

import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

private const val CATALOG_JSON = "catalog.json"
private const val CATALOG_SIG = "catalog.sig"

data class RemoteCatalogPayload(
  val catalogBytes: ByteArray,
  val signatureBase64: String,
  val sourceCatalogUrl: String,
  val sourceSignatureUrl: String
)

class CatalogRemoteDataSource(
  private val client: OkHttpClient
) {
  suspend fun fetch(baseUrl: String): RemoteCatalogPayload = withContext(Dispatchers.IO) {
    val catalogUrl = normalize(baseUrl, CATALOG_JSON)
    val signatureUrl = normalize(baseUrl, CATALOG_SIG)

    val catalogBytes = withRetries { getBytes(catalogUrl) }
    val signature = withRetries { getString(signatureUrl).trim() }

    RemoteCatalogPayload(
      catalogBytes = catalogBytes,
      signatureBase64 = signature,
      sourceCatalogUrl = catalogUrl,
      sourceSignatureUrl = signatureUrl
    )
  }

  private fun normalize(baseUrl: String, filename: String): String {
    return if (baseUrl.endsWith("/")) "$baseUrl$filename" else "$baseUrl/$filename"
  }

  private suspend fun <T> withRetries(block: suspend () -> T): T {
    var lastError: Throwable? = null
    repeat(3) { attempt ->
      try {
        return block()
      } catch (error: Throwable) {
        lastError = error
        if (attempt < 2) {
          delay((attempt + 1) * 1000L)
        }
      }
    }
    throw IOException("Network fetch failed after retries", lastError)
  }

  private fun getBytes(url: String): ByteArray {
    val request = Request.Builder().url(url).get().build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw IOException("HTTP ${response.code} for $url")
      }
      return response.body?.bytes() ?: throw IOException("Empty body for $url")
    }
  }

  private fun getString(url: String): String {
    val request = Request.Builder().url(url).get().build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw IOException("HTTP ${response.code} for $url")
      }
      return response.body?.string() ?: throw IOException("Empty body for $url")
    }
  }
}
