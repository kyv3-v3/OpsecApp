package com.opsecapp.app.ui.detail

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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opsecapp.app.ui.components.OpsecBackground
import com.opsecapp.app.install.InstallResult
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
  stateFlow: kotlinx.coroutines.flow.StateFlow<DetailUiState>,
  events: Flow<InstallResult>,
  onBack: () -> Unit,
  onInstallClick: () -> Unit,
  onOpenLink: (String) -> Unit
) {
  val state by stateFlow.collectAsStateWithLifecycle()
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
    snackbarHost = { SnackbarHost(snackbar) },
    topBar = {
      TopAppBar(
        title = { Text("Details") },
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
    val item = state.item
    if (item == null) {
      OpsecBackground(modifier = Modifier.padding(padding)) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          Text("Item unavailable")
        }
      }
      return@Scaffold
    }

    OpsecBackground(modifier = Modifier.padding(padding)) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Card(
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(item.titleExact, style = MaterialTheme.typography.headlineSmall)
              AssistChip(
                onClick = {},
                label = { Text(item.badgeExact) },
                colors = AssistChipDefaults.assistChipColors(
                  containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
                )
              )
              Text(item.descriptionExact, style = MaterialTheme.typography.bodyLarge)
              Text(
                "Trust badge: Signed catalog verified",
                style = MaterialTheme.typography.labelLarge
              )
            }
          }
        }

        item {
          Button(onClick = onInstallClick, modifier = Modifier.fillMaxWidth()) {
            Text("Install / Open Upstream")
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
                "Install constraints",
                style = MaterialTheme.typography.titleMedium
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                "Android consumer devices do not permit silent installs/updates for regular apps. " +
                  "The system Package Installer always requires user confirmation.",
                style = MaterialTheme.typography.bodyMedium
              )
            }
          }
        }

        item {
          Text("Source Links", style = MaterialTheme.typography.titleMedium)
        }

        items(item.upstreamLinks, key = { it.url }) { link ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenLink(link.url) },
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
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
                  text = "No hash/cert metadata available. Verify upstream integrity manually.",
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
