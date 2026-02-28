package com.opsecapp.domain.model

import java.time.Instant

data class CatalogCategory(
  val id: String,
  val titleExact: String,
  val descriptionExact: String,
  val path: String,
  val sortOrder: Int
)

enum class UpstreamLinkType {
  FDROID,
  GITHUB,
  OFFICIAL,
  OTHER
}

data class UpstreamLink(
  val type: UpstreamLinkType,
  val url: String,
  val labelExact: String,
  val expectedSha256: String? = null,
  val expectedSignerSha256: String? = null
)

enum class SourceConfidence {
  HIGH,
  MEDIUM,
  LOW
}

enum class InstallType {
  FDROID,
  GITHUB_APK,
  OFFICIAL_APK,
  OFFICIAL_SITE,
  WEB_SERVICE,
  UNKNOWN
}

data class CatalogItem(
  val id: String,
  val titleExact: String,
  val descriptionExact: String,
  val badgeExact: String,
  val sectionExact: String,
  val categoryIds: List<String>,
  val upstreamLinks: List<UpstreamLink>,
  val packageId: String? = null,
  val githubRepo: String? = null,
  val sourceConfidence: SourceConfidence,
  val installType: InstallType
)

data class CatalogMeta(
  val schemaVersion: String,
  val generatedAt: Instant,
  val lastSyncedFromSourceAt: Instant,
  val sourceUrl: String,
  val sourceLanguage: String,
  val extractor: String,
  val signed: Boolean,
  val trustStatus: TrustStatus
)

enum class TrustStatus {
  TRUSTED,
  UNTRUSTED,
  INVALID_SIGNATURE,
  INVALID_FINGERPRINT,
  NETWORK_ERROR,
  PARSE_ERROR
}

data class HomeState(
  val categories: List<CatalogCategory>,
  val highlights: List<CatalogItem>,
  val lastSyncedText: String,
  val trustStatus: TrustStatus
)
