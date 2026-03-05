package com.opsecapp.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opsecapp.app.install.InstallManager
import com.opsecapp.app.install.InstallResult
import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.model.UpstreamLink
import com.opsecapp.domain.model.UpstreamLinkType
import com.opsecapp.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
  val itemId: String = "",
  val item: CatalogItem? = null,
  val previewTitle: String = "",
  val previewBadge: String = "",
  val githubRepoUrl: String? = null,
  val githubReleasesUrl: String? = null
)

class DetailViewModel(
  private val catalogRepository: CatalogRepository,
  private val installManager: InstallManager
) : ViewModel() {
  private data class DetailSelection(
    val id: String = "",
    val previewTitle: String? = null,
    val previewBadge: String? = null
  )

  private val selection = MutableStateFlow(DetailSelection())
  private val _events = MutableSharedFlow<InstallResult>()
  val events = _events.asSharedFlow()

  val state: StateFlow<DetailUiState> = selection
    .flatMapLatest { selected ->
      if (selected.id.isBlank()) {
        flowOf(DetailUiState())
      } else {
        catalogRepository.observeItem(selected.id).map { item ->
          DetailUiState(
            itemId = selected.id,
            item = item,
            previewTitle = selected.previewTitle.orEmpty(),
            previewBadge = selected.previewBadge.orEmpty(),
            githubRepoUrl = item?.let(::resolveGithubRepoUrl),
            githubReleasesUrl = item?.let(::resolveGithubReleasesUrl)
          )
        }
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

  fun load(id: String, previewTitle: String? = null, previewBadge: String? = null) {
    selection.value = DetailSelection(
      id = id,
      previewTitle = previewTitle,
      previewBadge = previewBadge
    )
  }

  fun installPreferred() {
    val item = state.value.item ?: return

    viewModelScope.launch {
      val fdroidFirst = item.upstreamLinks.firstOrNull { it.type == UpstreamLinkType.FDROID }
      if (fdroidFirst != null || !item.packageId.isNullOrBlank()) {
        _events.emit(installManager.launchFdroid(item))
        return@launch
      }

      val apkLink = item.upstreamLinks.firstOrNull { it.isDirectApkLink() }

      if (apkLink != null) {
        _events.emit(installManager.downloadVerifyAndInstall(apkLink))
        return@launch
      }

      val githubReleasesUrl = resolveGithubReleasesUrl(item)
      if (githubReleasesUrl != null) {
        _events.emit(
          installManager.openUpstreamUrl(
            url = githubReleasesUrl,
            message = MESSAGE_OPEN_GITHUB_RELEASES
          )
        )
        return@launch
      }

      val upstreamUrl = item.upstreamLinks.firstOrNull()?.url
      if (!upstreamUrl.isNullOrBlank()) {
        _events.emit(
          installManager.openUpstreamUrl(
            url = upstreamUrl,
            message = MESSAGE_OPEN_UPSTREAM_FALLBACK
          )
        )
      } else {
        _events.emit(InstallResult.Error(ERROR_NO_UPSTREAM))
      }
    }
  }

  fun openUpstreamLink(url: String) = emitOpenUrl(url = url, message = MESSAGE_OPEN_UPSTREAM)

  fun openGithubRepository() {
    val repoUrl = state.value.githubRepoUrl
    if (repoUrl.isNullOrBlank()) {
      emitError(ERROR_NO_GITHUB_REPO)
      return
    }
    emitOpenUrl(url = repoUrl, message = MESSAGE_OPEN_GITHUB_REPO)
  }

  fun openGithubReleases() {
    val releasesUrl = state.value.githubReleasesUrl
    if (releasesUrl.isNullOrBlank()) {
      emitError(ERROR_NO_GITHUB_RELEASES)
      return
    }
    emitOpenUrl(url = releasesUrl, message = MESSAGE_OPEN_GITHUB_RELEASES_PAGE)
  }

  fun promptInstallFdroid() {
    val intent = installManager.promptInstallFdroidIntent()
    _events.tryEmit(InstallResult.NeedsUserAction(MESSAGE_PROMPT_INSTALL_FDROID, intent))
  }

  private fun emitOpenUrl(url: String, message: String) {
    viewModelScope.launch {
      _events.emit(installManager.openUpstreamUrl(url = url, message = message))
    }
  }

  private fun emitError(message: String) {
    viewModelScope.launch {
      _events.emit(InstallResult.Error(message))
    }
  }

  private fun UpstreamLink.isDirectApkLink(): Boolean {
    val normalized = url.substringBefore('?').substringBefore('#')
    return normalized.endsWith(".apk", ignoreCase = true) ||
      (type == UpstreamLinkType.GITHUB && url.contains("/releases/download/", ignoreCase = true))
  }

  private fun resolveGithubReleasesUrl(item: CatalogItem): String? {
    val repoPath = resolveGithubRepoPath(item) ?: return null
    return "https://github.com/$repoPath/releases/latest"
  }

  private fun resolveGithubRepoUrl(item: CatalogItem): String? {
    val repoPath = resolveGithubRepoPath(item) ?: return null
    return "https://github.com/$repoPath"
  }

  private fun resolveGithubRepoPath(item: CatalogItem): String? {
    return item.githubRepo?.toGithubRepoPath()
      ?: item.upstreamLinks.asSequence()
        .filter { it.type == UpstreamLinkType.GITHUB }
        .mapNotNull { it.url.toGithubRepoPath() }
        .firstOrNull()
  }

  private fun String.toGithubRepoPath(): String? {
    val trimmed = this.trim()
    SIMPLE_GITHUB_REPO_REGEX.matchEntire(trimmed)?.let { simple ->
      val owner = simple.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
      val repo = simple.groupValues.getOrNull(2)
        ?.removeSuffix(".git")
        ?.takeIf { it.isNotBlank() }
        ?: return null
      return "$owner/$repo"
    }

    val match = GITHUB_REPO_REGEX.find(trimmed) ?: return null
    val owner = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
    val repo = match.groupValues.getOrNull(2)
      ?.removeSuffix(".git")
      ?.takeIf { it.isNotBlank() }
      ?: return null
    return "$owner/$repo"
  }

  companion object {
    private val GITHUB_REPO_REGEX =
      Regex("""(?:https?://)?(?:www\.)?github\.com/([^/\s]+)/([^/\s?#]+)""", RegexOption.IGNORE_CASE)
    private val SIMPLE_GITHUB_REPO_REGEX =
      Regex("""^([^/\s]+)/([^/\s]+)$""")

    private const val MESSAGE_OPEN_GITHUB_RELEASES =
      "No direct APK found in catalog. Opening GitHub Releases."
    private const val MESSAGE_OPEN_UPSTREAM_FALLBACK =
      "No direct APK/F-Droid link in catalog. Opening upstream source."
    private const val MESSAGE_OPEN_UPSTREAM =
      "Opening upstream source."
    private const val MESSAGE_OPEN_GITHUB_REPO =
      "Opening GitHub repository."
    private const val MESSAGE_OPEN_GITHUB_RELEASES_PAGE =
      "Opening latest GitHub releases page."
    private const val MESSAGE_PROMPT_INSTALL_FDROID =
      "Install F-Droid from the official website."

    private const val ERROR_NO_UPSTREAM =
      "No upstream link available for this item."
    private const val ERROR_NO_GITHUB_REPO =
      "GitHub repository link unavailable for this item."
    private const val ERROR_NO_GITHUB_RELEASES =
      "GitHub releases link unavailable for this item."
  }
}
