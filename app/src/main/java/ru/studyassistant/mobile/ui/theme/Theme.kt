package ru.studyassistant.mobile.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val UiKitColors = lightColorScheme(
    primary = Primary, onPrimary = TextOnPrimary,
    primaryContainer = PrimarySurface, onPrimaryContainer = TextPrimary,
    secondary = TextSecondary, onSecondary = TextOnPrimary,
    secondaryContainer = PrimarySurface, onSecondaryContainer = TextPrimary,
    tertiary = TextSecondary, onTertiary = TextOnPrimary,
    tertiaryContainer = Success, onTertiaryContainer = TextPrimary,
    background = Background, onBackground = TextPrimary,
    surface = Background, onSurface = TextPrimary,
    surfaceVariant = PrimarySurface, onSurfaceVariant = TextSecondary,
    surfaceContainer = PrimarySurface, surfaceContainerLow = Background,
    surfaceContainerHigh = PrimarySurface,
    error = ErrorText, onError = TextOnPrimary,
    errorContainer = ErrorSurface, onErrorContainer = TextPrimary,
    outline = TextSecondary, outlineVariant = PrimarySurface,
    surfaceTint = Primary
)

// UI-kit содержит только светлую палитру: цвета обоев и системная тёмная тема
// не должны подменять утверждённые цвета. Тёмная палитра требует отдельного макета.
@Composable
fun AIStudyAssistantTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.activity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val status = controller?.isAppearanceLightStatusBars
        val navigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        onDispose {
            if (status != null) controller?.isAppearanceLightStatusBars = status
            if (navigation != null) controller?.isAppearanceLightNavigationBars = navigation
        }
    }
    MaterialTheme(colorScheme = UiKitColors, typography = Typography,
        shapes = StudyShapes, content = content)
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
