package com.opsecapp.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.model.InstallType
import com.opsecapp.domain.model.SourceConfidence
import com.opsecapp.domain.usecase.SearchCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
  val query: String = "",
  val sourceConfidence: SourceConfidence? = null,
  val installType: InstallType? = null,
  val results: List<CatalogItem> = emptyList()
)

class SearchViewModel(
  private val searchCatalogUseCase: SearchCatalogUseCase
) : ViewModel() {
  private val query = MutableStateFlow("")
  private val sourceConfidence = MutableStateFlow<SourceConfidence?>(null)
  private val installType = MutableStateFlow<InstallType?>(null)

  val state: StateFlow<SearchUiState> = combine(
    query.debounce(150),
    sourceConfidence,
    installType
  ) { q, confidence, install ->
    Triple(q, confidence, install)
  }
    .flatMapLatest { (q, confidence, install) ->
      searchCatalogUseCase(
        query = q,
        sourceConfidence = confidence,
        installType = install
      ).map { results ->
        SearchUiState(
          query = q,
          sourceConfidence = confidence,
          installType = install,
          results = results
        )
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

  fun onQueryChange(value: String) {
    query.value = value
  }

  fun onSourceConfidenceChange(value: SourceConfidence?) {
    sourceConfidence.value = value
  }

  fun onInstallTypeChange(value: InstallType?) {
    installType.value = value
  }
}
