package com.opsecapp.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opsecapp.app.ui.components.OpsecBackground
import com.opsecapp.domain.model.CatalogItem

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  stateFlow: kotlinx.coroutines.flow.StateFlow<HomeUiState>,
  onRefresh: () -> Unit,
  onCategoryClick: (String) -> Unit,
  onItemClick: (String) -> Unit,
  onSearchClick: () -> Unit,
  onSettingsClick: () -> Unit
) {
  val state by stateFlow.collectAsStateWithLifecycle()
  val pullRefreshState = rememberPullRefreshState(
    refreshing = state.isRefreshing,
    onRefresh = onRefresh
  )

  Scaffold(
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    topBar = {
      TopAppBar(
        title = { Text("Opsec Catalog", fontWeight = FontWeight.SemiBold) },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
          titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        actions = {
          IconButton(onClick = onSearchClick) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search"
            )
          }
          IconButton(onClick = onSettingsClick) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings"
            )
          }
        }
      )
    }
  ) { padding ->
    OpsecBackground(
      modifier = Modifier
        .padding(padding)
        .pullRefresh(pullRefreshState)
    ) {
      val homeState = state.home
      if (homeState == null) {
        HomeSkeleton(modifier = Modifier.fillMaxSize())
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
              )
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                  text = "Last synced from opsecguide.vip on ${homeState.lastSyncedText}",
                  style = MaterialTheme.typography.labelLarge
                )
                TrustBadge(homeState.trustStatus.name)
              }
            }
          }

          item {
            Text(
              text = "Categories",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.SemiBold
            )
          }

          items(homeState.categories, key = { it.id }) { category ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onCategoryClick(category.id) },
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
              )
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(category.titleExact, style = MaterialTheme.typography.titleMedium)
                  AssistChip(
                    onClick = { onCategoryClick(category.id) },
                    label = { Text("Browse") },
                    colors = AssistChipDefaults.assistChipColors(
                      containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
                    )
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(category.descriptionExact, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }

          item {
            Text(
              text = "Recommended Highlights",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.SemiBold
            )
          }

          items(homeState.highlights, key = { it.id }) { item ->
            ItemHighlight(item = item, onClick = { onItemClick(item.id) })
          }
        }
      }

      PullRefreshIndicator(
        refreshing = state.isRefreshing,
        state = pullRefreshState,
        modifier = Modifier.align(Alignment.TopCenter)
      )
    }
  }
}

@Composable
private fun ItemHighlight(
  item: CatalogItem,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    )
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(item.titleExact, style = MaterialTheme.typography.titleMedium)
        AssistChip(
          onClick = {},
          label = { Text(item.badgeExact) },
          colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f)
          )
        )
      }
      Text(item.descriptionExact, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
private fun TrustBadge(text: String) {
  AssistChip(
    onClick = {},
    label = { Text("Trust: $text") },
    colors = AssistChipDefaults.assistChipColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
    )
  )
}

@Composable
private fun HomeSkeleton(modifier: Modifier = Modifier) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(5) {
      AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          )
        ) {
          Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(16.dp))
        }
      }
    }
  }
}
