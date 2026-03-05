package com.opsecapp.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun GlitchText(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.titleLarge
) {
  val transition = rememberInfiniteTransition(label = "glitch_text")
  val xJitter by transition.animateFloat(
    initialValue = -1.5f,
    targetValue = 1.5f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 140, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glitch_x"
  )
  val yJitter by transition.animateFloat(
    initialValue = -0.7f,
    targetValue = 0.7f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 170, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glitch_y"
  )

  Box(modifier = modifier) {
    Text(
      text = text,
      style = style,
      color = Color(0x9927E0FF),
      modifier = Modifier.drawWithContent {
        translate(left = xJitter, top = yJitter) {
          this@drawWithContent.drawContent()
        }
      }
    )
    Text(
      text = text,
      style = style,
      color = Color(0x99FF2A6D),
      modifier = Modifier.drawWithContent {
        translate(left = -xJitter, top = -yJitter) {
          this@drawWithContent.drawContent()
        }
      }
    )
    Text(text = text, style = style, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
fun NeonPanel(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  val colorScheme = MaterialTheme.colorScheme
  val transition = rememberInfiniteTransition(label = "neon_panel")
  val glow by transition.animateFloat(
    initialValue = 0.12f,
    targetValue = 0.28f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "neon_glow"
  )

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(
        brush = Brush.linearGradient(
          colors = listOf(
            colorScheme.surface.copy(alpha = 0.88f),
            colorScheme.surfaceVariant.copy(alpha = 0.68f)
          )
        )
      )
      .drawWithContent {
        drawContent()
        drawRoundRect(
          color = colorScheme.primary.copy(alpha = glow),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
          style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.1f)
        )
      }
      .padding(14.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      content()
    }
  }
}

@Composable
fun ThreatMeter(
  value: Float,
  modifier: Modifier = Modifier
) {
  val safeValue = value.coerceIn(0f, 1f)
  val transition = rememberInfiniteTransition(label = "threat_meter")
  val sweep by transition.animateFloat(
    initialValue = -120f,
    targetValue = 520f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "threat_sweep"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(20.dp)
      .clip(RoundedCornerShape(999.dp))
      .background(MaterialTheme.colorScheme.surface)
  ) {
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(safeValue)
        .background(
          brush = Brush.horizontalGradient(
            listOf(
              Color(0xFF2DFFB3),
              Color(0xFF00D3FF),
              Color(0xFFFF4D78)
            )
          )
        )
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
      val stripeStart = sweep
      drawRect(
        brush = Brush.linearGradient(
          colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.24f),
            Color.Transparent
          ),
          start = Offset(stripeStart, 0f),
          end = Offset(stripeStart + 160f, size.height)
        )
      )
    }
  }
}

@Composable
fun StatusTicker(
  leftLabel: String,
  rightLabel: String,
  modifier: Modifier = Modifier
) {
  val transition = rememberInfiniteTransition(label = "status_ticker")
  val pulse by transition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 760, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "ticker_pulse"
  )

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(
        modifier = Modifier
          .width(8.dp)
          .height(8.dp)
          .clip(RoundedCornerShape(999.dp))
          .background(Color(0xFF35FFA4).copy(alpha = pulse))
      )
      Text(leftLabel, style = MaterialTheme.typography.labelMedium)
    }
    Text(rightLabel, style = MaterialTheme.typography.labelMedium)
  }
}

@Composable
fun MatrixPulseText(
  text: String,
  modifier: Modifier = Modifier
) {
  val transition = rememberInfiniteTransition(label = "matrix_text")
  val alpha by transition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "matrix_alpha"
  )

  Text(
    text = text,
    modifier = modifier,
    style = MaterialTheme.typography.labelLarge,
    color = Color(0xFF7EFFC0).copy(alpha = alpha)
  )
}

@Composable
fun DataGlyphCloud(
  seed: Int,
  modifier: Modifier = Modifier
) {
  val transition = rememberInfiniteTransition(label = "glyph_cloud")
  val drift by transition.animateFloat(
    initialValue = -20f,
    targetValue = 20f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2200, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glyph_drift"
  )
  val alphas = listOf(0.18f, 0.24f, 0.3f)
  val tokens = listOf(
    "0x${(seed * 913).toString(16)}",
    "SIG-${(seed * 33 % 997).toString().padStart(3, '0')}",
    "N${(seed * 7 % 99).toString().padStart(2, '0')}",
    "CHK-${(seed * 19 % 541).toString().padStart(3, '0')}"
  )

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    repeat(tokens.size) { index ->
      Text(
        text = tokens[index],
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alphas[index % alphas.size]),
        modifier = Modifier.drawWithContent {
          translate(left = drift * ((index + 1) / 5f), top = 0f) {
            this@drawWithContent.drawContent()
          }
        }
      )
    }
  }
}

fun threatLabel(value: Float): String {
  val percent = (value.coerceIn(0f, 1f) * 100f).roundToInt()
  return "THREAT VECTOR $percent%"
}
