package com.opsecapp.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opsecapp.app.ui.components.OpsecBackground
import com.opsecapp.app.install.InstallResult
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

  LaunchedEffect(Unit) {
    events.collect { event ->
      when (event) {
        InstallResult.FdroidLaunched -> snackbar.showSnackbar("Opened F-Droid.")
        InstallResult.FdroidMissing -> snackbar.showSnackbar("F-Droid is not installed.")
        InstallResult.InstallerLaunched -> snackbar.showSnackbar("Opened package installer.")
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
        title = { Text("Settings") },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
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
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
          )
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = editableUrl,
              onValueChange = { editableUrl = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Catalog base URL") },
              supportingText = { Text("HTTPS endpoint containing catalog.json and catalog.sig") }
            )

            Button(onClick = { onCatalogUrlSave(editableUrl) }) {
              Text("Save URL")
            }
          }
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
          )
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onSyncNow, enabled = !state.isSyncingCatalog, modifier = Modifier.fillMaxWidth()) {
              Text(if (state.isSyncingCatalog) "Syncing catalog..." else "Update catalog")
            }

            Button(
              onClick = onCheckAppUpdates,
              enabled = !state.isCheckingAppUpdate,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(if (state.isCheckingAppUpdate) "Checking app updates..." else "Check app updates")
            }

            state.availableAppUpdate?.let { update ->
              Text(
                "Latest app release: ${update.tagName}" +
                  (update.publishedAt?.let { " ($it)" } ?: ""),
                style = MaterialTheme.typography.bodyMedium
              )
              Button(onClick = onInstallAppUpdate, enabled = !update.apkUrl.isNullOrBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Install app update")
              }
              TextButton(onClick = { onOpenReleasePage(update.releasePageUrl) }) {
                Text("Open release page")
              }
            }

            state.statusMessage?.let {
              Text(it, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
          )
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Trust status: ${state.trustStatus}", style = MaterialTheme.typography.titleMedium)
            Text(
              "Last synced from opsecguide.vip: ${state.meta?.lastSyncedFromSourceAt ?: "Never"}",
              style = MaterialTheme.typography.bodyMedium
            )
            Text(
              "Schema: ${state.meta?.schemaVersion ?: "-"}",
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }
      }
    }
  }
}
