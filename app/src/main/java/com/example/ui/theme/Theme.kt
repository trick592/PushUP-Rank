package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.domain.model.ShopCatalog

@Composable
fun PushUpRankTheme(
  selectedThemeId: String = ShopCatalog.DEFAULT_THEME_ID,
  content: @Composable () -> Unit,
) {
  val (primary, secondary) =
    when (selectedThemeId) {
      "theme_crimson" -> CrimsonPrimary to CrimsonSecondary
      "theme_cyber" -> CyberPrimary to CyberSecondary
      "theme_gold" -> GoldPrimary to GoldSecondary
      else -> VoltLime to CyberCyan
    }

  val colorScheme =
    darkColorScheme(
      primary = primary,
      onPrimary = ArenaBlack,
      primaryContainer = primary.copy(alpha = 0.16f),
      onPrimaryContainer = primary,
      secondary = secondary,
      onSecondary = ArenaBlack,
      secondaryContainer = secondary.copy(alpha = 0.16f),
      onSecondaryContainer = secondary,
      tertiary = CoinAmber,
      onTertiary = ArenaBlack,
      background = ArenaBlack,
      onBackground = ArenaTextPrimary,
      surface = ArenaSurface,
      onSurface = ArenaTextPrimary,
      surfaceVariant = ArenaSurfaceElevated,
      onSurfaceVariant = ArenaTextSecondary,
      surfaceContainerLow = ArenaSurface,
      surfaceContainer = ArenaSurfaceElevated,
      surfaceContainerHigh = ArenaSurfaceHighest,
      outline = ArenaOutline,
      error = PostureRed,
      onError = Color.White,
    )

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}
