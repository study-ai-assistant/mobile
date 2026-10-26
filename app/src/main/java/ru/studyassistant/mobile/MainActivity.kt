package ru.studyassistant.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.studyassistant.mobile.api.ApiException
import ru.studyassistant.mobile.api.StudyApi
import ru.studyassistant.mobile.api.validateMaterial
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AIStudyAssistantTheme { ServerScreen() } }
    }
}

@Composable
private fun ServerScreen() {
    var material by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val api = remember { StudyApi() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val localNetworkPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        error = !granted
        message = if (granted) {
            "Доступ к локальной сети разрешён. Нажмите нужную кнопку ещё раз."
        } else {
            "Для связи с сервером на компьютере разрешите доступ к устройствам поблизости " +
                "в настройках приложения → Разрешения."
        }
    }

    fun runRequest(healthOnly: Boolean) {
        if (busy) return
        val submittedText = material
        val validation = if (healthOnly) null else validateMaterial(submittedText)
        if (validation != null) {
            error = true
            message = validation
            return
        }
        // Сервер разработки находится в локальной сети. Android 17 блокирует её
        // без runtime-разрешения, причём блокировка может выглядеть как таймаут.
        if (Build.VERSION.SDK_INT >= 37 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            localNetworkPermission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            return
        }
        busy = true
        error = false
        message = ""
        scope.launch {
            try {
                message = withContext(Dispatchers.IO) {
                    if (healthOnly) api.health() else api.process(submittedText)
                }
            } catch (e: ApiException) {
                error = true
                message = e.message ?: "Не удалось выполнить запрос."
            } finally {
                busy = false
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
        Column(
            modifier = Modifier.padding(insets).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("ИИ-помощник для учёбы", style = MaterialTheme.typography.headlineSmall)
            Text("Краткий конспект", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = material,
                onValueChange = { material = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Учебный текст") },
                supportingText = { Text("${material.codePointCount(0, material.length)} / 10 000 символов") },
                minLines = 5,
                maxLines = 10,
                enabled = !busy
            )
            Button(onClick = { runRequest(false) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Отправить")
            }
            OutlinedButton(onClick = { runRequest(true) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Проверить связь с сервером")
            }
            if (busy) {
                CircularProgressIndicator()
                Text("Ожидаем ответ сервера…")
            }
            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
    }
}
