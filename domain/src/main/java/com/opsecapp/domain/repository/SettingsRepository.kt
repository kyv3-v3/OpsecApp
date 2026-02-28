package com.opsecapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
  fun observeCatalogBaseUrl(): Flow<String>
  suspend fun setCatalogBaseUrl(url: String)
}
