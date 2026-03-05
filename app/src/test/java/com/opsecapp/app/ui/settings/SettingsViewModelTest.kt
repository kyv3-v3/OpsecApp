package com.opsecapp.app.ui.settings

import com.google.common.truth.Truth.assertThat
import com.opsecapp.app.install.InstallManager
import com.opsecapp.app.install.InstallResult
import com.opsecapp.app.test.MainDispatcherRule
import com.opsecapp.app.update.AppReleaseInfo
import com.opsecapp.app.update.AppUpdateCheckResult
import com.opsecapp.app.update.AppUpdateChecker
import com.opsecapp.domain.model.TrustStatus
import com.opsecapp.domain.repository.CatalogRepository
import com.opsecapp.domain.repository.SettingsRepository
import com.opsecapp.domain.usecase.SyncCatalogUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun checkAppUpdates_sets_available_update_when_new_release_exists() = runTest {
    val settingsRepository = mockk<SettingsRepository>()
    every { settingsRepository.observeCatalogBaseUrl() } returns flowOf("https://example.com/catalog")

    val catalogRepository = mockk<CatalogRepository>()
    every { catalogRepository.observeMeta() } returns flowOf(null)

    val syncCatalogUseCase = mockk<SyncCatalogUseCase>()
    coEvery { syncCatalogUseCase.invoke(any()) } returns TrustStatus.TRUSTED

    val appUpdateChecker = mockk<AppUpdateChecker>()
    coEvery { appUpdateChecker.checkLatestRelease() } returns AppUpdateCheckResult.UpdateAvailable(
      currentVersion = "1.0.0",
      release = AppReleaseInfo(
        tagName = "v1.1.0",
        htmlUrl = "https://github.com/kyv3-v3/OpsecApp/releases/tag/v1.1.0",
        publishedAt = "2026-03-01T12:00:00Z",
        notes = "notes",
        apkDownloadUrl = "https://github.com/kyv3-v3/OpsecApp/releases/download/v1.1.0/app.apk"
      )
    )

    val installManager = mockk<InstallManager>()

    val viewModel = SettingsViewModel(
      settingsRepository = settingsRepository,
      catalogRepository = catalogRepository,
      syncCatalogUseCase = syncCatalogUseCase,
      appUpdateChecker = appUpdateChecker,
      installManager = installManager
    )
    val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.state.collect { }
    }

    viewModel.checkAppUpdates()
    advanceUntilIdle()

    val state = viewModel.state.value
    assertThat(state.availableAppUpdate).isNotNull()
    assertThat(state.availableAppUpdate?.tagName).isEqualTo("v1.1.0")
    assertThat(state.isCheckingAppUpdate).isFalse()

    collector.cancel()
  }

  @Test
  fun installAppUpdate_uses_install_manager_with_release_apk() = runTest {
    val settingsRepository = mockk<SettingsRepository>()
    every { settingsRepository.observeCatalogBaseUrl() } returns flowOf("https://example.com/catalog")

    val catalogRepository = mockk<CatalogRepository>()
    every { catalogRepository.observeMeta() } returns flowOf(null)

    val syncCatalogUseCase = mockk<SyncCatalogUseCase>()
    coEvery { syncCatalogUseCase.invoke(any()) } returns TrustStatus.TRUSTED

    val appUpdateChecker = mockk<AppUpdateChecker>()
    coEvery { appUpdateChecker.checkLatestRelease() } returns AppUpdateCheckResult.UpdateAvailable(
      currentVersion = "1.0.0",
      release = AppReleaseInfo(
        tagName = "v1.2.0",
        htmlUrl = "https://github.com/kyv3-v3/OpsecApp/releases/tag/v1.2.0",
        publishedAt = null,
        notes = null,
        apkDownloadUrl = "https://github.com/kyv3-v3/OpsecApp/releases/download/v1.2.0/app.apk"
      )
    )

    val installManager = mockk<InstallManager>()
    coEvery { installManager.downloadVerifyAndInstall(any()) } returns InstallResult.InstallerLaunched

    val viewModel = SettingsViewModel(
      settingsRepository = settingsRepository,
      catalogRepository = catalogRepository,
      syncCatalogUseCase = syncCatalogUseCase,
      appUpdateChecker = appUpdateChecker,
      installManager = installManager
    )
    val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.state.collect { }
    }

    viewModel.checkAppUpdates()
    advanceUntilIdle()
    viewModel.installAppUpdate()
    advanceUntilIdle()

    coVerify(exactly = 1) {
      installManager.downloadVerifyAndInstall(match { it.url.endsWith("/app.apk") })
    }

    collector.cancel()
  }

  @Test
  fun saveCatalogBaseUrl_rejects_invalid_non_https_url() = runTest {
    val settingsRepository = mockk<SettingsRepository>()
    every { settingsRepository.observeCatalogBaseUrl() } returns flowOf("https://example.com/catalog")
    coEvery { settingsRepository.setCatalogBaseUrl(any()) } returns Unit

    val catalogRepository = mockk<CatalogRepository>()
    every { catalogRepository.observeMeta() } returns flowOf(null)

    val syncCatalogUseCase = mockk<SyncCatalogUseCase>()
    coEvery { syncCatalogUseCase.invoke(any()) } returns TrustStatus.TRUSTED

    val appUpdateChecker = mockk<AppUpdateChecker>()
    coEvery { appUpdateChecker.checkLatestRelease() } returns AppUpdateCheckResult.UpToDate(
      currentVersion = "1.0.0",
      latestVersion = "1.0.0"
    )

    val installManager = mockk<InstallManager>()

    val viewModel = SettingsViewModel(
      settingsRepository = settingsRepository,
      catalogRepository = catalogRepository,
      syncCatalogUseCase = syncCatalogUseCase,
      appUpdateChecker = appUpdateChecker,
      installManager = installManager
    )
    val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.state.collect { }
    }

    viewModel.saveCatalogBaseUrl("http://example.com/catalog")
    advanceUntilIdle()

    coVerify(exactly = 0) { settingsRepository.setCatalogBaseUrl(any()) }
    assertThat(viewModel.state.value.catalogUrlMessageIsError).isTrue()
    assertThat(viewModel.state.value.catalogUrlMessage).contains("HTTPS")

    collector.cancel()
  }

  @Test
  fun saveCatalogBaseUrl_trims_and_saves_valid_https_url() = runTest {
    val settingsRepository = mockk<SettingsRepository>()
    every { settingsRepository.observeCatalogBaseUrl() } returns flowOf("https://example.com/catalog")
    coEvery { settingsRepository.setCatalogBaseUrl(any()) } returns Unit

    val catalogRepository = mockk<CatalogRepository>()
    every { catalogRepository.observeMeta() } returns flowOf(null)

    val syncCatalogUseCase = mockk<SyncCatalogUseCase>()
    coEvery { syncCatalogUseCase.invoke(any()) } returns TrustStatus.TRUSTED

    val appUpdateChecker = mockk<AppUpdateChecker>()
    coEvery { appUpdateChecker.checkLatestRelease() } returns AppUpdateCheckResult.UpToDate(
      currentVersion = "1.0.0",
      latestVersion = "1.0.0"
    )

    val installManager = mockk<InstallManager>()

    val viewModel = SettingsViewModel(
      settingsRepository = settingsRepository,
      catalogRepository = catalogRepository,
      syncCatalogUseCase = syncCatalogUseCase,
      appUpdateChecker = appUpdateChecker,
      installManager = installManager
    )
    val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.state.collect { }
    }

    viewModel.saveCatalogBaseUrl("  https://example.com/catalog/  ")
    advanceUntilIdle()

    coVerify(exactly = 1) {
      settingsRepository.setCatalogBaseUrl("https://example.com/catalog")
    }
    assertThat(viewModel.state.value.catalogUrlMessageIsError).isFalse()
    assertThat(viewModel.state.value.catalogUrlMessage).isEqualTo("Catalog source URL saved.")

    collector.cancel()
  }
}
