package com.opsecapp.domain.repository

import com.opsecapp.domain.model.CatalogCategory
import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.model.CatalogMeta
import com.opsecapp.domain.model.InstallType
import com.opsecapp.domain.model.SourceConfidence
import com.opsecapp.domain.model.TrustStatus
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
  fun observeCategories(): Flow<List<CatalogCategory>>
  fun observeHighlights(limit: Int = 8): Flow<List<CatalogItem>>
  fun observeCategoryItems(categoryId: String): Flow<List<CatalogItem>>
  fun observeItem(itemId: String): Flow<CatalogItem?>
  fun search(
    query: String,
    confidence: SourceConfidence? = null,
    installType: InstallType? = null
  ): Flow<List<CatalogItem>>

  fun observeMeta(): Flow<CatalogMeta?>
  suspend fun syncCatalog(force: Boolean = false): TrustStatus
}
