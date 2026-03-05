package com.opsecapp.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
  primary = Color(0xFF2FFFA2),
  onPrimary = Color(0xFF001A12),
  primaryContainer = Color(0xFF0C3D2A),
  onPrimaryContainer = Color(0xFF94FFCF),
  secondary = Color(0xFF2DDAFF),
  onSecondary = Color(0xFF00151C),
  secondaryContainer = Color(0xFF003E4E),
  onSecondaryContainer = Color(0xFFA8F1FF),
  tertiary = Color(0xFFFF477E),
  onTertiary = Color(0xFF2A0012),
  tertiaryContainer = Color(0xFF5F1734),
  onTertiaryContainer = Color(0xFFFFB9CE),
  background = Color(0xFF06080D),
  onBackground = Color(0xFFE2F5EC),
  surface = Color(0xFF0C1018),
  onSurface = Color(0xFFE2F5EC),
  surfaceVariant = Color(0xFF131A24),
  onSurfaceVariant = Color(0xFF9DB3AB),
  error = Color(0xFFFF5A5A),
  onError = Color(0xFF2B0000)
)

private val LightColors = lightColorScheme(
  primary = Color(0xFF00A266),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFB8F9DE),
  onPrimaryContainer = Color(0xFF002014),
  secondary = Color(0xFF007FA0),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFC4F0FF),
  onSecondaryContainer = Color(0xFF001F28),
  tertiary = Color(0xFFB7265A),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFFFD6E3),
  onTertiaryContainer = Color(0xFF390015),
  background = Color(0xFFEAF2EF),
  onBackground = Color(0xFF0A1814),
  surface = Color(0xFFF4FAF8),
  onSurface = Color(0xFF0A1814),
  surfaceVariant = Color(0xFFD4E0DB),
  onSurfaceVariant = Color(0xFF34423E),
  error = Color(0xFFBA1A1A),
  onError = Color(0xFFFFFFFF)
)

private val DisplayFamily = FontFamily.Monospace
private val BodyFamily = FontFamily.SansSerif

private val OpsecTypography = Typography(
  headlineMedium = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.2.sp,
    fontSize = 32.sp,
    lineHeight = 38.sp
  ),
  headlineSmall = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 1.sp,
    fontSize = 24.sp,
    lineHeight = 30.sp
  ),
  titleLarge = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.8.sp,
    fontSize = 20.sp,
    lineHeight = 26.sp
  ),
  titleMedium = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.5.sp,
    fontSize = 16.sp,
    lineHeight = 22.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = BodyFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 23.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = BodyFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 21.sp
  ),
  labelLarge = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.Medium,
    letterSpacing = 1.sp,
    fontSize = 13.sp,
    lineHeight = 18.sp
  ),
  labelMedium = TextStyle(
    fontFamily = DisplayFamily,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.8.sp,
    fontSize = 12.sp,
    lineHeight = 16.sp
  )
)

private val OpsecShapes = Shapes(
  extraSmall = RoundedCornerShape(6.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(20.dp)
)

@Composable
fun OpsecTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColors else LightColors,
    typography = OpsecTypography,
    shapes = OpsecShapes,
    content = content
  )
}
