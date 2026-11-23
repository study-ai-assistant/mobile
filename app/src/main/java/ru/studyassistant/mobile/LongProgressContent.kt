package ru.studyassistant.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import ru.studyassistant.mobile.ui.theme.ErrorMessage
import ru.studyassistant.mobile.ui.theme.StudySpacing

@Composable
fun LongProgressContent(state: LongProcessingState, onCancel: () -> Unit, onRetry: () -> Unit,
                        modifier: Modifier = Modifier, materialName: String = "Введённый текст") {
    Column(modifier.background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(StudySpacing.screen),
        verticalArrangement = Arrangement.spacedBy(StudySpacing.gap), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large, border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()) {
            Text(materialName, Modifier.padding(StudySpacing.screen), style = MaterialTheme.typography.bodyMedium)
        }
        when {
            state.running -> {
                val progress = state.progress
                Text(if (progress.queued) "Материал в очереди" else "Обрабатываем материал…",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                val total = progress.totalParts
                Box(Modifier.fillMaxWidth().padding(vertical = StudySpacing.screen), contentAlignment = Alignment.Center) {
                    if (progress.queued || total == null) CircularProgressIndicator(Modifier.size(160.dp))
                    else CircularProgressIndicator(progress = { progress.completedParts.toFloat() / total },
                        modifier = Modifier.size(160.dp), strokeWidth = 12.dp,
                        trackColor = ru.studyassistant.mobile.ui.theme.Inactive)
                }
                if (!progress.queued && total != null) {
                    Text(if (progress.completedParts == total) "Все части обработаны. Подготавливаем результат…"
                        else "Часть ${progress.completedParts + 1} из $total", style = MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("Готово частей: ${progress.completedParts} из $total")
                }
                Text(progress.remainingSeconds?.let {
                    if (it == 0) "Завершаем обработку…" else "Осталось примерно ${(it + 59L) / 60} мин."
                } ?: "Обработка может занять 3–5 минут. Точное время пока неизвестно.")
                ru.studyassistant.mobile.ui.theme.StudyButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = StudySpacing.actionHeight)) { Text("Отменить") }
            }
            state.error != null -> {
                Text("Обработка остановлена", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer)) {
                    Column(Modifier.padding(StudySpacing.screen),
                        verticalArrangement = Arrangement.spacedBy(StudySpacing.gap), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("!", style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(state.error.substringBefore(". "), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(state.error.substringAfter(". ", ""), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        if (state.canRetry) OutlinedButton(onClick = onRetry, shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer)) { Text("Повторить") }
                        else OutlinedButton(onClick = onCancel, shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary)) { Text("Назад") }
                    }
                }
                if (state.canRetry) OutlinedButton(onClick = onCancel, shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary), colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary)) { Text("Назад") }
            }
            state.cancelled -> Text("Ожидание отменено. Сервер может продолжать обработку.")
            state.result != null -> Text("Результат готов")
            else -> Text("Материал ещё не отправлен")
        }
    }
}
