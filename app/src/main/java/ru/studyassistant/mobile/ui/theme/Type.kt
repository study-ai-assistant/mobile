package ru.studyassistant.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ru.studyassistant.mobile.R

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)
// Размеры и интерлиньяж Material сохранены: числовых значений в UI-kit нет.
private val Defaults = Typography()
val Typography = Typography(
    displayLarge = Defaults.displayLarge.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    displayMedium = Defaults.displayMedium.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    displaySmall = Defaults.displaySmall.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    headlineLarge = Defaults.headlineLarge.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    headlineMedium = Defaults.headlineMedium.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    headlineSmall = Defaults.headlineSmall.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    titleLarge = Defaults.titleLarge.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    titleMedium = Defaults.titleMedium.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    titleSmall = Defaults.titleSmall.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    bodyLarge = Defaults.bodyLarge.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    bodyMedium = Defaults.bodyMedium.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal),
    bodySmall = Defaults.bodySmall.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal),
    labelLarge = Defaults.labelLarge.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    labelMedium = Defaults.labelMedium.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal),
    labelSmall = Defaults.labelSmall.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal)
)
