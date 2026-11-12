package ru.studyassistant.mobile

import androidx.compose.foundation.BorderStroke
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
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(StudySpacing.screen),
            verticalArrangement = Arrangement.spacedBy(StudySpacing.compact)) {
            Text(when {
                state.loadingFile -> "Загрузка файла…"
                state.fileName != null -> "Материал добавлен"
                else -> "Добавьте материал"
            }, style = MaterialTheme.typography.titleMedium)
            if (state.loadingFile) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Читаем файл. Пожалуйста, подождите.")
            } else if (state.fileName != null) {
                Text(state.fileName, modifier = Modifier.testTag("selected-file"))
                TextButton(onClick = onRemoveFile, enabled = !state.busy) { Text("Убрать файл") }
            } else {
                Text("Введите текст или загрузите файл (только .txt).",
                    style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onPickFile, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()) {
                Text(if (state.fileName == null) "Выбрать файл" else "Заменить файл")
            }
        }
    }
    val length = state.material.codePointCount(0, state.material.length)
    val validation = if (state.material.isNotEmpty() && state.fileName == null) validateMaterial(state.material) else null
    OutlinedTextField(value = state.material, onValueChange = onText,
        modifier = Modifier.fillMaxWidth().heightIn(min = StudySpacing.inputHeight),
        label = { Text("Учебный текст") }, placeholder = { Text("Введите текст материала…") },
        supportingText = { Text("$length / 10 000 символов") },
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
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
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
                    border = if (selected) BorderStroke(StudySpacing.selectionBorder, MaterialTheme.colorScheme.primary) else null) {
                    Row(Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton,
                        onClick = { onSelect(mode) }).padding(StudySpacing.row),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(StudySpacing.compact)) {
                        RadioButton(selected = selected, onClick = null)
                        Column(Modifier.weight(1f)) {
                            Text(mode.title, style = MaterialTheme.typography.titleMedium)
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
