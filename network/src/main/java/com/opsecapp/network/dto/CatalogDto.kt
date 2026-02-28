package com.opsecapp.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CatalogDto(
  val schemaVersion: String,
  val generatedAt: String,
  val lastSyncedFromSourceAt: String,
  val sourceUrl: String,
  val sourceLanguage: String,
  val extractor: String,
  val categories: List<CategoryDto>,
  val items: List<ItemDto>
)

@Serializable
data class CategoryDto(
  val id: String,
  val titleExact: String,
  val descriptionExact: String,
  val path: String,
  val sortOrder: Int
)

@Serializable
data class ItemDto(
  val id: String,
  val titleExact: String,
  val descriptionExact: String,
  val badgeExact: String,
  val sectionExact: String,
  val categoryIds: List<String>,
  val upstreamLinks: List<UpstreamLinkDto>,
  val packageId: String? = null,
  val githubRepo: String? = null,
  val sourceConfidence: String,
  val installType: String
)

@Serializable
data class UpstreamLinkDto(
  val type: String,
  val url: String,
  val labelExact: String,
  val expectedSha256: String? = null,
  val expectedSignerSha256: String? = null
)

@Serializable
data class ApkMetadataDto(
  @SerialName("sha256")
  val sha256: String? = null,
  @SerialName("signerSha256")
  val signerSha256: String? = null
)
