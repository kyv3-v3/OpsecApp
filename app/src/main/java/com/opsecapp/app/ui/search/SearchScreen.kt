package com.opsecapp.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opsecapp.app.ui.components.OpsecBackground
import com.opsecapp.domain.model.InstallType
import com.opsecapp.domain.model.SourceConfidence

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  stateFlow: kotlinx.coroutines.flow.StateFlow<SearchUiState>,
  onBack: () -> Unit,
  onQueryChange: (String) -> Unit,
  onSourceConfidenceChange: (SourceConfidence?) -> Unit,
  onInstallTypeChange: (InstallType?) -> Unit,
  onItemClick: (String) -> Unit
) {
  val state by stateFlow.collectAsStateWithLifecycle()

  Scaffold(
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    topBar = {
      TopAppBar(
        title = { Text("Search") },
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
          .padding(16.dp)
      ) {
        OutlinedTextField(
          value = state.query,
          onValueChange = onQueryChange,
          label = { Text("Search exact title/description") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text("Source confidence", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            AssistChip(
              onClick = { onSourceConfidenceChange(null) },
              label = { Text("Any confidence") },
              colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
              )
            )
          }
          item {
            AssistChip(
              onClick = { onSourceConfidenceChange(SourceConfidence.HIGH) },
              label = { Text("High") }
            )
          }
          item {
            AssistChip(
              onClick = { onSourceConfidenceChange(SourceConfidence.MEDIUM) },
              label = { Text("Medium") }
            )
          }
          item {
            AssistChip(
              onClick = { onSourceConfidenceChange(SourceConfidence.LOW) },
              label = { Text("Low") }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Install type", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            AssistChip(
              onClick = { onInstallTypeChange(null) },
              label = { Text("Any install") },
              colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
              )
            )
          }
          item {
            AssistChip(onClick = { onInstallTypeChange(InstallType.FDROID) }, label = { Text("F-Droid") })
          }
          item {
            AssistChip(onClick = { onInstallTypeChange(InstallType.GITHUB_APK) }, label = { Text("GitHub APK") })
          }
          item {
            AssistChip(onClick = { onInstallTypeChange(InstallType.OFFICIAL_APK) }, label = { Text("Official APK") })
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 32.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(state.results, key = { it.id }) { item ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick(item.id) },
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
              )
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Text(item.titleExact, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.descriptionExact, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "${item.sourceConfidence} • ${item.installType}",
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
