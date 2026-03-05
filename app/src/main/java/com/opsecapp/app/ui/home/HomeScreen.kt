package com.opsecapp.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opsecapp.app.R
import com.opsecapp.app.ui.components.DataGlyphCloud
import com.opsecapp.app.ui.components.GlitchText
import com.opsecapp.app.ui.components.MatrixPulseText
import com.opsecapp.app.ui.components.NeonPanel
import com.opsecapp.app.ui.components.OpsecBackground
import com.opsecapp.app.ui.components.ShimmerPlaceholder
import com.opsecapp.app.ui.components.StaggeredReveal
import com.opsecapp.app.ui.components.StatusTicker
import com.opsecapp.app.ui.components.ThreatMeter
import com.opsecapp.app.ui.components.threatLabel
import com.opsecapp.domain.model.CatalogItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  stateFlow: kotlinx.coroutines.flow.StateFlow<HomeUiState>,
  events: Flow<HomeRefreshEvent>,
  onRefresh: () -> Unit,
  onCategoryClick: (String) -> Unit,
  onItemClick: (CatalogItem) -> Unit,
  onSearchClick: () -> Unit,
  onSettingsClick: () -> Unit
) {
  val state by stateFlow.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val pullRefreshState = rememberPullRefreshState(
    refreshing = state.isRefreshing,
    onRefresh = onRefresh
  )

  var threatLevel by remember { mutableFloatStateOf(0.48f) }
  var chaosMode by remember { mutableStateOf(false) }
  val terminalLogs = remember { mutableStateListOf<String>() }
  var pulseSeed by remember { mutableStateOf(11) }

  val updatedMessage = stringResource(R.string.home_snackbar_catalog_updated)
  val invalidSignatureMessage = stringResource(R.string.home_snackbar_invalid_signature)
  val invalidFingerprintMessage = stringResource(R.string.home_snackbar_invalid_fingerprint)
  val networkErrorMessage = stringResource(R.string.home_snackbar_network_error)
  val parseErrorMessage = stringResource(R.string.home_snackbar_parse_error)
  val untrustedMessage = stringResource(R.string.home_snackbar_untrusted)
  val refreshFailedMessage = stringResource(R.string.home_snackbar_refresh_failed)

  fun appendTerminalLog(prefix: String = "PKT"): Unit {
    val token = Random.nextInt(1000, 9999)
    terminalLogs.add(0, "$prefix-${Random.nextInt(10, 99)} :: tunnel[$token] latency=${Random.nextInt(7, 142)}ms")
    while (terminalLogs.size > 7) {
      terminalLogs.removeLast()
    }
  }

  LaunchedEffect(chaosMode) {
    if (!chaosMode) return@LaunchedEffect
    while (chaosMode) {
      delay(1350)
      pulseSeed = Random.nextInt(1, 99)
      threatLevel = (threatLevel + Random.nextFloat() * 0.16f).coerceIn(0.14f, 0.98f)
      appendTerminalLog(prefix = "CHAOS")
    }
  }

  LaunchedEffect(Unit) {
    events.collect { event ->
      val message = when (event) {
        HomeRefreshEvent.CatalogUpdated -> updatedMessage
        HomeRefreshEvent.InvalidSignature -> invalidSignatureMessage
        HomeRefreshEvent.InvalidFingerprint -> invalidFingerprintMessage
        HomeRefreshEvent.NetworkError -> networkErrorMessage
        HomeRefreshEvent.ParseError -> parseErrorMessage
        HomeRefreshEvent.Untrusted -> untrustedMessage
        HomeRefreshEvent.SyncFailed -> refreshFailedMessage
      }
      snackbarHostState.showSnackbar(message)
    }
  }

  Scaffold(
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          GlitchText(
            text = "OPSEC // DARKNET CONSOLE",
            style = MaterialTheme.typography.titleLarge
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
          titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        actions = {
          IconButton(onClick = onRefresh) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh"
            )
          }
          IconButton(onClick = onSearchClick) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = stringResource(R.string.home_action_search)
            )
          }
          IconButton(onClick = onSettingsClick) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = stringResource(R.string.home_action_settings)
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
        val lastSynced = homeState.lastSyncedText.ifBlank { stringResource(R.string.common_never) }
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          item {
            NeonPanel(modifier = Modifier.fillMaxWidth()) {
              GlitchText(
                text = "LIVE NODE MATRIX",
                style = MaterialTheme.typography.titleMedium
              )
              StatusTicker(
                leftLabel = "SIGNATURE ${homeState.trustStatus.name}",
                rightLabel = "SYNC $lastSynced"
              )
              ThreatMeter(value = threatLevel, modifier = Modifier.padding(top = 2.dp))
              MatrixPulseText(text = threatLabel(threatLevel))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = {
                    appendTerminalLog(prefix = "SCAN")
                    threatLevel = (threatLevel + 0.07f).coerceAtMost(1f)
                  },
                  modifier = Modifier.weight(1f)
                ) {
                  Text("PACKET STORM")
                }
                Button(
                  onClick = {
                    chaosMode = !chaosMode
                    appendTerminalLog(prefix = if (chaosMode) "MODE" else "SAFE")
                  },
                  modifier = Modifier.weight(1f)
                ) {
                  Text(if (chaosMode) "DISABLE CHAOS" else "ENABLE CHAOS")
                }
              }
            }
          }

          if (terminalLogs.isNotEmpty()) {
            item {
              NeonPanel(modifier = Modifier.fillMaxWidth()) {
                StatusTicker(
                  leftLabel = "TERMINAL FEED",
                  rightLabel = if (chaosMode) "OVERCLOCKED" else "STEADY"
                )
                terminalLogs.forEachIndexed { index, entry ->
                  StaggeredReveal(index = index) {
                    Text(
                      text = "> $entry",
                      style = MaterialTheme.typography.labelMedium
                    )
                  }
                }
              }
            }
          }

          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              GlitchText(
                text = "CATEGORIES",
                style = MaterialTheme.typography.titleMedium
              )
              DataGlyphCloud(seed = pulseSeed)
            }
          }

          itemsIndexed(homeState.categories, key = { _, category -> category.id }) { index, category ->
            StaggeredReveal(index = index) {
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onCategoryClick(category.id) },
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                )
              ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      category.titleExact.uppercase(),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.SemiBold
                    )
                    AssistChip(
                      onClick = { onCategoryClick(category.id) },
                      label = { Text(stringResource(R.string.home_browse)) },
                      colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                      )
                    )
                  }
                  Text(category.descriptionExact, style = MaterialTheme.typography.bodyMedium)
                }
              }
            }
          }

          item {
            GlitchText(
              text = "RECOMMENDED PAYLOADS",
              style = MaterialTheme.typography.titleMedium
            )
          }

          itemsIndexed(homeState.highlights, key = { _, item -> item.id }) { index, item ->
            StaggeredReveal(index = index + homeState.categories.size) {
              ItemHighlight(item = item, onClick = { onItemClick(item) }, chaosMode = chaosMode)
            }
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
  onClick: () -> Unit,
  chaosMode: Boolean
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    )
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(item.titleExact.uppercase(), style = MaterialTheme.typography.titleMedium)
        AssistChip(
          onClick = {},
          label = { Text(item.badgeExact.uppercase()) },
          colors = AssistChipDefaults.assistChipColors(
            containerColor = if (chaosMode) {
              MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
            } else {
              MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
            }
          )
        )
      }
      Text(item.descriptionExact, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
private fun HomeSkeleton(modifier: Modifier = Modifier) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(6) { index ->
      StaggeredReveal(index = index) {
        Box(modifier = Modifier.fillMaxWidth()) {
          ShimmerPlaceholder(
            modifier = Modifier
              .fillMaxWidth()
              .height(84.dp)
          )
        }
      }
    }
  }
}
