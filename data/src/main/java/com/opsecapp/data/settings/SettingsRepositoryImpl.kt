package com.opsecapp.data.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import com.opsecapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_settings")

class SettingsRepositoryImpl(
  private val context: Context,
  private val defaultCatalogBaseUrl: String
) : SettingsRepository {

  companion object {
    private val CATALOG_BASE_URL_KEY = stringPreferencesKey("catalog_base_url")
  }

  override fun observeCatalogBaseUrl(): Flow<String> {
    return context.dataStore.data.map { prefs ->
      prefs[CATALOG_BASE_URL_KEY] ?: defaultCatalogBaseUrl
    }
  }

  override suspend fun setCatalogBaseUrl(url: String) {
    context.dataStore.edit { prefs ->
      prefs[CATALOG_BASE_URL_KEY] = url
    }
  }
}
