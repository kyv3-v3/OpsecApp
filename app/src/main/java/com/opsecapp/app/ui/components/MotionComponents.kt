package com.opsecapp.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun StaggeredReveal(
  index: Int,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  var visible by remember(index) { mutableStateOf(false) }

  LaunchedEffect(index) {
    delay((index * 40L).coerceAtMost(360L))
    visible = true
  }

  AnimatedVisibility(
    modifier = modifier,
    visible = visible,
    enter = fadeIn(animationSpec = tween(durationMillis = 320)) +
      scaleIn(
        animationSpec = tween(durationMillis = 340),
        initialScale = 0.94f
      ) +
      slideInVertically(
        animationSpec = tween(durationMillis = 320),
        initialOffsetY = { fullHeight -> fullHeight / 5 }
      )
  ) {
    content()
  }
}

@Composable
fun ShimmerPlaceholder(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(14.dp)
) {
  val colorScheme = MaterialTheme.colorScheme
  val transition = rememberInfiniteTransition(label = "shimmer_transition")
  val xOffset by transition.animateFloat(
    initialValue = -320f,
    targetValue = 640f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1100, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "shimmer_offset"
  )

  val brush = Brush.linearGradient(
    colors = listOf(
      colorScheme.secondary.copy(alpha = 0.2f),
      colorScheme.primary.copy(alpha = 0.6f),
      colorScheme.tertiary.copy(alpha = 0.22f)
    ),
    start = Offset(x = xOffset, y = 0f),
    end = Offset(x = xOffset + 320f, y = 220f)
  )

  Box(
    modifier = modifier.background(brush = brush, shape = shape)
  )
}
