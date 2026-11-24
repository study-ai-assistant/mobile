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

@Composable
fun LongProgressContent(state: LongProcessingState, onCancel: () -> Unit, onRetry: () -> Unit,
                        modifier: Modifier = Modifier, materialName: String = "Введённый текст") {
    AdaptiveScreen(modifier, footer = {
        StudyButton(onCancel, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = state.running) { Text("Отменить") }
        if (!state.running && state.canRetry) SecondaryButton("Назад", onCancel, Modifier.fillMaxWidth())
    }) {
        Spacer(Modifier.height(24.dp))
        MaterialBadge(materialName, file = materialName != "Введённый текст")
        Text(if (state.error != null) "Обработка остановлена" else if (state.progress.queued) "Материал в очереди" else "Обрабатываем материал…",
            Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        val progress = state.progress
        Box(Modifier.fillMaxWidth().heightIn(min = if (state.error == null) 200.dp else 280.dp), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.padding(top = 24.dp)) {
                if (state.running && (progress.queued || progress.totalParts == null)) CircularProgressIndicator(
                    Modifier.size(180.dp), color = TextSecondary, strokeWidth = 16.dp, trackColor = Inactive)
                else CircularProgressIndicator(progress = { progress.totalParts?.let { progress.completedParts.toFloat() / it } ?: 0f },
                    modifier = Modifier.size(180.dp), color = TextSecondary, strokeWidth = 16.dp, trackColor = Inactive)
            }
            state.error?.let { error ->
                ErrorCard(error.substringBefore(". "), error.substringAfter(". ", ""), Modifier.align(Alignment.BottomCenter)) {
                    OutlinedButton(if (state.canRetry) onRetry else onCancel, shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, ErrorText), colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorText),
                        modifier = Modifier.widthIn(min = 160.dp)) { Text(if (state.canRetry) "Повторить" else "Назад") }
                }
            }
        }
        if (state.running) {
            if (!progress.queued && progress.totalParts != null) Text(
                if (progress.completedParts == progress.totalParts) "Все части обработаны. Подготавливаем результат…"
                else "Часть ${progress.completedParts + 1} из ${progress.totalParts}",
                Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                StudySymbol("clock", Modifier.size(28.dp), TextPrimary)
                Text(progress.remainingSeconds?.let { if (it == 0) " Завершаем обработку…" else " ≈ ${(it + 59L) / 60} мин." }
                    ?: " Время уточняется…", style = MaterialTheme.typography.titleSmall)
            }
        }
        if (state.cancelled) Text("Ожидание отменено. Сервер может продолжать обработку.")
    }
}
