package com.opsecapp.app.ui.category

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opsecapp.app.R
import com.opsecapp.app.ui.components.DataGlyphCloud
import com.opsecapp.app.ui.components.GlitchText
import com.opsecapp.app.ui.components.OpsecBackground
import com.opsecapp.app.ui.components.StaggeredReveal
import com.opsecapp.domain.model.CatalogItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
  stateFlow: kotlinx.coroutines.flow.StateFlow<CategoryUiState>,
  onBack: () -> Unit,
  onItemClick: (CatalogItem) -> Unit
) {
  val state by stateFlow.collectAsStateWithLifecycle()
  val title = state.categoryId.ifBlank { stringResource(R.string.category_fallback_title) }

  Scaffold(
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
    topBar = {
      TopAppBar(
        title = {
          AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "category_title_transition"
          ) { animatedTitle ->
            GlitchText(text = animatedTitle.uppercase(), style = MaterialTheme.typography.titleMedium)
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
    OpsecBackground(modifier = Modifier.padding(padding)) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("CATEGORY PAYLOADS", style = MaterialTheme.typography.titleMedium)
            DataGlyphCloud(seed = state.categoryId.hashCode().absoluteValueSafe())
          }
        }

        itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
          StaggeredReveal(index = index) {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick(item) },
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
              )
            ) {
              Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(item.titleExact.uppercase(), style = MaterialTheme.typography.titleMedium)
                  AssistChip(
                    onClick = {},
                    label = { Text(item.badgeExact.uppercase()) },
                    colors = AssistChipDefaults.assistChipColors(
                      containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.68f)
                    )
                  )
                }
                Text(item.descriptionExact, style = MaterialTheme.typography.bodyMedium)
                Text(
                  text = "${item.sourceConfidence} / ${item.installType}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun Int.absoluteValueSafe(): Int = if (this == Int.MIN_VALUE) 0 else kotlin.math.abs(this)
