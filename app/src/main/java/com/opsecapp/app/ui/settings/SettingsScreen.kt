package com.opsecapp.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opsecapp.app.R
import com.opsecapp.app.install.InstallResult
import com.opsecapp.app.ui.components.GlitchText
import com.opsecapp.app.ui.components.MatrixPulseText
import com.opsecapp.app.ui.components.NeonPanel
import com.opsecapp.app.ui.components.OpsecBackground
import com.opsecapp.app.ui.components.StatusTicker
import com.opsecapp.app.ui.components.ThreatMeter
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  stateFlow: kotlinx.coroutines.flow.StateFlow<SettingsUiState>,
  events: Flow<InstallResult>,
  onBack: () -> Unit,
  onCatalogUrlSave: (String) -> Unit,
  onSyncNow: () -> Unit,
  onCheckAppUpdates: () -> Unit,
  onInstallAppUpdate: () -> Unit,
  onOpenReleasePage: (String) -> Unit
) {
  val state by stateFlow.collectAsStateWithLifecycle()
  var editableUrl by remember(state.catalogBaseUrl) { mutableStateOf(state.catalogBaseUrl) }
  val snackbar = remember { SnackbarHostState() }
  var warpMode by remember { mutableStateOf(false) }

  val openedFdroid = stringResource(R.string.snackbar_opened_fdroid)
  val missingFdroid = stringResource(R.string.snackbar_fdroid_missing)
  val openedInstaller = stringResource(R.string.snackbar_opened_installer)

  LaunchedEffect(Unit) {
    events.collect { event ->
      when (event) {
        InstallResult.FdroidLaunched -> snackbar.showSnackbar(openedFdroid)
        InstallResult.FdroidMissing -> snackbar.showSnackbar(missingFdroid)
        InstallResult.InstallerLaunched -> snackbar.showSnackbar(openedInstaller)
        is InstallResult.InstallerLaunchedWithWarning -> {
          snackbar.showSnackbar(openedInstaller)
          snackbar.showSnackbar(event.message)
        }
        is InstallResult.Warning -> snackbar.showSnackbar(event.message)
        is InstallResult.Error -> snackbar.showSnackbar(event.message)
        is InstallResult.NeedsUserAction -> snackbar.showSnackbar(event.message)
      }
    }
  }

  Scaffold(
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    snackbarHost = { SnackbarHost(hostState = snackbar) },
    topBar = {
      TopAppBar(
        title = { GlitchText("SETTINGS // CONTROL ROOM", style = MaterialTheme.typography.titleMedium) },
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
    OpsecBackground(modifier = Modifier.padding(padding)) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        NeonPanel(modifier = Modifier.fillMaxWidth()) {
          StatusTicker(
            leftLabel = "CATALOG CHANNEL",
            rightLabel = if (warpMode) "WARP MODE" else "STABLE MODE"
          )
          ThreatMeter(
            value = when (state.trustStatus.name) {
              "TRUSTED" -> 0.18f
              "NETWORK_ERROR" -> 0.64f
              "INVALID_SIGNATURE", "INVALID_FINGERPRINT" -> 0.9f
              else -> 0.52f
            }
          )
          OutlinedTextField(
            value = editableUrl,
            onValueChange = { editableUrl = it },
            isError = state.catalogUrlMessageIsError,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_catalog_url_label)) },
            supportingText = {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.settings_catalog_url_support))
                state.catalogUrlMessage?.let { message ->
                  Text(
                    text = message,
                    color = if (state.catalogUrlMessageIsError) {
                      MaterialTheme.colorScheme.error
                    } else {
                      MaterialTheme.colorScheme.primary
                    }
                  )
                }
              }
            }
          )

          Button(onClick = { onCatalogUrlSave(editableUrl) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_save_url))
          }

          Button(
            onClick = { warpMode = !warpMode },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(if (warpMode) "DISABLE WARP OVERLAY" else "ENABLE WARP OVERLAY")
          }
        }

        NeonPanel(modifier = Modifier.fillMaxWidth()) {
          Button(onClick = onSyncNow, enabled = !state.isSyncingCatalog, modifier = Modifier.fillMaxWidth()) {
            Text(
              if (state.isSyncingCatalog) {
                stringResource(R.string.settings_syncing_catalog)
              } else {
                stringResource(R.string.settings_update_catalog)
              }
            )
          }

          Button(
            onClick = onCheckAppUpdates,
            enabled = !state.isCheckingAppUpdate,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              if (state.isCheckingAppUpdate) {
                stringResource(R.string.settings_checking_updates)
              } else {
                stringResource(R.string.settings_check_updates)
              }
            )
          }

          state.availableAppUpdate?.let { update ->
            val hasApkAsset = !update.apkUrl.isNullOrBlank()
            val publishedSuffix = update.publishedAt?.let {
              stringResource(R.string.settings_release_published_suffix, it)
            }.orEmpty()

            MatrixPulseText(
              text = stringResource(R.string.settings_latest_release, update.tagName, publishedSuffix)
            )
            update.notes?.takeIf { it.isNotBlank() }?.let { notes ->
              MatrixPulseText(
                text = "${stringResource(R.string.settings_release_notes)} ${notes.take(220)}"
              )
            }
            if (!hasApkAsset) {
              Text(
                stringResource(R.string.settings_no_apk_asset),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
              )
            }
            Button(
              onClick = onInstallAppUpdate,
              enabled = hasApkAsset,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(stringResource(R.string.settings_install_app_update))
            }
            TextButton(onClick = { onOpenReleasePage(update.releasePageUrl) }) {
              Text(stringResource(R.string.settings_open_release_page))
            }
          }

          state.statusMessage?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
          }
        }

        NeonPanel(modifier = Modifier.fillMaxWidth()) {
          val lastSynced = state.meta?.lastSyncedFromSourceAt?.toString()
            ?: stringResource(R.string.common_never)
          val schemaVersion = state.meta?.schemaVersion ?: stringResource(R.string.common_not_available)

          Text(
            stringResource(R.string.settings_trust_status, state.trustStatus),
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            stringResource(R.string.settings_last_synced, lastSynced),
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            stringResource(R.string.settings_schema, schemaVersion),
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
    }
  }
}
