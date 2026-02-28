package com.opsecapp.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opsecapp.domain.model.HomeState
import com.opsecapp.domain.usecase.ObserveHomeUseCase
import com.opsecapp.domain.usecase.SyncCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
  val isRefreshing: Boolean = false,
  val home: HomeState? = null,
  val error: String? = null
)

class HomeViewModel(
  observeHomeUseCase: ObserveHomeUseCase,
  private val syncCatalogUseCase: SyncCatalogUseCase
) : ViewModel() {
  private val refreshing = MutableStateFlow(false)

  val state: StateFlow<HomeUiState> = observeHomeUseCase()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = HomeState(emptyList(), emptyList(), "Never", com.opsecapp.domain.model.TrustStatus.UNTRUSTED)
    )
    .let { flow ->
      kotlinx.coroutines.flow.combine(flow, refreshing) { home, isRefreshing ->
        HomeUiState(isRefreshing = isRefreshing, home = home)
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

  fun refresh() {
    viewModelScope.launch {
      refreshing.value = true
      runCatching { syncCatalogUseCase(force = true) }
      refreshing.value = false
    }
  }
}
