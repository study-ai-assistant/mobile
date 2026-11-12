package ru.studyassistant.mobile
import ru.studyassistant.mobile.ui.theme.StudyButton
import kotlinx.coroutines.Dispatchers
import android.Manifest
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.studyassistant.mobile.api.StudyApi
import android.net.Uri
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme
import ru.studyassistant.mobile.ui.theme.StudySpacing
import ru.studyassistant.mobile.ui.theme.ErrorMessage

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
        loadModes = { withContext(Dispatchers.IO) { api.modes() } },
        uploadTxt = { name, bytes, mode -> withContext(Dispatchers.IO) { api.uploadTxt(name, bytes, mode.name) } }
    )
    fun importFile(uri: Uri) = flow.readFile {
        withContext(Dispatchers.IO) { readMaterialFile(getApplication<Application>().contentResolver, uri) }
    }
    override fun onCleared() { scope.cancel() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model = ViewModelProvider(this)[StudyViewModel::class.java]
        setContent { AIStudyAssistantTheme {
            StudyApp(model.flow, model.serverAddress, model::saveServerAddress, model::importFile)
        } }
    }
}

@Composable
fun StudyApp(flow: StudyFlow, serverAddress: String? = null, onSaveServer: (String) -> Unit = {}, onPickFile: (Uri) -> Unit = {}) {
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
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onPickFile)
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
            Modifier.padding(insets).imePadding().padding(StudySpacing.screen)
                .fillMaxSize().then(if (state.screen == Screen.RESULT) Modifier else Modifier.verticalScroll(rememberScrollState())),
            verticalArrangement = Arrangement.spacedBy(StudySpacing.gap)
        ) {
            if (state.screen != Screen.MATERIAL) Text(when (state.screen) {
                Screen.MATERIAL -> "Загрузка материала"
                Screen.MODES -> "Выбор режима"
                Screen.RESULT -> "Результат"
            }, style = MaterialTheme.typography.headlineSmall)
            when (state.screen) {
                Screen.MATERIAL -> {
                    MaterialContent(state, flow::editMaterial,
                        { filePicker.launch(arrayOf("text/plain")) }, flow::clearFile,
                        { request(false, modes = true) })
                    OutlinedButton(onClick = { request(true) }, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()) { Text("Проверить связь с сервером") }
                    serverAddress?.let { ServerSettingsButton(it, !state.busy, onSaveServer) }
                }
                Screen.MODES -> {
                    ModesContent(state, flow::selectMode,
                        { request(false, modes = true) }, { request(false) })
                }
                Screen.RESULT -> {
                    ResultContent(state.result, state.resultMode.title, flow::back,
                        flow::newMaterial, Modifier.weight(1f))
                }
            }
            if (state.busy && !state.loadingFile) {
                Column(Modifier.fillMaxWidth().padding(vertical = StudySpacing.screen),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(StudySpacing.screen)) {
                CircularProgressIndicator(Modifier.size(StudySpacing.progress))
                Text(if (state.loadingModes) "Загружаем режимы…" else if (state.screen == Screen.MATERIAL) "Проверяем связь…" else "Обрабатываем · ${state.mode.title}")
                if (longWait) Text("Обработка занимает больше времени. Можно подождать или отменить ожидание.")
                OutlinedButton(onClick = flow::cancel, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
                }
            }
            if (state.loadingFile) OutlinedButton(onClick = flow::cancel) { Text("Отмена") }
            state.error?.let { error ->
                ErrorMessage(error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (state.canRetry && !state.busy) {
                    StudyButton(onClick = { request(false, retry = true) }, modifier = Modifier.fillMaxWidth()) { Text("Повторить") }
                }
            }
            state.notice?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            if (state.screen == Screen.MODES && !state.busy) {
                OutlinedButton(onClick = flow::back, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
            }
        }
    }
}
