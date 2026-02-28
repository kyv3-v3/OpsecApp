package com.opsecapp.app.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.usecase.ObserveCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CategoryUiState(
  val categoryId: String = "",
  val items: List<CatalogItem> = emptyList()
)

class CategoryViewModel(
  private val observeCategoryUseCase: ObserveCategoryUseCase
) : ViewModel() {
  private val categoryId = MutableStateFlow("")

  val state: StateFlow<CategoryUiState> = categoryId
    .flatMapLatest { id ->
      if (id.isBlank()) {
        kotlinx.coroutines.flow.flowOf(CategoryUiState())
      } else {
        observeCategoryUseCase(id).let { flow ->
          flow.map { items -> CategoryUiState(categoryId = id, items = items) }
        }
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryUiState())

  fun load(category: String) {
    categoryId.value = category
  }
}
