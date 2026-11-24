package ru.studyassistant.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.studyassistant.mobile.api.validateMaterial
import ru.studyassistant.mobile.ui.theme.*

@Composable
fun MaterialContent(state: StudyState, onText: (String) -> Unit, onPickFile: () -> Unit,
                    onRemoveFile: () -> Unit, onModes: () -> Unit, onConnectionTools: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Привет!", modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onConnectionTools), style = MaterialTheme.typography.headlineLarge)
        Text("Загрузите учебный материал, а затем выберите режим, чтобы начать работу с ИИ.",
            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
    Surface(shape = MaterialTheme.shapes.large, color = PrimarySurface,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !state.busy, onClickLabel = "Выбрать файл", onClick = onPickFile)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (state.loadingFile) CircularProgressIndicator(Modifier.size(40.dp), trackColor = Inactive)
            else StudySymbol(when {
                state.fileName != null && state.error != null -> "error"
                state.fileName != null -> "success"
                else -> "upload"
            }, Modifier.size(40.dp), if (state.fileName != null && state.error != null) ErrorText else TextPrimary)
            Text(when {
                state.loadingFile -> "Загрузка файла…"
                state.fileName != null && state.error != null -> "Ошибка"
                state.fileName != null -> "Материал добавлен"
                else -> "Добавьте материал"
            }, style = MaterialTheme.typography.titleMedium)
            if (state.fileName != null) {
                Surface(shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, Primary), color = Background, shadowElevation = 2.dp) {
                    Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StudySymbol("file", Modifier.size(24.dp), TextPrimary)
                        Text(state.fileName, Modifier.weight(1f).padding(8.dp).testTag("selected-file"),
                            style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        IconButton(onClick = onRemoveFile, enabled = !state.busy, modifier = Modifier.semantics { contentDescription = "Убрать файл" }) {
                            StudySymbol("delete", Modifier.size(22.dp), ErrorText)
                        }
                    }
                }
            } else Text(if (state.loadingFile) "Пожалуйста, подождите." else "Введите текст или загрузите файл (только .txt и .pdf).", style = MaterialTheme.typography.bodySmall)
        }
    }
    val length = state.material.codePointCount(0, state.material.length)
    val validation = if (state.material.isNotEmpty() && state.fileName == null) validateMaterial(state.material) else null
    Box(Modifier.fillMaxWidth()) {
        Surface(color = PrimarySurface, shape = MaterialTheme.shapes.large) {
            Column {
                TextField(value = state.material, onValueChange = onText,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 168.dp).semantics { contentDescription = "Учебный текст" },
                    placeholder = { Text("Введите текст материала…", style = MaterialTheme.typography.bodySmall) },
                    enabled = !state.busy && state.fileName == null, textStyle = MaterialTheme.typography.bodySmall,
                    colors = TextFieldDefaults.colors(focusedContainerColor = PrimarySurface, unfocusedContainerColor = PrimarySurface,
                        disabledContainerColor = PrimarySurface, focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent, disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent),
                    minLines = 7, maxLines = 10)
                Text("$length/24000", Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End,
                    color = if (validation != null) ErrorText else TextPrimary)
            }
        }
        val error = state.error ?: validation
        if (error != null) {
            val presentation = materialErrorPresentation(error)
            ErrorCard(presentation.first, presentation.second, Modifier.align(Alignment.Center).padding(horizontal = 4.dp))
        }
    }
    StudyButton(onClick = onModes, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        enabled = !state.busy && state.error == null && (state.fileBytes?.isNotEmpty() ?: (validateMaterial(state.material) == null))) {
        Text("Выбрать режим")
    }
}

internal fun materialErrorPresentation(message: String): Pair<String, String> = when {
    message.contains("24 000") -> "Слишком большой объём текста." to "Допустимо не более 24 000 символов. Сократите текст."
    message.contains("10 МиБ") -> "Слишком большой объём документа." to "Максимальный размер файла — 10 МиБ. Выберите файл меньшего размера."
    message.contains("скан", true) || message.contains("текстового слоя") -> "Документ похож на отсканированное изображение." to "Выберите файл с текстовым слоем или введите текст вручную."
    message.contains("парол", true) -> "Файл защищён паролем." to "Снимите защиту в настройках документа и попробуйте загрузить его снова."
    message.contains("поврежд", true) -> "Повреждённый файл." to "Файл не читается или повреждён. Попробуйте выбрать другой документ."
    message.contains("пуст", true) -> "Материал пуст." to "Введите текст или выберите файл с текстом."
    else -> "Не удалось загрузить материал." to message
}

@Composable
fun ModesContent(state: StudyState, onSelect: (ProcessingMode) -> Unit, onReload: () -> Unit, onSubmit: () -> Unit) {
    MaterialBadge(state.fileName ?: state.material, file = state.fileName != null)
    state.modesError?.let { ErrorMessage(it, Modifier.fillMaxWidth()) }
    if (!state.modesLoaded && !state.busy) SecondaryButton("Загрузить режимы снова", onReload, Modifier.fillMaxWidth())
    if (!state.busy && state.error == null && state.modesLoaded) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.modes.forEach { mode ->
                val selected = state.mode == mode
                Surface(shape = MaterialTheme.shapes.medium, color = PrimarySurface,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, Primary)) {
                    Row(Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton,
                        onClick = { onSelect(mode) }).padding(10.dp), verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StudySymbol("mode", Modifier.size(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(mode.title, style = MaterialTheme.typography.bodyMedium)
                            Text(mode.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
    if (state.modesLoaded && (state.error == null || !state.canRetry)) StudyButton(onClick = onSubmit,
        enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Создать") }
}
