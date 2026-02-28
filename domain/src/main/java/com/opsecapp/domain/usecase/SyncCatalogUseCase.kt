package com.opsecapp.domain.usecase

import com.opsecapp.domain.model.TrustStatus
import com.opsecapp.domain.repository.CatalogRepository

class SyncCatalogUseCase(
  private val catalogRepository: CatalogRepository
) {
  suspend operator fun invoke(force: Boolean = false): TrustStatus {
    return catalogRepository.syncCatalog(force)
  }
}
