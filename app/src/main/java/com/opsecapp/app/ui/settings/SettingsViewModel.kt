package com.opsecapp.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opsecapp.app.BuildConfig
import com.opsecapp.app.install.InstallManager
import com.opsecapp.app.install.InstallResult
import com.opsecapp.app.update.AppUpdateCheckResult
import com.opsecapp.app.update.AppUpdateChecker
import com.opsecapp.domain.model.CatalogMeta
import com.opsecapp.domain.model.UpstreamLink
import com.opsecapp.domain.model.UpstreamLinkType
import com.opsecapp.domain.model.TrustStatus
import com.opsecapp.domain.repository.CatalogRepository
import com.opsecapp.domain.repository.SettingsRepository
import com.opsecapp.domain.usecase.SyncCatalogUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI

data class AvailableAppUpdate(
  val tagName: String,
  val apkUrl: String?,
  val releasePageUrl: String,
  val publishedAt: String?,
  val notes: String? = null
)

data class SettingsUiState(
  val catalogBaseUrl: String = "",
  val catalogUrlMessage: String? = null,
  val catalogUrlMessageIsError: Boolean = false,
  val meta: CatalogMeta? = null,
  val trustStatus: TrustStatus = TrustStatus.UNTRUSTED,
  val isSyncingCatalog: Boolean = false,
  val isCheckingAppUpdate: Boolean = false,
  val statusMessage: String? = null,
  val availableAppUpdate: AvailableAppUpdate? = null
)

class SettingsViewModel(
  private val settingsRepository: SettingsRepository,
  private val catalogRepository: CatalogRepository,
  private val syncCatalogUseCase: SyncCatalogUseCase,
  private val appUpdateChecker: AppUpdateChecker,
  private val installManager: InstallManager
) : ViewModel() {
  private data class OperationState(
    val isSyncingCatalog: Boolean = false,
    val isCheckingAppUpdate: Boolean = false,
    val catalogUrlMessage: String? = null,
    val catalogUrlMessageIsError: Boolean = false,
    val statusMessage: String? = null,
    val availableAppUpdate: AvailableAppUpdate? = null
  )

  private val operationState = MutableStateFlow(OperationState())
  private val _events = MutableSharedFlow<InstallResult>()
  val events = _events.asSharedFlow()

  val state: StateFlow<SettingsUiState> = combine(
    settingsRepository.observeCatalogBaseUrl(),
    catalogRepository.observeMeta(),
    operationState
  ) { catalogUrl, meta, opState ->
    SettingsUiState(
      catalogBaseUrl = catalogUrl,
      catalogUrlMessage = opState.catalogUrlMessage,
      catalogUrlMessageIsError = opState.catalogUrlMessageIsError,
      meta = meta,
      trustStatus = meta?.trustStatus ?: TrustStatus.UNTRUSTED,
      isSyncingCatalog = opState.isSyncingCatalog,
      isCheckingAppUpdate = opState.isCheckingAppUpdate,
      statusMessage = opState.statusMessage,
      availableAppUpdate = opState.availableAppUpdate
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

  fun saveCatalogBaseUrl(url: String) {
    viewModelScope.launch {
      when (val validation = validateCatalogBaseUrl(url)) {
        is CatalogUrlValidation.Valid -> {
          settingsRepository.setCatalogBaseUrl(validation.normalizedUrl)
          operationState.update {
            it.copy(
              catalogUrlMessage = CATALOG_URL_SAVED,
              catalogUrlMessageIsError = false
            )
          }
        }

        is CatalogUrlValidation.Invalid -> {
          operationState.update {
            it.copy(
              catalogUrlMessage = validation.message,
              catalogUrlMessageIsError = true
            )
          }
        }
      }
    }
  }

  fun syncNow() {
    if (operationState.value.isSyncingCatalog) return
    operationState.update {
      it.copy(
        isSyncingCatalog = true,
        statusMessage = "Sync catalog in progress..."
      )
    }

    viewModelScope.launch {
      val trustStatus = syncCatalogUseCase(force = true)
      operationState.update {
        it.copy(
          isSyncingCatalog = false,
          statusMessage = when (trustStatus) {
            TrustStatus.TRUSTED -> "Catalog updated and signature verified."
            TrustStatus.INVALID_SIGNATURE -> "Catalog rejected: invalid signature."
            TrustStatus.INVALID_FINGERPRINT -> "Catalog rejected: pinned key fingerprint mismatch."
            TrustStatus.NETWORK_ERROR -> "Catalog sync failed due to network error."
            TrustStatus.PARSE_ERROR -> "Catalog sync failed: parser/schema mismatch."
            TrustStatus.UNTRUSTED -> "Catalog is currently untrusted."
          }
        )
      }
    }
  }

  fun checkAppUpdates() {
    if (operationState.value.isCheckingAppUpdate) return
    operationState.update {
      it.copy(
        isCheckingAppUpdate = true,
        statusMessage = "Checking app releases...",
        availableAppUpdate = null
      )
    }

    viewModelScope.launch {
      when (val result = appUpdateChecker.checkLatestRelease()) {
        is AppUpdateCheckResult.UpdateAvailable -> {
          val hasApk = !result.release.apkDownloadUrl.isNullOrBlank()
          val updateHint = if (hasApk) "" else " (no installable APK asset in this release)"
          operationState.update {
            it.copy(
              isCheckingAppUpdate = false,
              statusMessage = "Update available: ${result.release.tagName}$updateHint",
              availableAppUpdate = AvailableAppUpdate(
                tagName = result.release.tagName,
                apkUrl = result.release.apkDownloadUrl,
                releasePageUrl = result.release.htmlUrl,
                publishedAt = result.release.publishedAt,
                notes = result.release.notes
              )
            )
          }
        }

        is AppUpdateCheckResult.UpToDate -> {
          operationState.update {
            it.copy(
              isCheckingAppUpdate = false,
              statusMessage = "App is up to date (${result.currentVersion}).",
              availableAppUpdate = null
            )
          }
        }

        is AppUpdateCheckResult.Error -> {
          operationState.update {
            it.copy(
              isCheckingAppUpdate = false,
              statusMessage = "App update check failed: ${result.message}",
              availableAppUpdate = null
            )
          }
        }
      }
    }
  }

  fun installAppUpdate() {
    val update = operationState.value.availableAppUpdate ?: return
    val apkUrl = update.apkUrl
    if (apkUrl.isNullOrBlank()) {
      viewModelScope.launch {
        _events.emit(
          InstallResult.Warning("No APK asset found in latest release. Open the release page instead.")
        )
      }
      return
    }

    viewModelScope.launch {
      val result = installManager.downloadVerifyAndInstall(
        UpstreamLink(
          type = UpstreamLinkType.GITHUB,
          url = apkUrl,
          labelExact = "GitHub Release APK"
        ),
        expectedPackageName = BuildConfig.APPLICATION_ID
      )
      _events.emit(result)
    }
  }

  private fun validateCatalogBaseUrl(rawUrl: String): CatalogUrlValidation {
    val candidate = rawUrl.trim()
    if (candidate.isBlank()) {
      return CatalogUrlValidation.Invalid(CATALOG_URL_EMPTY)
    }

    val uri = runCatching { URI(candidate) }.getOrNull()
      ?: return CatalogUrlValidation.Invalid(CATALOG_URL_INVALID_FORMAT)

    val scheme = uri.scheme?.lowercase()
    if (scheme != "https") {
      return CatalogUrlValidation.Invalid(CATALOG_URL_HTTPS_REQUIRED)
    }

    if (uri.host.isNullOrBlank()) {
      return CatalogUrlValidation.Invalid(CATALOG_URL_INVALID_HOST)
    }

    if (!uri.query.isNullOrBlank() || !uri.fragment.isNullOrBlank()) {
      return CatalogUrlValidation.Invalid(CATALOG_URL_NO_QUERY_OR_FRAGMENT)
    }

    val pathLower = uri.path.orEmpty().lowercase()
    if (pathLower.endsWith("/catalog.json") || pathLower.endsWith("/catalog.sig")) {
      return CatalogUrlValidation.Invalid(CATALOG_URL_EXPECTS_BASE_DIRECTORY)
    }

    return CatalogUrlValidation.Valid(candidate.trimEnd('/'))
  }

  private sealed interface CatalogUrlValidation {
    data class Valid(val normalizedUrl: String) : CatalogUrlValidation
    data class Invalid(val message: String) : CatalogUrlValidation
  }

  private companion object {
    private const val CATALOG_URL_EMPTY =
      "Catalog URL cannot be empty."
    private const val CATALOG_URL_INVALID_FORMAT =
      "Catalog URL is not a valid absolute URL."
    private const val CATALOG_URL_HTTPS_REQUIRED =
      "Catalog URL must use HTTPS."
    private const val CATALOG_URL_INVALID_HOST =
      "Catalog URL host is invalid."
    private const val CATALOG_URL_NO_QUERY_OR_FRAGMENT =
      "Catalog URL must not include query parameters or fragments."
    private const val CATALOG_URL_EXPECTS_BASE_DIRECTORY =
      "Use the catalog base directory URL, not catalog.json/catalog.sig directly."
    private const val CATALOG_URL_SAVED =
      "Catalog source URL saved."
  }
}
