package com.opsecapp.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

@Composable
fun OpsecBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  val colors = MaterialTheme.colorScheme
  val brush = Brush.verticalGradient(
    listOf(
      colors.background,
      colors.surface,
      colors.primary.copy(alpha = 0.08f)
    )
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(brush),
    content = content
  )
}
