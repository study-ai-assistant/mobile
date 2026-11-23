package ru.studyassistant.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import ru.studyassistant.mobile.api.validateMaterial
import ru.studyassistant.mobile.ui.theme.*

@Composable
fun MaterialContent(state: StudyState, onText: (String) -> Unit, onPickFile: () -> Unit,
                    onRemoveFile: () -> Unit, onModes: () -> Unit) {
    Text("Привет!", style = MaterialTheme.typography.headlineLarge)
    Text("Загрузите учебный материал, а затем выберите режим, чтобы начать работу с ИИ.",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !state.busy, onClickLabel = "Выбрать файл", onClick = onPickFile)) {
        Column(Modifier.padding(StudySpacing.screen), verticalArrangement = Arrangement.spacedBy(StudySpacing.compact)) {
            if (state.loadingFile) CircularProgressIndicator(Modifier.size(40.dp))
            else StudySymbol(if (state.fileName != null) "success" else "upload", color = MaterialTheme.colorScheme.onSurface)
            Text(when {
                state.loadingFile -> "Загрузка файла…"
                state.fileName != null -> "Материал добавлен"
                else -> "Добавьте материал"
            }, style = MaterialTheme.typography.titleMedium)
            if (state.fileName != null) {
                Surface(shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    color = MaterialTheme.colorScheme.background) {
                    Row(Modifier.fillMaxWidth().padding(start = StudySpacing.compact), verticalAlignment = Alignment.CenterVertically) {
                        StudySymbol("file", color = MaterialTheme.colorScheme.onSurface)
                        Text(state.fileName, Modifier.weight(1f).padding(StudySpacing.compact).testTag("selected-file"), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onRemoveFile, enabled = !state.busy,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Убрать файл", style = MaterialTheme.typography.labelSmall) }
                    }
                }
                TextButton(onClick = onPickFile, enabled = !state.busy) { Text("Заменить файл") }
            } else Text(if (state.loadingFile) "Пожалуйста, подождите." else "Введите текст или загрузите файл (только .txt и .pdf).", style = MaterialTheme.typography.bodySmall)
        }
    }
    val length = state.material.codePointCount(0, state.material.length)
    val validation = if (state.material.isNotEmpty() && state.fileName == null) validateMaterial(state.material) else null
    OutlinedTextField(value = state.material, onValueChange = onText,
        modifier = Modifier.fillMaxWidth().heightIn(min = StudySpacing.inputHeight),
        label = { Text("Учебный текст", style = MaterialTheme.typography.labelSmall) }, placeholder = { Text("Введите текст материала…") },
        supportingText = { Text("$length / 24 000 символов", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End) },
        enabled = !state.busy && state.fileName == null, isError = validation != null,
        shape = MaterialTheme.shapes.large, colors = studyTextFieldColors(), minLines = 6, maxLines = 10)
    if (state.fileName != null) Text("Чтобы ввести текст, уберите выбранный файл.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    validation?.let { ErrorMessage(it, Modifier.fillMaxWidth()) }
    StudyButton(onClick = onModes, modifier = Modifier.fillMaxWidth().heightIn(min = StudySpacing.actionHeight),
        enabled = !state.busy && (state.fileBytes?.isNotEmpty() ?: (validateMaterial(state.material) == null))) {
        Text("Выбрать режим")
    }
}

@Composable
fun ModesContent(state: StudyState, onSelect: (ProcessingMode) -> Unit, onReload: () -> Unit,
                 onSubmit: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth()) {
        Text(state.fileName ?: "Введённый текст · ${state.material.codePointCount(0, state.material.length)} символов",
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(StudySpacing.screen))
    }
    state.modesError?.let { ErrorMessage(it, Modifier.fillMaxWidth()) }
    if (!state.modesLoaded && !state.busy) {
        OutlinedButton(onClick = onReload, modifier = Modifier.fillMaxWidth()) { Text("Загрузить режимы снова") }
    }
    if (!state.busy && state.error == null && state.modesLoaded) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(StudySpacing.compact)) {
            state.modes.forEach { mode ->
                val selected = state.mode == mode
                Surface(shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, MaterialTheme.colorScheme.primary)) {
                    Row(Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton,
                        onClick = { onSelect(mode) }).padding(StudySpacing.row),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(StudySpacing.compact)) {
                        StudySymbol("mode", Modifier.size(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(mode.title, style = MaterialTheme.typography.bodyMedium)
                            Text(mode.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    if (state.modesLoaded && (state.error == null || !state.canRetry)) {
        StudyButton(onClick = onSubmit, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = StudySpacing.actionHeight)) { Text("Создать") }
    }
}
