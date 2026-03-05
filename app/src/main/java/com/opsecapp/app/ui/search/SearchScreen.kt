package com.opsecapp.app.ui.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opsecapp.app.R
import com.opsecapp.app.ui.components.GlitchText
import com.opsecapp.app.ui.components.MatrixPulseText
import com.opsecapp.app.ui.components.OpsecBackground
import com.opsecapp.app.ui.components.StaggeredReveal
import com.opsecapp.domain.model.CatalogItem
import com.opsecapp.domain.model.InstallType
import com.opsecapp.domain.model.SourceConfidence
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  stateFlow: kotlinx.coroutines.flow.StateFlow<SearchUiState>,
  onBack: () -> Unit,
  onQueryChange: (String) -> Unit,
  onSourceConfidenceChange: (SourceConfidence?) -> Unit,
  onInstallTypeChange: (InstallType?) -> Unit,
  onItemClick: (CatalogItem) -> Unit
) {
  val state by stateFlow.collectAsStateWithLifecycle()
  var turboMode by remember { mutableStateOf(false) }

  fun injectSignalJumble() {
    val payload = listOf("obscura", "mirror", "shadow", "relay", "cipher", "stealth")
      .shuffled()
      .take(2)
      .joinToString("-")
    onQueryChange(payload)
    turboMode = true
  }

  Scaffold(
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    topBar = {
      TopAppBar(
        title = { GlitchText("SEARCH // PACKET HUNTER", style = MaterialTheme.typography.titleMedium) },
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
        },
        actions = {
          IconButton(onClick = ::injectSignalJumble) {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = "Inject signal"
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
          .padding(16.dp)
      ) {
        OutlinedTextField(
          value = state.query,
          onValueChange = onQueryChange,
          label = { Text(if (turboMode) "Signal pattern" else stringResource(R.string.search_query_label)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))
        MatrixPulseText(
          text = "LIVE RESULTS ${state.results.size.toString().padStart(2, '0')} // RNG ${Random.nextInt(100, 999)}"
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(stringResource(R.string.search_source_confidence), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            AssistChip(
              onClick = { onSourceConfidenceChange(null) },
              label = { Text(stringResource(R.string.search_confidence_any)) },
              colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
              )
            )
          }
          item {
            AssistChip(
              onClick = { onSourceConfidenceChange(SourceConfidence.HIGH) },
              label = { Text(stringResource(R.string.search_confidence_high)) }
            )
          }
          item {
            AssistChip(
              onClick = { onSourceConfidenceChange(SourceConfidence.MEDIUM) },
              label = { Text(stringResource(R.string.search_confidence_medium)) }
            )
          }
          item {
            AssistChip(
              onClick = { onSourceConfidenceChange(SourceConfidence.LOW) },
              label = { Text(stringResource(R.string.search_confidence_low)) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(stringResource(R.string.search_install_type), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            AssistChip(
              onClick = { onInstallTypeChange(null) },
              label = { Text(stringResource(R.string.search_install_any)) },
              colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
              )
            )
          }
          item {
            AssistChip(
              onClick = { onInstallTypeChange(InstallType.FDROID) },
              label = { Text(stringResource(R.string.search_install_fdroid)) }
            )
          }
          item {
            AssistChip(
              onClick = { onInstallTypeChange(InstallType.GITHUB_APK) },
              label = { Text(stringResource(R.string.search_install_github_apk)) }
            )
          }
          item {
            AssistChip(
              onClick = { onInstallTypeChange(InstallType.OFFICIAL_APK) },
              label = { Text(stringResource(R.string.search_install_official_apk)) }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 32.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          itemsIndexed(state.results, key = { _, item -> item.id }) { index, item ->
            StaggeredReveal(index = index) {
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onItemClick(item) },
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.titleExact.uppercase(), style = MaterialTheme.typography.titleMedium)
                    Text(item.badgeExact.uppercase(), style = MaterialTheme.typography.labelSmall)
                  }
                  Text(item.descriptionExact, style = MaterialTheme.typography.bodyMedium)
                  Text(
                    text = stringResource(R.string.search_item_meta, item.sourceConfidence, item.installType),
                    style = MaterialTheme.typography.labelSmall
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
