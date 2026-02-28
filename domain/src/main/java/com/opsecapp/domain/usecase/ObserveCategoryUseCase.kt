package com.opsecapp.domain.usecase

import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow

class ObserveCategoryUseCase(
  private val catalogRepository: CatalogRepository
) {
  operator fun invoke(categoryId: String): Flow<List<CatalogItem>> {
    return catalogRepository.observeCategoryItems(categoryId)
  }
}
