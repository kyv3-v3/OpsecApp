package com.opsecapp.app.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.opsecapp.app.BuildConfig
import com.opsecapp.app.install.InstallManager
import com.opsecapp.app.update.AppUpdateChecker
import com.opsecapp.app.ui.category.CategoryViewModel
import com.opsecapp.app.ui.detail.DetailViewModel
import com.opsecapp.app.ui.home.HomeViewModel
import com.opsecapp.app.ui.search.SearchViewModel
import com.opsecapp.app.ui.settings.SettingsViewModel
import com.opsecapp.data.di.DataModule
import com.opsecapp.domain.repository.CatalogRepository
import com.opsecapp.domain.repository.SettingsRepository
import com.opsecapp.domain.usecase.ObserveCategoryUseCase
import com.opsecapp.domain.usecase.ObserveHomeUseCase
import com.opsecapp.domain.usecase.SearchCatalogUseCase
import com.opsecapp.domain.usecase.SyncCatalogUseCase

class AppContainer(context: Context) {
  private val appContext = context.applicationContext

  private val database = DataModule.provideDatabase(appContext)
  val settingsRepository: SettingsRepository = DataModule.provideSettingsRepository(
    appContext,
    BuildConfig.DEFAULT_CATALOG_BASE_URL
  )

  private val remoteDataSource = DataModule.provideRemoteDataSource(BuildConfig.DEBUG)
  private val signatureVerifier = DataModule.provideSignatureVerifier(
    BuildConfig.CATALOG_PUBLIC_KEY_PEM,
    BuildConfig.CATALOG_PUBLIC_KEY_FINGERPRINT_SHA256
  )

  val catalogRepository: CatalogRepository = DataModule.provideCatalogRepository(
    database = database,
    remoteDataSource = remoteDataSource,
    signatureVerifier = signatureVerifier,
    settingsRepository = settingsRepository
  )

  val observeHomeUseCase = ObserveHomeUseCase(catalogRepository)
  val observeCategoryUseCase = ObserveCategoryUseCase(catalogRepository)
  val searchCatalogUseCase = SearchCatalogUseCase(catalogRepository)
  val syncCatalogUseCase = SyncCatalogUseCase(catalogRepository)

  val installManager = InstallManager(appContext)
  val appUpdateChecker = AppUpdateChecker(
    latestReleaseApiUrl = BuildConfig.APP_RELEASES_LATEST_API_URL,
    currentVersionName = BuildConfig.VERSION_NAME,
    fallbackReleasesPageUrl = BuildConfig.APP_RELEASES_PAGE_URL
  )

  val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
          HomeViewModel(observeHomeUseCase, syncCatalogUseCase) as T
        }

        modelClass.isAssignableFrom(CategoryViewModel::class.java) -> {
          CategoryViewModel(observeCategoryUseCase) as T
        }

        modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
          SearchViewModel(searchCatalogUseCase) as T
        }

        modelClass.isAssignableFrom(DetailViewModel::class.java) -> {
          DetailViewModel(catalogRepository, installManager) as T
        }

        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
          SettingsViewModel(
            settingsRepository = settingsRepository,
            catalogRepository = catalogRepository,
            syncCatalogUseCase = syncCatalogUseCase,
            appUpdateChecker = appUpdateChecker,
            installManager = installManager
          ) as T
        }

        else -> error("Unknown ViewModel ${modelClass.simpleName}")
      }
    }
  }
}
