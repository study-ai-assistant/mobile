package ru.studyassistant.mobile

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ru.studyassistant.mobile.api.ServerConfig
import ru.studyassistant.mobile.api.normalizeServerAddress
import ru.studyassistant.mobile.ui.theme.StudySpacing

class ServerSettings(context: Context) {
    private val preferences = context.getSharedPreferences("server-settings", Context.MODE_PRIVATE)
    private val allowHttp = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    val address: String get() = preferences.getString("address", null) ?: ServerConfig.BASE_URL
    fun save(value: String): String {
        val normalized = normalizeServerAddress(value, allowHttp)
        preferences.edit().putString("address", normalized).apply()
        return normalized
    }
}

@Composable
fun ServerSettingsButton(address: String, enabled: Boolean, onSave: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var draft by remember(address) { mutableStateOf(address) }
    var error by remember { mutableStateOf<String?>(null) }
    TextButton(enabled = enabled, onClick = { draft = address; error = null; open = true }) {
        Text("Адрес сервера")
    }
    if (open) AlertDialog(onDismissRequest = { open = false },
        title = { Text("Подключение к серверу") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(StudySpacing.compact)) {
                Text("Для телефона укажите IP ноутбука в общей Wi-Fi-сети. После сохранения нажмите «Проверить связь с сервером».")
                OutlinedTextField(value = draft, onValueChange = { draft = it; error = null },
                    label = { Text("URL сервера") }, singleLine = true,
                    isError = error != null, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.onSurface) }
            }
        },
        confirmButton = { TextButton(onClick = {
            try { onSave(draft); open = false }
            catch (e: IllegalArgumentException) { error = e.message }
        }, enabled = enabled) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = { open = false }) { Text("Отмена") } })
}
