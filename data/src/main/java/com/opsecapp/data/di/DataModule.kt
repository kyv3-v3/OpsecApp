package com.opsecapp.data.di

import android.content.Context
import androidx.room.Room
import com.opsecapp.data.local.db.CatalogDatabase
import com.opsecapp.data.repository.CatalogRepositoryImpl
import com.opsecapp.data.settings.SettingsRepositoryImpl
import com.opsecapp.domain.repository.CatalogRepository
import com.opsecapp.domain.repository.SettingsRepository
import com.opsecapp.network.http.NetworkClientFactory
import com.opsecapp.network.source.CatalogRemoteDataSource
import com.opsecapp.security.CatalogSignatureVerifier

object DataModule {
  fun provideDatabase(context: Context): CatalogDatabase {
    return Room.databaseBuilder(
      context.applicationContext,
      CatalogDatabase::class.java,
      "catalog.db"
    ).fallbackToDestructiveMigration().build()
  }

  fun provideSettingsRepository(
    context: Context,
    defaultCatalogBaseUrl: String
  ): SettingsRepository {
    return SettingsRepositoryImpl(context.applicationContext, defaultCatalogBaseUrl)
  }

  fun provideRemoteDataSource(debug: Boolean): CatalogRemoteDataSource {
    return CatalogRemoteDataSource(NetworkClientFactory.create(debug))
  }

  fun provideSignatureVerifier(
    publicKeyPem: String,
    pinnedFingerprint: String
  ): CatalogSignatureVerifier {
    return CatalogSignatureVerifier(publicKeyPem, pinnedFingerprint)
  }

  fun provideCatalogRepository(
    database: CatalogDatabase,
    remoteDataSource: CatalogRemoteDataSource,
    signatureVerifier: CatalogSignatureVerifier,
    settingsRepository: SettingsRepository
  ): CatalogRepository {
    return CatalogRepositoryImpl(
      dao = database.catalogDao(),
      remoteDataSource = remoteDataSource,
      signatureVerifier = signatureVerifier,
      settingsRepository = settingsRepository
    )
  }
}
