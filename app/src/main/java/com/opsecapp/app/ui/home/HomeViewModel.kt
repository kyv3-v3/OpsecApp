package com.opsecapp.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opsecapp.domain.model.HomeState
import com.opsecapp.domain.model.TrustStatus
import com.opsecapp.domain.usecase.ObserveHomeUseCase
import com.opsecapp.domain.usecase.SyncCatalogUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
  val isRefreshing: Boolean = false,
  val home: HomeState? = null
)

sealed interface HomeRefreshEvent {
  data object CatalogUpdated : HomeRefreshEvent
  data object InvalidSignature : HomeRefreshEvent
  data object InvalidFingerprint : HomeRefreshEvent
  data object NetworkError : HomeRefreshEvent
  data object ParseError : HomeRefreshEvent
  data object Untrusted : HomeRefreshEvent
  data object SyncFailed : HomeRefreshEvent
}

class HomeViewModel(
  observeHomeUseCase: ObserveHomeUseCase,
  private val syncCatalogUseCase: SyncCatalogUseCase
) : ViewModel() {
  private val refreshing = MutableStateFlow(false)
  private val _events = MutableSharedFlow<HomeRefreshEvent>()
  val events = _events.asSharedFlow()

  val state: StateFlow<HomeUiState> = observeHomeUseCase()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = HomeState(emptyList(), emptyList(), "", TrustStatus.UNTRUSTED)
    )
    .let { flow ->
      combine(flow, refreshing) { home, isRefreshing ->
        HomeUiState(isRefreshing = isRefreshing, home = home)
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

  fun refresh() {
    if (refreshing.value) return
    viewModelScope.launch {
      refreshing.value = true
      val status = runCatching {
        syncCatalogUseCase(force = true)
      }.getOrElse {
        _events.emit(HomeRefreshEvent.SyncFailed)
        refreshing.value = false
        return@launch
      }

      _events.emit(status.toRefreshEvent())
      refreshing.value = false
    }
  }

  private fun TrustStatus.toRefreshEvent(): HomeRefreshEvent {
    return when (this) {
      TrustStatus.TRUSTED -> HomeRefreshEvent.CatalogUpdated
      TrustStatus.INVALID_SIGNATURE -> HomeRefreshEvent.InvalidSignature
      TrustStatus.INVALID_FINGERPRINT -> HomeRefreshEvent.InvalidFingerprint
      TrustStatus.NETWORK_ERROR -> HomeRefreshEvent.NetworkError
      TrustStatus.PARSE_ERROR -> HomeRefreshEvent.ParseError
      TrustStatus.UNTRUSTED -> HomeRefreshEvent.Untrusted
    }
  }
}
