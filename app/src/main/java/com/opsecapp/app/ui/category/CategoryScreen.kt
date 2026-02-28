package com.opsecapp.app.ui.category

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
  stateFlow: kotlinx.coroutines.flow.StateFlow<CategoryUiState>,
  onBack: () -> Unit,
  onItemClick: (String) -> Unit
) {
  val state by stateFlow.collectAsStateWithLifecycle()

  Scaffold(
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    topBar = {
      TopAppBar(
        title = { Text(text = state.categoryId.ifBlank { "Category" }) },
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
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(state.items, key = { it.id }) { item ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onItemClick(item.id) },
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(item.titleExact, style = MaterialTheme.typography.titleMedium)
              Spacer(modifier = Modifier.height(4.dp))
              Text(item.descriptionExact, style = MaterialTheme.typography.bodyMedium)
              Spacer(modifier = Modifier.height(10.dp))
              AssistChip(
                onClick = {},
                label = { Text(item.badgeExact) },
                colors = AssistChipDefaults.assistChipColors(
                  containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                )
              )
            }
          }
        }
      }
    }
  }
}
