package com.opsecapp.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
  primary = Color(0xFF7FE0B6),
  onPrimary = Color(0xFF003826),
  primaryContainer = Color(0xFF045139),
  onPrimaryContainer = Color(0xFFA6F6CF),
  secondary = Color(0xFFA3CCE3),
  onSecondary = Color(0xFF033548),
  secondaryContainer = Color(0xFF1D4B60),
  onSecondaryContainer = Color(0xFFD1E9F6),
  tertiary = Color(0xFFE8C17A),
  onTertiary = Color(0xFF432C00),
  background = Color(0xFF0D1512),
  onBackground = Color(0xFFE4EFEA),
  surface = Color(0xFF101A16),
  onSurface = Color(0xFFE4EFEA),
  surfaceVariant = Color(0xFF2C3732),
  onSurfaceVariant = Color(0xFFC0CCC6),
  error = Color(0xFFFFB4AB),
  onError = Color(0xFF690005)
)

private val LightColors = lightColorScheme(
  primary = Color(0xFF145C43),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFF9EF2C9),
  onPrimaryContainer = Color(0xFF002115),
  secondary = Color(0xFF355F74),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFB9E6FF),
  onSecondaryContainer = Color(0xFF001F2C),
  tertiary = Color(0xFF6A571D),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFF4DE9D),
  onTertiaryContainer = Color(0xFF221B00),
  background = Color(0xFFF3F8F5),
  onBackground = Color(0xFF161D1A),
  surface = Color(0xFFF7FCF9),
  onSurface = Color(0xFF161D1A),
  surfaceVariant = Color(0xFFD8E5DE),
  onSurfaceVariant = Color(0xFF3C4943),
  error = Color(0xFFBA1A1A),
  onError = Color(0xFFFFFFFF)
)

private val OpsecTypography = Typography(
  headlineMedium = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 30.sp,
    lineHeight = 36.sp
  ),
  headlineSmall = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 30.sp
  ),
  titleLarge = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp
  ),
  titleMedium = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 17.sp,
    lineHeight = 23.sp
  ),
  bodyLarge = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp
  ),
  bodyMedium = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
  ),
  labelLarge = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp
  ),
  labelMedium = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp
  )
)

private val OpsecShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp)
)

@Composable
fun OpsecTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColors else LightColors,
    typography = OpsecTypography,
    shapes = OpsecShapes,
    content = content
  )
}
