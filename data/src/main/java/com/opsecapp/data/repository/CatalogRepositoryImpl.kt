package com.opsecapp.data.repository

import com.opsecapp.data.local.dao.CatalogDao
import com.opsecapp.data.local.entity.CatalogMetaEntity
import com.opsecapp.data.mapper.toDomain
import com.opsecapp.data.mapper.toPersistable
import com.opsecapp.domain.model.CatalogCategory
import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.model.CatalogMeta
import com.opsecapp.domain.model.InstallType
import com.opsecapp.domain.model.SourceConfidence
import com.opsecapp.domain.model.TrustStatus
import com.opsecapp.domain.repository.CatalogRepository
import com.opsecapp.domain.repository.SettingsRepository
import com.opsecapp.network.dto.CatalogDto
import com.opsecapp.network.source.CatalogRemoteDataSource
import com.opsecapp.security.CatalogSignatureVerifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Instant

class CatalogRepositoryImpl(
  private val dao: CatalogDao,
  private val remoteDataSource: CatalogRemoteDataSource,
  private val signatureVerifier: CatalogSignatureVerifier,
  private val settingsRepository: SettingsRepository,
  private val json: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
  }
) : CatalogRepository {

  override fun observeCategories(): Flow<List<CatalogCategory>> {
    return dao.observeCategories().map { categories ->
      categories.map { it.toDomain() }
    }
  }

  override fun observeHighlights(limit: Int): Flow<List<CatalogItem>> {
    return dao.observeHighlights(limit).map { rows ->
      rows.map { it.toDomain() }
    }
  }

  override fun observeCategoryItems(categoryId: String): Flow<List<CatalogItem>> {
    return dao.observeCategoryItems(categoryId).map { rows ->
      rows.map { it.toDomain().copy(categoryIds = listOf(categoryId)) }
    }
  }

  override fun observeItem(itemId: String): Flow<CatalogItem?> {
    return dao.observeItem(itemId).map { row -> row?.toDomain() }
  }

  override fun search(
    query: String,
    confidence: SourceConfidence?,
    installType: InstallType?
  ): Flow<List<CatalogItem>> {
    val normalized = query
      .trim()
      .replace(Regex("[^\\p{L}\\p{N}_]+"), " ")
      .trim()

    val flow = if (normalized.isBlank()) {
      dao.filterItems(
        sourceConfidence = confidence?.name,
        installType = installType?.name
      )
    } else {
      val tokenized = normalized
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(separator = " ") { "$it*" }

      dao.search(
        ftsQuery = tokenized,
        sourceConfidence = confidence?.name,
        installType = installType?.name
      )
    }

    return flow.map { rows -> rows.map { it.toDomain() } }
  }

  override fun observeMeta(): Flow<CatalogMeta?> {
    return dao.observeMeta().map { it?.toDomain() }
  }

  override suspend fun syncCatalog(force: Boolean): TrustStatus {
    val baseUrl = settingsRepository.observeCatalogBaseUrl().first()

    return try {
      val payload = remoteDataSource.fetch(baseUrl)

      val trust = signatureVerifier.verify(
        catalogBytes = payload.catalogBytes,
        signatureBase64 = payload.signatureBase64
      )

      if (!trust.trusted) {
        val trustStatus = when (trust.failure) {
          com.opsecapp.security.CatalogTrustFailure.INVALID_PUBLIC_KEY_FINGERPRINT -> TrustStatus.INVALID_FINGERPRINT
          com.opsecapp.security.CatalogTrustFailure.INVALID_SIGNATURE,
          com.opsecapp.security.CatalogTrustFailure.INVALID_SIGNATURE_ENCODING -> TrustStatus.INVALID_SIGNATURE
          null -> TrustStatus.UNTRUSTED
        }
        persistTrustStatus(trustStatus, baseUrl)
        return trustStatus
      }

      val dto = json.decodeFromString<CatalogDto>(payload.catalogBytes.decodeToString())
      val persistable = dto.toPersistable(TrustStatus.TRUSTED)

      dao.replaceAll(
        categories = persistable.categories,
        items = persistable.items,
        crossRefs = persistable.itemCategoryCrossRefs,
        links = persistable.links,
        ftsRows = persistable.ftsRows,
        meta = persistable.meta
      )

      TrustStatus.TRUSTED
    } catch (error: Throwable) {
      val status = when (error) {
        is SerializationException -> TrustStatus.PARSE_ERROR
        else -> TrustStatus.NETWORK_ERROR
      }
      persistTrustStatus(status, baseUrl)
      status
    }
  }

  private suspend fun persistTrustStatus(status: TrustStatus, sourceUrl: String) {
    val existingMeta = dao.observeMeta().first()
    if (existingMeta != null) {
      dao.insertMeta(
        existingMeta.copy(
          trustStatus = status.name,
          signed = status == TrustStatus.TRUSTED
        )
      )
      return
    }

    val now = Instant.now().toString()
    dao.insertMeta(
      CatalogMetaEntity(
        schemaVersion = "unknown",
        generatedAt = now,
        lastSyncedFromSourceAt = now,
        sourceUrl = sourceUrl,
        sourceLanguage = "unknown",
        extractor = "unknown",
        signed = false,
        trustStatus = status.name
      )
    )
  }
}
