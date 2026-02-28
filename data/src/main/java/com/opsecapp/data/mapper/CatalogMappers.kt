package com.opsecapp.data.mapper

import com.opsecapp.data.local.entity.CatalogMetaEntity
import com.opsecapp.data.local.entity.CategoryEntity
import com.opsecapp.data.local.entity.ItemCategoryCrossRef
import com.opsecapp.data.local.entity.ItemEntity
import com.opsecapp.data.local.entity.ItemFtsEntity
import com.opsecapp.data.local.entity.ItemLinkEntity
import com.opsecapp.data.local.model.ItemWithLinks
import com.opsecapp.domain.model.CatalogCategory
import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.model.CatalogMeta
import com.opsecapp.domain.model.InstallType
import com.opsecapp.domain.model.SourceConfidence
import com.opsecapp.domain.model.TrustStatus
import com.opsecapp.domain.model.UpstreamLink
import com.opsecapp.domain.model.UpstreamLinkType
import com.opsecapp.network.dto.CatalogDto
import java.time.Instant

fun CategoryEntity.toDomain(): CatalogCategory {
  return CatalogCategory(
    id = id,
    titleExact = titleExact,
    descriptionExact = descriptionExact,
    path = path,
    sortOrder = sortOrder
  )
}

fun ItemWithLinks.toDomain(): CatalogItem {
  return CatalogItem(
    id = item.id,
    titleExact = item.titleExact,
    descriptionExact = item.descriptionExact,
    badgeExact = item.badgeExact,
    sectionExact = item.sectionExact,
    categoryIds = emptyList(),
    upstreamLinks = links.map {
      UpstreamLink(
        type = it.type.toUpstreamLinkType(),
        url = it.url,
        labelExact = it.labelExact,
        expectedSha256 = it.expectedSha256,
        expectedSignerSha256 = it.expectedSignerSha256
      )
    },
    packageId = item.packageId,
    githubRepo = item.githubRepo,
    sourceConfidence = item.sourceConfidence.toSourceConfidence(),
    installType = item.installType.toInstallType()
  )
}

fun CatalogMetaEntity.toDomain(): CatalogMeta {
  return CatalogMeta(
    schemaVersion = schemaVersion,
    generatedAt = Instant.parse(generatedAt),
    lastSyncedFromSourceAt = Instant.parse(lastSyncedFromSourceAt),
    sourceUrl = sourceUrl,
    sourceLanguage = sourceLanguage,
    extractor = extractor,
    signed = signed,
    trustStatus = trustStatus.toTrustStatus()
  )
}

data class PersistableCatalog(
  val categories: List<CategoryEntity>,
  val items: List<ItemEntity>,
  val itemCategoryCrossRefs: List<ItemCategoryCrossRef>,
  val links: List<ItemLinkEntity>,
  val ftsRows: List<ItemFtsEntity>,
  val meta: CatalogMetaEntity
)

fun CatalogDto.toPersistable(trustStatus: TrustStatus): PersistableCatalog {
  val categories = categories.map {
    CategoryEntity(
      id = it.id,
      titleExact = it.titleExact,
      descriptionExact = it.descriptionExact,
      path = it.path,
      sortOrder = it.sortOrder
    )
  }

  val items = items.map {
    ItemEntity(
      id = it.id,
      titleExact = it.titleExact,
      descriptionExact = it.descriptionExact,
      badgeExact = it.badgeExact,
      sectionExact = it.sectionExact,
      packageId = it.packageId,
      githubRepo = it.githubRepo,
      sourceConfidence = it.sourceConfidence.uppercase(),
      installType = it.installType.uppercase()
    )
  }

  val crossRefs = items.flatMap { item ->
    val sourceItem = this.items.first { it.id == item.id }
    sourceItem.categoryIds.map { categoryId ->
      ItemCategoryCrossRef(itemId = sourceItem.id, categoryId = categoryId)
    }
  }

  val links = this.items.flatMap { item ->
    item.upstreamLinks.map { link ->
      ItemLinkEntity(
        itemId = item.id,
        type = link.type.uppercase(),
        url = link.url,
        labelExact = link.labelExact,
        expectedSha256 = link.expectedSha256,
        expectedSignerSha256 = link.expectedSignerSha256
      )
    }
  }

  val ftsRows = this.items.mapIndexed { index, item ->
    ItemFtsEntity(
      rowId = index + 1,
      itemId = item.id,
      titleExact = item.titleExact,
      descriptionExact = item.descriptionExact,
      sectionExact = item.sectionExact,
      linksText = item.upstreamLinks.joinToString(separator = " ") { link -> link.url }
    )
  }

  val meta = CatalogMetaEntity(
    schemaVersion = schemaVersion,
    generatedAt = generatedAt,
    lastSyncedFromSourceAt = lastSyncedFromSourceAt,
    sourceUrl = sourceUrl,
    sourceLanguage = sourceLanguage,
    extractor = extractor,
    signed = true,
    trustStatus = trustStatus.name
  )

  return PersistableCatalog(
    categories = categories,
    items = items,
    itemCategoryCrossRefs = crossRefs,
    links = links,
    ftsRows = ftsRows,
    meta = meta
  )
}

private fun String.toSourceConfidence(): SourceConfidence {
  return when (uppercase()) {
    "HIGH" -> SourceConfidence.HIGH
    "MEDIUM" -> SourceConfidence.MEDIUM
    else -> SourceConfidence.LOW
  }
}

private fun String.toInstallType(): InstallType {
  return when (uppercase()) {
    "FDROID" -> InstallType.FDROID
    "GITHUB_APK" -> InstallType.GITHUB_APK
    "OFFICIAL_APK" -> InstallType.OFFICIAL_APK
    "OFFICIAL_SITE" -> InstallType.OFFICIAL_SITE
    "WEB_SERVICE" -> InstallType.WEB_SERVICE
    else -> InstallType.UNKNOWN
  }
}

private fun String.toUpstreamLinkType(): UpstreamLinkType {
  return when (uppercase()) {
    "FDROID" -> UpstreamLinkType.FDROID
    "GITHUB" -> UpstreamLinkType.GITHUB
    "OFFICIAL" -> UpstreamLinkType.OFFICIAL
    else -> UpstreamLinkType.OTHER
  }
}

private fun String.toTrustStatus(): TrustStatus {
  return runCatching { TrustStatus.valueOf(this) }.getOrElse { TrustStatus.UNTRUSTED }
}
