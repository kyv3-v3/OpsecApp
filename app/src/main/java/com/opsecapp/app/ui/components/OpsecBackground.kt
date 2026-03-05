package com.opsecapp.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun OpsecBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  val colors = MaterialTheme.colorScheme
  val transition = rememberInfiniteTransition(label = "opsec_background")
  val drift by transition.animateFloat(
    initialValue = -0.25f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 18000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "bg_drift"
  )
  val scanlineShift by transition.animateFloat(
    initialValue = 0f,
    targetValue = 24f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "scan_shift"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val gradient = Brush.linearGradient(
        colors = listOf(
          colors.background,
          colors.surface,
          colors.secondary.copy(alpha = 0.1f),
          colors.primary.copy(alpha = 0.14f),
          colors.background
        ),
        start = Offset(x = size.width * drift, y = 0f),
        end = Offset(x = size.width * (1f - drift), y = size.height)
      )
      drawRect(brush = gradient)

      val gridColor = colors.onSurface.copy(alpha = 0.08f)
      val gridGap = 34f
      var x = 0f
      while (x <= size.width) {
        drawLine(
          color = gridColor,
          start = Offset(x, 0f),
          end = Offset(x, size.height),
          strokeWidth = 1f
        )
        x += gridGap
      }

      var y = 0f
      while (y <= size.height) {
        drawLine(
          color = gridColor,
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1f
        )
        y += gridGap
      }

      val scanColor = Color(0xFF6DFFCB).copy(alpha = 0.055f)
      var scanY = -24f + scanlineShift
      while (scanY <= size.height) {
        drawRect(
          color = scanColor,
          topLeft = Offset(0f, scanY),
          size = androidx.compose.ui.geometry.Size(size.width, 2.2f)
        )
        scanY += 8f
      }
    }

    content()
  }
}
