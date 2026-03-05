package com.opsecapp.app.ui.detail

import com.google.common.truth.Truth.assertThat
import com.opsecapp.app.install.InstallManager
import com.opsecapp.app.install.InstallResult
import com.opsecapp.app.test.MainDispatcherRule
import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.model.InstallType
import com.opsecapp.domain.model.SourceConfidence
import com.opsecapp.domain.model.UpstreamLink
import com.opsecapp.domain.model.UpstreamLinkType
import com.opsecapp.domain.repository.CatalogRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun installPreferred_uses_fdroid_when_available() = runTest {
    val item = catalogItem(
      links = listOf(
        UpstreamLink(
          type = UpstreamLinkType.FDROID,
          url = "https://f-droid.org/packages/com.example.app",
          labelExact = "F-Droid"
        )
      )
    )

    val catalogRepository = mockk<CatalogRepository>()
    every { catalogRepository.observeItem("item-1") } returns flowOf(item)

    val installManager = mockk<InstallManager>()
    every { installManager.launchFdroid(item) } returns InstallResult.FdroidLaunched

    val viewModel = DetailViewModel(catalogRepository, installManager)
    val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.state.collect { }
    }

    viewModel.load("item-1")
    advanceUntilIdle()
    viewModel.installPreferred()
    advanceUntilIdle()

    verify(exactly = 1) { installManager.launchFdroid(item) }
    coVerify(exactly = 0) { installManager.downloadVerifyAndInstall(any()) }

    collector.cancel()
  }

  @Test
  fun installPreferred_falls_back_to_github_releases_when_no_direct_apk() = runTest {
    val item = catalogItem(
      githubRepo = "acme/sample-app",
      links = listOf(
        UpstreamLink(
          type = UpstreamLinkType.GITHUB,
          url = "https://github.com/acme/sample-app",
          labelExact = "GitHub"
        )
      )
    )

    val catalogRepository = mockk<CatalogRepository>()
    every { catalogRepository.observeItem("item-2") } returns flowOf(item)

    val installManager = mockk<InstallManager>()
    every {
      installManager.openUpstreamUrl(
        "https://github.com/acme/sample-app/releases/latest",
        any()
      )
    } returns InstallResult.NeedsUserAction("Open", null)

    val viewModel = DetailViewModel(catalogRepository, installManager)
    val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.state.collect { }
    }

    viewModel.load("item-2")
    advanceUntilIdle()
    viewModel.installPreferred()
    advanceUntilIdle()

    verify(exactly = 1) {
      installManager.openUpstreamUrl("https://github.com/acme/sample-app/releases/latest", any())
    }

    collector.cancel()
  }

  @Test
  fun openGithubRepository_emits_action_when_repo_exists() = runTest {
    val item = catalogItem(githubRepo = "acme/sample-app")

    val catalogRepository = mockk<CatalogRepository>()
    every { catalogRepository.observeItem("item-3") } returns flowOf(item)

    val installManager = mockk<InstallManager>()
    every {
      installManager.openUpstreamUrl(
        "https://github.com/acme/sample-app",
        any()
      )
    } returns InstallResult.NeedsUserAction("Open", null)

    val viewModel = DetailViewModel(catalogRepository, installManager)
    val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.state.collect { }
    }

    viewModel.load("item-3")
    advanceUntilIdle()
    viewModel.openGithubRepository()
    advanceUntilIdle()

    verify(exactly = 1) { installManager.openUpstreamUrl("https://github.com/acme/sample-app", any()) }
    assertThat(viewModel.state.value.githubRepoUrl).isEqualTo("https://github.com/acme/sample-app")

    collector.cancel()
  }

  private fun catalogItem(
    githubRepo: String? = null,
    links: List<UpstreamLink> = emptyList()
  ): CatalogItem {
    return CatalogItem(
      id = "item",
      titleExact = "Sample",
      descriptionExact = "Description",
      badgeExact = "Badge",
      sectionExact = "Section",
      categoryIds = listOf("cat"),
      upstreamLinks = links,
      packageId = null,
      githubRepo = githubRepo,
      sourceConfidence = SourceConfidence.HIGH,
      installType = InstallType.GITHUB_APK
    )
  }
}
