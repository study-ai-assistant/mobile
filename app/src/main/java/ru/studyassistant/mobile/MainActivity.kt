package ru.studyassistant.mobile

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.studyassistant.mobile.api.StudyApi
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class StudyViewModel : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val api = StudyApi()
    val flow = StudyFlow(scope,
        process = { text, mode -> withContext(Dispatchers.IO) { api.process(text, mode.name) } },
        health = { withContext(Dispatchers.IO) { api.health() } }
    )
    override fun onCleared() { scope.cancel() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val flow = ViewModelProvider(this)[StudyViewModel::class.java].flow
        setContent { AIStudyAssistantTheme { StudyApp(flow) } }
    }
}

@Composable
fun StudyApp(flow: StudyFlow) {
    val state by flow.state.collectAsState()
    val context = LocalContext.current
    var copied by remember(state.result, state.screen) { mutableStateOf(false) }
    var longWait by remember { mutableStateOf(false) }
    LaunchedEffect(state.busy) {
        longWait = false
        if (state.busy) { delay(20_000); longWait = true }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        flow.permissionMessage(it)
    }
    fun request(health: Boolean, retry: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 37 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else if (retry) flow.retry() else if (health) flow.checkHealth() else flow.submit()
    }
    BackHandler(state.screen != Screen.MATERIAL || state.busy) { flow.back() }
    Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
        Column(
            Modifier.padding(insets).imePadding().padding(16.dp)
                .fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(when (state.screen) {
                Screen.MATERIAL -> "Загрузка материала"
                Screen.MODES -> "Выбор режима"
                Screen.RESULT -> "Результат"
            }, style = MaterialTheme.typography.headlineSmall)
            when (state.screen) {
                Screen.MATERIAL -> {
                    Text("Вставьте учебный текст. Загрузка файлов появится позже.")
                    OutlinedTextField(
                        value = state.material, onValueChange = flow::editMaterial,
                        modifier = Modifier.fillMaxWidth(), label = { Text("Учебный текст") },
                        supportingText = { Text("${state.material.codePointCount(0, state.material.length)} / 10 000 символов") },
                        minLines = 5, maxLines = 10, enabled = !state.busy
                    )
                    Button(onClick = flow::showModes, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Выбрать режим")
                    }
                    OutlinedButton(onClick = { request(true) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Проверить связь с сервером")
                    }
                }
                Screen.MODES -> {
                    Text("Введённый текст · ${state.material.codePointCount(0, state.material.length)} символов")
                    if (!state.busy && state.error == null) {
                        Column(Modifier.selectableGroup()) {
                            ProcessingMode.entries.forEach { mode ->
                                Row(
                                    Modifier.fillMaxWidth().selectable(
                                        selected = state.mode == mode, enabled = mode.available,
                                        role = Role.RadioButton, onClick = { flow.selectMode(mode) }
                                    ).padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(selected = state.mode == mode, onClick = null, enabled = mode.available)
                                    Text(mode.title + if (mode.available) "" else " — скоро",
                                        color = if (mode.available) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Text("Сейчас доступен краткий конспект.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (state.error == null || !state.canRetry) {
                        Button(onClick = { request(false) }, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth()) { Text("Создать") }
                    }
                }
                Screen.RESULT -> {
                    Text(state.mode.title, style = MaterialTheme.typography.titleMedium)
                    Text(state.result, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Button(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Результат", state.result))
                        copied = true
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (copied) "Скопировано" else "Копировать") }
                    OutlinedButton(onClick = flow::back, modifier = Modifier.fillMaxWidth()) { Text("Режимы") }
                }
            }
            if (state.busy) {
                CircularProgressIndicator()
                Text(if (state.screen == Screen.MATERIAL) "Проверяем связь…" else "Обрабатываем · ${state.mode.title}")
                if (longWait) Text("Обработка занимает больше времени. Можно подождать или отменить ожидание.")
                OutlinedButton(onClick = flow::cancel, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
            }
            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (state.canRetry && !state.busy) {
                    Button(onClick = { request(false, retry = true) }, modifier = Modifier.fillMaxWidth()) { Text("Повторить") }
                }
            }
            state.notice?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            if (state.screen == Screen.MODES && !state.busy) {
                OutlinedButton(onClick = flow::back, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
            }
        }
    }
}
