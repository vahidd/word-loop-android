package com.codewiz.wordloop.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.codewiz.wordloop.domain.model.WordStatus

val AccentLight = Color(0xFF007AFF)
val AccentDark = Color(0xFF0A84FF)
val GroupedBackgroundLight = Color(0xFFF2F2F7)
val GroupedBackgroundDark = Color(0xFF000000)
val SecondaryGroupedLight = Color(0xFFFFFFFF)
val SecondaryGroupedDark = Color(0xFF1C1C1E)
val ChipFillLight = Color(0xFFE5E5EA)
val ChipFillDark = Color(0xFF2C2C2E)
val OrangeAccent = Color(0xFFFF9500)
val YellowAccent = Color(0xFFFFCC00)

object WlDesign {
    val metricRadius = 16.dp
    val cardRadius = 16.dp
    val rowRadius = 14.dp
    val heroRadius = 22.dp
    val chipRadius = 10.dp
    val screenPadding = 16.dp
    val sectionSpacing = 24.dp
    val cardShape = RoundedCornerShape(cardRadius)
    val rowShape = RoundedCornerShape(rowRadius)
    val heroShape = RoundedCornerShape(heroRadius)
}

fun WordStatus.accentColor(): Color = when (this) {
    WordStatus.NEW -> Color(0xFF007AFF)
    WordStatus.LEARNING -> OrangeAccent
    WordStatus.DIFFICULT -> Color(0xFFFF3B30)
    WordStatus.MASTERED -> Color(0xFF34C759)
    WordStatus.ARCHIVED -> Color(0xFF8E8E93)
}

@Composable
fun accentHeroGradient(): Brush {
    val accent = MaterialTheme.colorScheme.primary
    return Brush.linearGradient(
        colors = listOf(accent, accent.copy(alpha = 0.75f), OrangeAccent.copy(alpha = 0.9f)),
    )
}

@Composable
fun xpHeroGradient(): Brush = Brush.linearGradient(
    colors = listOf(OrangeAccent, YellowAccent.copy(alpha = 0.85f), MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
)

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color.White,
    secondary = OrangeAccent,
    background = GroupedBackgroundLight,
    surface = SecondaryGroupedLight,
    surfaceVariant = ChipFillLight,
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color.White,
    secondary = OrangeAccent,
    background = GroupedBackgroundDark,
    surface = SecondaryGroupedDark,
    surfaceVariant = ChipFillDark,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun WordLoopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
