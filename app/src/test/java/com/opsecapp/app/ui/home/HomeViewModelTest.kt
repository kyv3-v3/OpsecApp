package com.opsecapp.app.ui.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.opsecapp.app.test.MainDispatcherRule
import com.opsecapp.domain.model.HomeState
import com.opsecapp.domain.model.TrustStatus
import com.opsecapp.domain.usecase.ObserveHomeUseCase
import com.opsecapp.domain.usecase.SyncCatalogUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun refresh_emits_catalog_updated_event_when_sync_is_trusted() = runTest {
    val observeHomeUseCase = mockk<ObserveHomeUseCase>()
    every { observeHomeUseCase.invoke() } returns flowOf(homeState())

    val syncCatalogUseCase = mockk<SyncCatalogUseCase>()
    coEvery { syncCatalogUseCase.invoke(true) } returns TrustStatus.TRUSTED

    val viewModel = HomeViewModel(
      observeHomeUseCase = observeHomeUseCase,
      syncCatalogUseCase = syncCatalogUseCase
    )

    viewModel.events.test {
      viewModel.refresh()
      assertThat(awaitItem()).isEqualTo(HomeRefreshEvent.CatalogUpdated)
      cancelAndIgnoreRemainingEvents()
    }

    advanceUntilIdle()
    assertThat(viewModel.state.value.isRefreshing).isFalse()
  }

  @Test
  fun refresh_emits_invalid_signature_event_when_sync_rejects_signature() = runTest {
    val observeHomeUseCase = mockk<ObserveHomeUseCase>()
    every { observeHomeUseCase.invoke() } returns flowOf(homeState())

    val syncCatalogUseCase = mockk<SyncCatalogUseCase>()
    coEvery { syncCatalogUseCase.invoke(true) } returns TrustStatus.INVALID_SIGNATURE

    val viewModel = HomeViewModel(
      observeHomeUseCase = observeHomeUseCase,
      syncCatalogUseCase = syncCatalogUseCase
    )

    viewModel.events.test {
      viewModel.refresh()
      assertThat(awaitItem()).isEqualTo(HomeRefreshEvent.InvalidSignature)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun refresh_emits_sync_failed_event_when_use_case_throws() = runTest {
    val observeHomeUseCase = mockk<ObserveHomeUseCase>()
    every { observeHomeUseCase.invoke() } returns flowOf(homeState())

    val syncCatalogUseCase = mockk<SyncCatalogUseCase>()
    coEvery { syncCatalogUseCase.invoke(true) } throws IOException("network down")

    val viewModel = HomeViewModel(
      observeHomeUseCase = observeHomeUseCase,
      syncCatalogUseCase = syncCatalogUseCase
    )

    viewModel.events.test {
      viewModel.refresh()
      assertThat(awaitItem()).isEqualTo(HomeRefreshEvent.SyncFailed)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun homeState(): HomeState {
    return HomeState(
      categories = emptyList(),
      highlights = emptyList(),
      lastSyncedText = "",
      trustStatus = TrustStatus.UNTRUSTED
    )
  }
}
