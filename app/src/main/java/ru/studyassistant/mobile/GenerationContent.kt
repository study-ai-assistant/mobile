package ru.studyassistant.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.studyassistant.mobile.ui.theme.*

/** Waiting for a normal request; no fabricated percentage or partial response. */
@Composable
fun GenerationContent(state: StudyState, longWait: Boolean, onCancel: () -> Unit, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StudySpacing.gap)) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) {
            Row(Modifier.padding(StudySpacing.compact), verticalAlignment = Alignment.CenterVertically) {
                StudySymbol("mode", Modifier.size(20.dp))
                Text(state.mode.title, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (state.busy) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(StudySpacing.screen), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(StudySpacing.compact)) {
                    CircularProgressIndicator(Modifier.size(48.dp), trackColor = Inactive)
                    Column(Modifier.weight(1f)) {
                        Text("Генерирую…", style = MaterialTheme.typography.titleMedium)
                        Text(if (longWait) "Обработка занимает больше времени. Можно подождать или отменить ожидание."
                            else "Это может занять до минуты", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Surface(Modifier.fillMaxWidth().height(140.dp), shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant) {}
            Text("Не закрывайте экран, пока готовится ответ", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
        } else {
            state.error?.let { ErrorMessage(it, Modifier.fillMaxWidth()) }
            if (state.canRetry) StudyButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Повторить") }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
        }
    }
}
