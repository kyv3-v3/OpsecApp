package com.opsecapp.app.ui.search

import com.google.common.truth.Truth.assertThat
import com.opsecapp.app.test.MainDispatcherRule
import com.opsecapp.domain.model.CatalogCategory
import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.model.CatalogMeta
import com.opsecapp.domain.model.InstallType
import com.opsecapp.domain.model.SourceConfidence
import com.opsecapp.domain.model.TrustStatus
import com.opsecapp.domain.model.UpstreamLink
import com.opsecapp.domain.model.UpstreamLinkType
import com.opsecapp.domain.repository.CatalogRepository
import com.opsecapp.domain.usecase.SearchCatalogUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun updates_state_with_query_and_filters() = runTest {
    val fakeRepository = object : CatalogRepository {
      override fun observeCategories(): Flow<List<CatalogCategory>> = flowOf(emptyList())
      override fun observeHighlights(limit: Int): Flow<List<CatalogItem>> = flowOf(emptyList())
      override fun observeCategoryItems(categoryId: String): Flow<List<CatalogItem>> = flowOf(emptyList())
      override fun observeItem(itemId: String): Flow<CatalogItem?> = flowOf(null)

      override fun search(
        query: String,
        confidence: SourceConfidence?,
        installType: InstallType?
      ): Flow<List<CatalogItem>> {
        return flowOf(
          listOf(
            CatalogItem(
              id = "result",
              titleExact = "result-$query",
              descriptionExact = "desc",
              badgeExact = "badge",
              sectionExact = "section",
              categoryIds = listOf("c1"),
              upstreamLinks = listOf(
                UpstreamLink(
                  type = UpstreamLinkType.GITHUB,
                  url = "https://github.com/example/repo",
                  labelExact = "GitHub"
                )
              ),
              sourceConfidence = confidence ?: SourceConfidence.HIGH,
              installType = installType ?: InstallType.FDROID
            )
          )
        )
      }

      override fun observeMeta(): Flow<CatalogMeta?> = flowOf(null)
      override suspend fun syncCatalog(force: Boolean): TrustStatus = TrustStatus.TRUSTED
    }

    val viewModel = SearchViewModel(SearchCatalogUseCase(fakeRepository))
    val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.state.collect { }
    }

    viewModel.onQueryChange("signal")
    viewModel.onSourceConfidenceChange(SourceConfidence.MEDIUM)
    viewModel.onInstallTypeChange(InstallType.GITHUB_APK)

    advanceTimeBy(250)
    advanceUntilIdle()

    val state = viewModel.state.value
    assertThat(state.query).isEqualTo("signal")
    assertThat(state.sourceConfidence).isEqualTo(SourceConfidence.MEDIUM)
    assertThat(state.installType).isEqualTo(InstallType.GITHUB_APK)
    assertThat(state.results).hasSize(1)
    assertThat(state.results.first().titleExact).isEqualTo("result-signal")

    collector.cancel()
  }
}
