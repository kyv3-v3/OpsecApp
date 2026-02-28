package com.opsecapp.domain.usecase

import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.model.InstallType
import com.opsecapp.domain.model.SourceConfidence
import com.opsecapp.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow

class SearchCatalogUseCase(
  private val catalogRepository: CatalogRepository
) {
  operator fun invoke(
    query: String,
    sourceConfidence: SourceConfidence? = null,
    installType: InstallType? = null
  ): Flow<List<CatalogItem>> {
    return catalogRepository.search(query, sourceConfidence, installType)
  }
}
