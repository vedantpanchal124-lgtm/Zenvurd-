package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoiceStudioColorScheme =
  darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color(0xFF111827),
    primaryContainer = IndigoSecondary,
    onPrimaryContainer = TextPrimary,
    secondary = PurpleTertiary,
    onSecondary = Color.White,
    tertiary = TealAccent,
    background = NavyBackground,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF334155)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = VoiceStudioColorScheme, typography = Typography, content = content)
}
