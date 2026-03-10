package com.opsecapp.app.ui.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opsecapp.app.R
import com.opsecapp.app.install.InstallResult
import com.opsecapp.app.ui.components.GlitchText
import com.opsecapp.app.ui.components.MatrixPulseText
import com.opsecapp.app.ui.components.NeonPanel
import com.opsecapp.app.ui.components.OpsecBackground
import com.opsecapp.app.ui.components.ShimmerPlaceholder
import com.opsecapp.app.ui.components.StaggeredReveal
import com.opsecapp.app.ui.components.ThreatMeter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
  stateFlow: kotlinx.coroutines.flow.StateFlow<DetailUiState>,
  events: Flow<InstallResult>,
  onBack: () -> Unit,
  onInstallClick: () -> Unit,
  onInstallDirect: () -> Unit,
  onOpenLink: (String) -> Unit,
  onOpenGithubRepo: () -> Unit,
  onOpenGithubReleases: () -> Unit
) {
  val state by stateFlow.collectAsStateWithLifecycle()
  val snackbar = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val clipboardManager = LocalClipboardManager.current

  val title = state.item?.titleExact
    ?: state.previewTitle.ifBlank { stringResource(R.string.detail_title) }

  val openedFdroid = stringResource(R.string.snackbar_opened_fdroid)
  val missingFdroid = stringResource(R.string.snackbar_fdroid_missing)
  val openedInstaller = stringResource(R.string.snackbar_opened_installer)
  var anomalyMode by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    events.collect { event ->
      when (event) {
        InstallResult.FdroidLaunched -> snackbar.showSnackbar(openedFdroid)
        InstallResult.FdroidMissing -> snackbar.showSnackbar(missingFdroid)
        InstallResult.InstallerLaunched -> snackbar.showSnackbar(openedInstaller)
        is InstallResult.Warning -> snackbar.showSnackbar(event.message)
        is InstallResult.Error -> snackbar.showSnackbar(event.message)
        is InstallResult.NeedsUserAction -> snackbar.showSnackbar(event.message)
      }
    }
  }

  Scaffold(
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    snackbarHost = { SnackbarHost(snackbar) },
    topBar = {
      TopAppBar(
        title = {
          AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "detail_toolbar_title"
          ) { animatedTitle ->
            GlitchText(animatedTitle.uppercase(), style = MaterialTheme.typography.titleMedium)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        ),
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.common_back)
            )
          }
        }
      )
    }
  ) { padding ->
    val selectedItem = state.item
    if (selectedItem == null) {
      OpsecBackground(modifier = Modifier.padding(padding)) {
        DetailSkeleton(
          previewTitle = state.previewTitle,
          previewBadge = state.previewBadge,
          modifier = Modifier.fillMaxSize()
        )
      }
      return@Scaffold
    }

    var revealStarted by remember(state.itemId) { mutableStateOf(false) }
    LaunchedEffect(state.itemId) {
      revealStarted = true
    }

    val revealProgress by animateFloatAsState(
      targetValue = if (revealStarted) 1f else 0.94f,
      animationSpec = tween(durationMillis = 280),
      label = "detail_reveal_progress"
    )

    val firstHash = selectedItem.upstreamLinks.firstNotNullOfOrNull { it.expectedSha256 }
    val firstSigner = selectedItem.upstreamLinks.firstNotNullOfOrNull { it.expectedSignerSha256 }

    val copiedPackageId = stringResource(R.string.detail_copied_package_id)
    val copiedSha = stringResource(R.string.detail_copied_sha256)
    val copiedSigner = stringResource(R.string.detail_copied_signer_sha256)

    OpsecBackground(modifier = Modifier.padding(padding)) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Card(
            modifier = Modifier.graphicsLayer {
              scaleX = revealProgress
              scaleY = revealProgress
              alpha = revealProgress
            },
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              AnimatedContent(
                targetState = selectedItem.titleExact,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "detail_title"
              ) { animatedTitle ->
                Text(animatedTitle, style = MaterialTheme.typography.headlineSmall)
              }

              AnimatedContent(
                targetState = selectedItem.badgeExact,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "detail_badge"
              ) { badge ->
                AssistChip(
                  onClick = {},
                  label = { Text(badge) },
                  colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
                  )
                )
              }

              Text(selectedItem.descriptionExact, style = MaterialTheme.typography.bodyLarge)
              Text(
                stringResource(R.string.detail_trust_verified),
                style = MaterialTheme.typography.labelLarge
              )
            }
          }
        }

        item {
          NeonPanel(modifier = Modifier.fillMaxWidth()) {
            MatrixPulseText(
              text = "TRACE ${selectedItem.upstreamLinks.size} UPSTREAM NODES // MODE ${if (anomalyMode) "ANOMALY" else "STABLE"}"
            )
            ThreatMeter(
              value = (selectedItem.upstreamLinks.size / 6f).coerceIn(0.12f, 1f)
            )
            Button(
              onClick = { anomalyMode = !anomalyMode },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(if (anomalyMode) "DISABLE ANOMALY OVERLAY" else "ENABLE ANOMALY OVERLAY")
            }
          }
        }

        item {
          if (selectedItem.hasDirectApkLink) {
            Button(onClick = onInstallDirect, modifier = Modifier.fillMaxWidth()) {
              Text(stringResource(R.string.detail_install_direct))
            }
            OutlinedButton(onClick = onInstallClick, modifier = Modifier.fillMaxWidth()) {
              Text(stringResource(R.string.detail_install_open_upstream))
            }
          } else {
            Button(onClick = onInstallClick, modifier = Modifier.fillMaxWidth()) {
              Text(stringResource(R.string.detail_install_open_upstream))
            }
          }
        }

        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                stringResource(R.string.detail_quick_actions),
                style = MaterialTheme.typography.titleMedium
              )

              selectedItem.packageId?.takeIf { it.isNotBlank() }?.let { packageId ->
                TextButton(onClick = {
                  clipboardManager.setText(AnnotatedString(packageId))
                  scope.launch { snackbar.showSnackbar(copiedPackageId) }
                }) {
                  Text(stringResource(R.string.detail_copy_package_id))
                }
              }

              firstHash?.let { hash ->
                TextButton(onClick = {
                  clipboardManager.setText(AnnotatedString(hash))
                  scope.launch { snackbar.showSnackbar(copiedSha) }
                }) {
                  Text(stringResource(R.string.detail_copy_sha256))
                }
              }

              firstSigner?.let { signer ->
                TextButton(onClick = {
                  clipboardManager.setText(AnnotatedString(signer))
                  scope.launch { snackbar.showSnackbar(copiedSigner) }
                }) {
                  Text(stringResource(R.string.detail_copy_signer_sha256))
                }
              }

              state.githubRepoUrl?.let {
                TextButton(onClick = onOpenGithubRepo) {
                  Text(stringResource(R.string.detail_open_github_repo))
                }
              }

              state.githubReleasesUrl?.let {
                TextButton(onClick = onOpenGithubReleases) {
                  Text(stringResource(R.string.detail_open_latest_release))
                }
              }
            }
          }
        }

        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                stringResource(R.string.detail_install_constraints),
                style = MaterialTheme.typography.titleMedium
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                stringResource(R.string.detail_install_constraints_body),
                style = MaterialTheme.typography.bodyMedium
              )
            }
          }
        }

        item {
          Text(stringResource(R.string.detail_source_links), style = MaterialTheme.typography.titleMedium)
        }

        itemsIndexed(selectedItem.upstreamLinks, key = { _, link -> link.url }) { index, link ->
          StaggeredReveal(index = index) {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenLink(link.url) },
              colors = CardDefaults.cardColors(
                containerColor = if (anomalyMode) {
                  MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                } else {
                  MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                }
              )
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(link.labelExact, style = MaterialTheme.typography.titleSmall)
                  Text(link.type.name, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(link.url, style = MaterialTheme.typography.bodySmall)
                if (link.expectedSha256 == null && link.expectedSignerSha256 == null) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = stringResource(R.string.detail_link_missing_metadata),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DetailSkeleton(
  previewTitle: String,
  previewBadge: String,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          if (previewTitle.isBlank()) {
            ShimmerPlaceholder(
              modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(28.dp)
            )
          } else {
            Text(previewTitle, style = MaterialTheme.typography.headlineSmall)
          }

          if (previewBadge.isBlank()) {
            ShimmerPlaceholder(
              modifier = Modifier
                .fillMaxWidth(0.3f)
                .height(26.dp)
            )
          } else {
            AssistChip(onClick = {}, label = { Text(previewBadge) })
          }

          ShimmerPlaceholder(
            modifier = Modifier
              .fillMaxWidth()
              .height(76.dp)
          )
        }
      }
    }

    itemsIndexed(List(4) { it }) { index, _ ->
      StaggeredReveal(index = index) {
        ShimmerPlaceholder(
          modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
        )
      }
    }
  }
}
