package ru.studyassistant.mobile.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Same badge on waiting and result screens; always aligned to the start. */
@Composable
fun ModeBadge(title: String, modifier: Modifier = Modifier) {
    Surface(modifier.testTag("mode-badge"), shape = MaterialTheme.shapes.large,
        color = PrimarySurface, border = BorderStroke(2.dp, Primary)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StudySymbol("mode", Modifier.size(24.dp).testTag("mode-star"))
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun MaterialBadge(name: String, file: Boolean = true, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = PrimarySurface, shape = MaterialTheme.shapes.large,
        border = BorderStroke(2.dp, Primary), shadowElevation = 2.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (file) StudySymbol("file", Modifier.size(24.dp), TextPrimary)
            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled,
        shape = MaterialTheme.shapes.medium, border = BorderStroke(2.dp, Primary),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = PrimarySurface, contentColor = TextPrimary)) {
        Text(label, textAlign = TextAlign.Center)
    }
}

/** Scrolls on short screens; keeps actions low without fixed screen-size assumptions. */
@Composable
fun AdaptiveScreen(modifier: Modifier = Modifier, footer: @Composable ColumnScope.() -> Unit,
                   content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = viewportHeight),
            verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
            Column(Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = footer)
        }
    }
}

@Composable
fun ScreenTitle(title: String) {
    Text(title, Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall)
}

@Composable
fun ErrorCard(title: String, description: String, modifier: Modifier = Modifier, compact: Boolean = false,
              action: (@Composable () -> Unit)? = null) {
    Surface(modifier.fillMaxWidth(), color = ErrorSurface, shape = MaterialTheme.shapes.large,
        border = BorderStroke(if (compact) 2.dp else 1.dp, if (compact) ErrorAccent else ErrorText)) {
        if (compact) Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StudySymbol("error-filled", Modifier.padding(top = 2.dp).size(36.dp), ErrorAccent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = ErrorAccent)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
        } else Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StudySymbol("error", Modifier.size(40.dp), ErrorText)
            Text(title, style = MaterialTheme.typography.titleSmall, color = ErrorText, textAlign = TextAlign.Center)
            if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodySmall,
                color = ErrorText, textAlign = TextAlign.Center)
            action?.invoke()
        }
    }
}
