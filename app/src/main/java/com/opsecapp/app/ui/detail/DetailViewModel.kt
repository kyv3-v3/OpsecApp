package com.opsecapp.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opsecapp.app.install.InstallManager
import com.opsecapp.app.install.InstallResult
import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.model.UpstreamLinkType
import com.opsecapp.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
  val itemId: String = "",
  val item: CatalogItem? = null
)

class DetailViewModel(
  private val catalogRepository: CatalogRepository,
  private val installManager: InstallManager
) : ViewModel() {
  private val itemId = MutableStateFlow("")
  private val _events = MutableSharedFlow<InstallResult>()
  val events = _events.asSharedFlow()

  val state: StateFlow<DetailUiState> = itemId
    .flatMapLatest { id ->
      if (id.isBlank()) {
        kotlinx.coroutines.flow.flowOf(DetailUiState())
      } else {
        catalogRepository.observeItem(id).map { item ->
          DetailUiState(itemId = id, item = item)
        }
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

  fun load(id: String) {
    itemId.value = id
  }

  fun installPreferred() {
    val item = state.value.item ?: return

    viewModelScope.launch {
      val fdroidFirst = item.upstreamLinks.firstOrNull { it.type == UpstreamLinkType.FDROID }
      if (fdroidFirst != null || !item.packageId.isNullOrBlank()) {
        _events.emit(installManager.launchFdroid(item))
        return@launch
      }

      val apkLink = item.upstreamLinks.firstOrNull {
        it.url.endsWith(".apk") ||
          (it.type == UpstreamLinkType.GITHUB && it.url.contains("/releases"))
      }

      if (apkLink != null) {
        _events.emit(installManager.downloadVerifyAndInstall(apkLink))
      } else {
        _events.emit(InstallResult.Error("No installable APK/F-Droid link found for this item."))
      }
    }
  }

  fun promptInstallFdroid() {
    val intent = installManager.promptInstallFdroidIntent()
    _events.tryEmit(InstallResult.NeedsUserAction("Install F-Droid from the official website.", intent))
  }
}
