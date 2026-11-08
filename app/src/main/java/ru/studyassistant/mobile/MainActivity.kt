package ru.studyassistant.mobile
import kotlinx.coroutines.Dispatchers
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import ru.studyassistant.mobile.ui.theme.StudySpacing
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.studyassistant.mobile.api.StudyApi
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settings = ServerSettings(application)
    var serverAddress by mutableStateOf(settings.address)
        private set
    private val api get() = StudyApi(serverAddress)
    fun saveServerAddress(value: String) {
        check(!flow.state.value.busy) { "Дождитесь окончания запроса." }
        serverAddress = settings.save(value)
        flow.serverChanged()
    }
    val flow = StudyFlow(scope,
        process = { text, mode -> withContext(Dispatchers.IO) { api.process(text, mode.name) } },
        health = { withContext(Dispatchers.IO) { api.health() } },
        loadModes = { withContext(Dispatchers.IO) { api.modes() } }
    )
    override fun onCleared() { scope.cancel() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model = ViewModelProvider(this)[StudyViewModel::class.java]
        setContent { AIStudyAssistantTheme {
            StudyApp(model.flow, model.serverAddress, model::saveServerAddress)
        } }
    }
}

@Composable
fun StudyApp(flow: StudyFlow, serverAddress: String? = null, onSaveServer: (String) -> Unit = {}) {
    val state by flow.state.collectAsState()
    val context = LocalContext.current
    var longWait by remember { mutableStateOf(false) }
    LaunchedEffect(state.busy) {
        longWait = false
        if (state.busy) { delay(20_000); longWait = true }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        flow.permissionMessage(it)
    }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val fileName = getFileName(contentResolver, uri) ?: "document"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                flow.selectFile(name = fileName, bytes = bytes)
            }
        }
    }
    fun request(health: Boolean, retry: Boolean = false, modes: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 37 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else if (retry) flow.retry() else if (modes) flow.showModes() else if (health) flow.checkHealth() else flow.submit()
    }
    BackHandler(state.screen != Screen.MATERIAL || state.busy) { flow.back() }
    Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
        Column(
            Modifier.padding(insets).imePadding().padding(16.dp)
                .fillMaxSize().then(if (state.screen == Screen.RESULT) Modifier else Modifier.verticalScroll(rememberScrollState())),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(when (state.screen) {
                Screen.MATERIAL -> "Загрузка материала"
                Screen.MODES -> "Выбор режима"
                Screen.RESULT -> "Результат"
            }, style = MaterialTheme.typography.headlineSmall)
            when (state.screen) {
                Screen.MATERIAL -> {
                    serverAddress?.let {
                        ServerSettingsButton(it, !state.busy, onSaveServer)
                    }
                    Text("Вставьте учебный текст или прикрепите файл:")
                    Button(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("text/plain", "application/pdf"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Выбрать файл (TXT, PDF)")
                    }

                    state.fileName?.let { fileName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = fileName,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = flow::clearFile) {
                                Text("Убрать")
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.material, onValueChange = flow::editMaterial,
                        modifier = Modifier.fillMaxWidth(), label = { Text("Учебный текст") },
                        supportingText = { Text("${state.material.codePointCount(0, state.material.length)} / 10 000 символов") },
                        minLines = 5, maxLines = 10, enabled = !state.busy
                    )
                    Button(onClick = { request(false, modes = true) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Выбрать режим")
                    }
                    OutlinedButton(onClick = { request(true) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Проверить связь с сервером")
                    }
                }
                Screen.MODES -> {
                    Text("Введённый текст · ${state.material.codePointCount(0, state.material.length)} символов")
                    state.modesError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (!state.modesLoaded && !state.busy) {
                        OutlinedButton(onClick = { request(false, modes = true) }) { Text("Загрузить режимы снова") }
                    }
                    if (!state.busy && state.error == null && state.modesLoaded) {
                        Column(Modifier.selectableGroup()) {
                            state.modes.forEach { mode ->
                                Row(
                                    Modifier.fillMaxWidth().selectable(
                                        selected = state.mode == mode, enabled = true,
                                        role = Role.RadioButton, onClick = { flow.selectMode(mode) }
                                    ).padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(selected = state.mode == mode, onClick = null, enabled = true)
                                    Column {
                                        Text(mode.title)
                                        Text(mode.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    if (state.modesLoaded && (state.error == null || !state.canRetry)) {
                        Button(onClick = { request(false) }, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth()) { Text("Создать") }
                    }
                }
                Screen.RESULT -> {
                    ResultContent(state.result, state.resultMode.title, flow::back,
                        flow::newMaterial, Modifier.weight(1f))
                }
            }
            if (state.busy) {
                Column(Modifier.fillMaxWidth().padding(vertical = StudySpacing.screen),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(StudySpacing.screen)) {
                CircularProgressIndicator(Modifier.size(StudySpacing.progress))
                Text(if (state.loadingModes) "Загружаем режимы…" else if (state.screen == Screen.MATERIAL) "Проверяем связь…" else "Обрабатываем · ${state.mode.title}")
                if (longWait) Text("Обработка занимает больше времени. Можно подождать или отменить ожидание.")
                OutlinedButton(onClick = flow::cancel, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
                }
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
private fun getFileName(resolver: ContentResolver, uri: Uri): String? {
    var name: String? = null
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
        }
    }
    return name ?: uri.lastPathSegment
}