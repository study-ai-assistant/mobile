package ru.studyassistant.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.studyassistant.mobile.ui.theme.*

@Composable
fun GenerationContent(state: StudyState, longWait: Boolean, onCancel: () -> Unit, onRetry: () -> Unit, onBack: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val errorTopGap = (maxHeight * .22f).coerceIn(48.dp, 180.dp)
    AdaptiveScreen(footer = {
        if (state.busy) SecondaryButton("Отмена", onCancel, Modifier.fillMaxWidth())
        else {
            if (state.canRetry) StudyButton(onRetry, Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 64.dp)) { Text("Повторить", style = MaterialTheme.typography.titleMedium) }
            SecondaryButton("Назад", onBack, Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 64.dp))
        }
    }) {
        ScreenTitle("Результат")
        if (state.busy) {
            ModeBadge(state.mode.title)
            Surface(shape = MaterialTheme.shapes.large, color = PrimarySurface) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(48.dp), color = TextSecondary, trackColor = Inactive)
                    Column(Modifier.weight(1f)) {
                        Text("Генерирую…", style = MaterialTheme.typography.titleMedium)
                        Text(if (longWait) "Обработка занимает больше времени. Можно подождать или отменить ожидание."
                            else "Это может занять до минуты", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Surface(Modifier.fillMaxWidth().height(220.dp), shape = MaterialTheme.shapes.large, color = PrimarySurface) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { index -> Box(Modifier.fillMaxWidth(if (index == 3) .7f else 1f).height(6.dp).background(Inactive.copy(alpha = .35f), CircleShape)) }
                }
            }
            Text("Не закрывайте экран, пока готовится ответ", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        } else {
            val message = state.error.orEmpty()
            val title = when {
                message.contains("Лимит", true) -> "Лимит исчерпан"
                message.contains("соедин", true) || message.contains("связ", true) -> "Нет связи"
                message.contains("время", true) || message.contains("долго", true) -> "Время ожидания истекло"
                message.contains("больш", true) || message.contains("объём", true) -> "Материал слишком объёмный"
                else -> "Сервер недоступен"
            }
            Spacer(Modifier.height(errorTopGap))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(96.dp).background(ErrorSurface, CircleShape), contentAlignment = Alignment.Center) {
                    StudySymbol(when (title) {
                        "Нет связи" -> "offline"
                        "Материал слишком объёмный" -> "upload"
                        else -> "warning"
                    }, Modifier.size(48.dp), ErrorAccent)
                }
            }
            val explanation = if (title == "Нет связи") "Проверьте подключение к интернету и повторите запрос"
                else message.substringAfter(". ", message)
            ErrorCard(title, explanation, compact = true)
        }
    }
    }
}
