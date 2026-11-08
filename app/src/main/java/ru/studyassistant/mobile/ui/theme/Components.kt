package ru.studyassistant.mobile.ui.theme

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Текущие отступы приложения, централизованные до уточнения размеров в Figma.
object StudySpacing {
    val screen = 16.dp
    val gap = 16.dp
    val compact = 8.dp
    val row = 10.dp
}
val StudyShapes = Shapes(
    small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun StudyButton(onClick: () -> Unit, modifier: Modifier = Modifier,
                enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = Inactive, disabledContentColor = TextOnPrimary),
        content = content)
}

@Composable
fun studyTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun ErrorMessage(text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer) {
        Text(text, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(StudySpacing.screen))
    }
}
