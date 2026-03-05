package com.opsecapp.data.repository

import com.google.common.truth.Truth.assertThat
import com.opsecapp.data.local.dao.CatalogDao
import com.opsecapp.data.local.entity.CatalogMetaEntity
import com.opsecapp.domain.model.TrustStatus
import com.opsecapp.domain.repository.SettingsRepository
import com.opsecapp.network.source.CatalogRemoteDataSource
import com.opsecapp.network.source.RemoteCatalogPayload
import com.opsecapp.security.CatalogSignatureVerifier
import com.opsecapp.security.CatalogTrustFailure
import com.opsecapp.security.CatalogTrustResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException

class CatalogRepositoryImplTest {

  @Test
  fun syncCatalog_persists_invalid_signature_status_in_meta() = runBlocking {
    val dao = mockk<CatalogDao>()
    val remoteDataSource = mockk<CatalogRemoteDataSource>()
    val signatureVerifier = mockk<CatalogSignatureVerifier>()
    val settingsRepository = mockk<SettingsRepository>()

    every { settingsRepository.observeCatalogBaseUrl() } returns flowOf(BASE_URL)
    coEvery { remoteDataSource.fetch(BASE_URL) } returns RemoteCatalogPayload(
      catalogBytes = """{"schemaVersion":"1.0.0"}""".toByteArray(),
      signatureBase64 = "invalid",
      sourceCatalogUrl = "$BASE_URL/catalog.json",
      sourceSignatureUrl = "$BASE_URL/catalog.sig"
    )
    every {
      signatureVerifier.verify(any(), any())
    } returns CatalogTrustResult(
      trusted = false,
      failure = CatalogTrustFailure.INVALID_SIGNATURE
    )
    every { dao.observeMeta() } returns flowOf(existingMeta())
    coEvery { dao.insertMeta(any()) } returns Unit

    val repository = CatalogRepositoryImpl(
      dao = dao,
      remoteDataSource = remoteDataSource,
      signatureVerifier = signatureVerifier,
      settingsRepository = settingsRepository
    )

    val status = repository.syncCatalog(force = true)

    assertThat(status).isEqualTo(TrustStatus.INVALID_SIGNATURE)
    coVerify(exactly = 1) {
      dao.insertMeta(match { meta ->
        meta.trustStatus == TrustStatus.INVALID_SIGNATURE.name &&
          !meta.signed
      })
    }
    coVerify(exactly = 0) {
      dao.replaceAll(any(), any(), any(), any(), any(), any())
    }
  }

  @Test
  fun syncCatalog_persists_parse_error_status_in_meta() = runBlocking {
    val dao = mockk<CatalogDao>()
    val remoteDataSource = mockk<CatalogRemoteDataSource>()
    val signatureVerifier = mockk<CatalogSignatureVerifier>()
    val settingsRepository = mockk<SettingsRepository>()

    every { settingsRepository.observeCatalogBaseUrl() } returns flowOf(BASE_URL)
    coEvery { remoteDataSource.fetch(BASE_URL) } returns RemoteCatalogPayload(
      catalogBytes = "not-json".toByteArray(),
      signatureBase64 = "any",
      sourceCatalogUrl = "$BASE_URL/catalog.json",
      sourceSignatureUrl = "$BASE_URL/catalog.sig"
    )
    every { signatureVerifier.verify(any(), any()) } returns CatalogTrustResult(trusted = true)
    every { dao.observeMeta() } returns flowOf(existingMeta())
    coEvery { dao.insertMeta(any()) } returns Unit

    val repository = CatalogRepositoryImpl(
      dao = dao,
      remoteDataSource = remoteDataSource,
      signatureVerifier = signatureVerifier,
      settingsRepository = settingsRepository
    )

    val status = repository.syncCatalog(force = true)

    assertThat(status).isEqualTo(TrustStatus.PARSE_ERROR)
    coVerify(exactly = 1) {
      dao.insertMeta(match { meta ->
        meta.trustStatus == TrustStatus.PARSE_ERROR.name &&
          !meta.signed
      })
    }
    coVerify(exactly = 0) {
      dao.replaceAll(any(), any(), any(), any(), any(), any())
    }
  }

  @Test
  fun syncCatalog_creates_meta_row_on_network_error_when_missing() = runBlocking {
    val dao = mockk<CatalogDao>()
    val remoteDataSource = mockk<CatalogRemoteDataSource>()
    val signatureVerifier = mockk<CatalogSignatureVerifier>()
    val settingsRepository = mockk<SettingsRepository>()

    every { settingsRepository.observeCatalogBaseUrl() } returns flowOf(BASE_URL)
    coEvery { remoteDataSource.fetch(BASE_URL) } throws IOException("boom")
    every { dao.observeMeta() } returns flowOf(null)
    coEvery { dao.insertMeta(any()) } returns Unit

    val repository = CatalogRepositoryImpl(
      dao = dao,
      remoteDataSource = remoteDataSource,
      signatureVerifier = signatureVerifier,
      settingsRepository = settingsRepository
    )

    val status = repository.syncCatalog(force = true)

    assertThat(status).isEqualTo(TrustStatus.NETWORK_ERROR)
    coVerify(exactly = 1) {
      dao.insertMeta(match { meta ->
        meta.trustStatus == TrustStatus.NETWORK_ERROR.name &&
          meta.sourceUrl == BASE_URL &&
          !meta.signed
      })
    }
  }

  private fun existingMeta(): CatalogMetaEntity {
    return CatalogMetaEntity(
      schemaVersion = "1.0.0",
      generatedAt = "2026-03-05T00:00:00Z",
      lastSyncedFromSourceAt = "2026-03-05T00:00:00Z",
      sourceUrl = BASE_URL,
      sourceLanguage = "en",
      extractor = "extractor",
      signed = true,
      trustStatus = TrustStatus.TRUSTED.name
    )
  }

  companion object {
    private const val BASE_URL = "https://catalog.example"
  }
}
