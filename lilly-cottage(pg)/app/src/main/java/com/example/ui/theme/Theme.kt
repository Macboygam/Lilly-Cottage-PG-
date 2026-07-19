package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = BoldPurple80,
    secondary = BoldPurpleGrey80,
    background = BoldBgDark,
    surface = BoldSurfaceDark,
    onPrimary = BoldBgDark,
    onSecondary = BoldSecondaryDark,
    outline = BoldOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BoldPrimary,
    onPrimary = BoldOnPrimary,
    primaryContainer = BoldPrimaryContainer,
    onPrimaryContainer = BoldOnPrimaryContainer,
    secondary = BoldSecondary,
    onSecondary = BoldOnSecondary,
    background = BoldBackground,
    surface = BoldSurface,
    onBackground = BoldOnBackground,
    onSurface = BoldOnSurface,
    outline = BoldOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Set to false to strictly enforce the unique Bold Typography branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

