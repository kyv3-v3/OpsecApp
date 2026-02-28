package com.opsecapp.domain.usecase

import com.opsecapp.domain.model.HomeState
import com.opsecapp.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ObserveHomeUseCase(
  private val catalogRepository: CatalogRepository
) {
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")

  operator fun invoke(): Flow<HomeState> {
    return combine(
      catalogRepository.observeCategories(),
      catalogRepository.observeHighlights(),
      catalogRepository.observeMeta()
    ) { categories, highlights, meta ->
      val lastSyncedText = meta?.lastSyncedFromSourceAt
        ?.atZone(ZoneId.systemDefault())
        ?.format(formatter)
        ?: "Never"

      HomeState(
        categories = categories,
        highlights = highlights,
        lastSyncedText = lastSyncedText,
        trustStatus = meta?.trustStatus ?: com.opsecapp.domain.model.TrustStatus.UNTRUSTED
      )
    }
  }
}
