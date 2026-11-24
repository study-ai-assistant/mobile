package ru.studyassistant.mobile

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.studyassistant.mobile.api.ApiException
import ru.studyassistant.mobile.api.validateMaterial

enum class Screen { MATERIAL, MODES, RESULT }

enum class ProcessingMode(val title: String, val description: String) {
    SHORT_SUMMARY("Краткий конспект", "Объёмные лекции в короткие тезисы"),
    DETAILED_SUMMARY("Подробный конспект", "Полный разбор со всеми деталями"),
    SIMPLE_EXPLANATION("Объяснение простым языком", "Сложные темы простым языком"),
    KEY_TERMS("Ключевые термины", "Главные понятия с краткими определениями"),
    SELF_CHECK_QUESTIONS("Вопросы для самопроверки", "Готовые тесты для проверки знаний");

    companion object {
        fun fromId(id: String): ProcessingMode? = entries.find { it.name == id }
    }
}

data class StudyState(
    val screen: Screen = Screen.MATERIAL,
    val material: String = "",
    val fileName: String? = null,
    val fileBytes: ByteArray? = null,
    val mode: ProcessingMode = ProcessingMode.SHORT_SUMMARY,
    val busy: Boolean = false,
    val loadingFile: Boolean = false,
    val loadingModes: Boolean = false,
    val modes: List<ProcessingMode> = listOf(ProcessingMode.SHORT_SUMMARY),
    val modesLoaded: Boolean = true,
    val modesError: String? = null,
    val result: String = "",
    val resultMode: ProcessingMode = ProcessingMode.SHORT_SUMMARY,
    val error: String? = null,
    val notice: String? = null,
    val longProgress: LongProgress? = null, val longError: Boolean = false, val canRetry: Boolean = false
)

/** Все методы вызываются на UI-потоке; зависимости запросов подменяются в тестах. */
class StudyFlow(
    private val scope: CoroutineScope,
    private val process: suspend (String, ProcessingMode) -> String,
    private val health: suspend () -> String,
    private val loadModes: (suspend () -> List<ProcessingMode>)? = null,
    private val uploadTxt: (suspend (String, ByteArray, ProcessingMode) -> String)? = null, private val processMaterial: (suspend (String, String?, ByteArray?, ProcessingMode, (LongProgress) -> Unit) -> String)? = null
) {
    private val mutableState = MutableStateFlow(initialState())
    val state: StateFlow<StudyState> = mutableState.asStateFlow()
    private var job: Job? = null
    private var generation = 0
    private data class Request(
        val text: String,
        val fileName: String? = null,
        val fileBytes: ByteArray? = null,
        val mode: ProcessingMode,
        val healthOnly: Boolean = false
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Request) return false

            if (text != other.text) return false
            if (fileName != other.fileName) return false
            if (fileBytes != null) {
                if (other.fileBytes == null) return false
                if (!fileBytes.contentEquals(other.fileBytes)) return false
            } else if (other.fileBytes != null) return false
            if (mode != other.mode) return false
            if (healthOnly != other.healthOnly) return false

            return true
        }

        override fun hashCode(): Int {
            var result = text.hashCode()
            result = 31 * result + (fileName?.hashCode() ?: 0)
            result = 31 * result + (fileBytes?.contentHashCode() ?: 0)
            result = 31 * result + mode.hashCode()
            result = 31 * result + healthOnly.hashCode()
            return result
        }
    }
    private var lastRequest: Request? = null

    private fun initialState() = StudyState(
        modes = if (loadModes == null) listOf(ProcessingMode.SHORT_SUMMARY) else emptyList(),
        modesLoaded = loadModes == null
    )

    fun editMaterial(text: String) {
        if (!state.value.busy) mutableState.value = state.value.copy(
            material = text, fileName = null, fileBytes = null, result = "", error = null, notice = null, canRetry = false, longProgress = null, longError = false
        )
    }
    fun serverChanged() {
        if (state.value.busy) return
        generation++
        lastRequest = null
        val previous = state.value
        mutableState.value = initialState().copy(material = previous.material,
            fileName = previous.fileName, fileBytes = previous.fileBytes,
            notice = "Адрес сохранён. Проверьте связь с сервером.")
    }

    fun selectFile(name: String, bytes: ByteArray) {
        if (state.value.busy) return
        try { validateFileName(name); validateFileSize(bytes.size.toLong())
            if (bytes.isEmpty()) throw ApiException("Файл пуст. Выберите другой файл.")
        } catch (e: ApiException) {
            mutableState.value = state.value.copy(fileName = null, fileBytes = null, error = e.message, canRetry = false)
            return
        }
        mutableState.value = state.value.copy(
            fileName = name,
            fileBytes = bytes,
            material = "", // очищаем ручной ввод, если выбран файл
            result = "",
            error = null,
            notice = null,
            canRetry = false
        )
    }

    fun readFile(read: suspend () -> MaterialFile) {
        if (state.value.busy) return
        val requestGeneration = ++generation
        mutableState.value = state.value.copy(busy = true, loadingFile = true, fileName = null, fileBytes = null, error = null, notice = null, canRetry = false, longProgress = null, longError = false)
        job = scope.launch {
            try {
                val file = read()
                validateFileName(file.name)
                validateFileSize(file.bytes.size.toLong())
                if (file.bytes.isEmpty()) throw ApiException("Файл пуст. Выберите другой файл.")
                if (generation == requestGeneration) mutableState.value = state.value.copy(
                    fileName = file.name, fileBytes = file.bytes, material = "", result = "")
            } catch (e: CancellationException) { throw e
            } catch (e: java.io.IOException) {
                if (generation == requestGeneration) mutableState.value = state.value.copy(
                    error = if (e is ApiException) e.message else "Не удалось прочитать файл. Выберите его снова.")
            } catch (_: SecurityException) {
                if (generation == requestGeneration) mutableState.value = state.value.copy(error = "Нет доступа к файлу. Выберите его снова.")
            } finally {
                if (generation == requestGeneration) mutableState.value = state.value.copy(busy = false, loadingFile = false)
            }
        }
    }

    fun clearFile() {
        if (state.value.busy) return
        mutableState.value = state.value.copy(
            fileName = null,
            fileBytes = null,
            error = null, result = "", notice = null, canRetry = false
        )
    }
    fun showModes() {
        if (state.value.busy) return
        val current = state.value
        if (current.error != null && current.screen == Screen.MATERIAL) return
        val error = if (current.fileBytes != null) {
            if (current.fileBytes.isEmpty()) "Файл пуст или не содержит текста." else null
        } else {
            validateMaterial(current.material)
        }
        mutableState.value = current.copy(
            screen = if (error == null) Screen.MODES else Screen.MATERIAL,
            error = error,
            notice = null,
            canRetry = false
        )
        if (error == null && !current.modesLoaded) refreshModes()
    }

    fun refreshModes() {
        val loader = loadModes ?: return
        if (state.value.busy) return
        val requestGeneration = ++generation
        mutableState.value = state.value.copy(busy = true, loadingModes = true, modesError = null)
        job = scope.launch {
            try {
                val modes = loader().distinct()
                if (modes.isEmpty()) throw ApiException("Сервер не вернул доступные режимы.")
                if (generation == requestGeneration) mutableState.value = state.value.copy(
                    modes = modes, modesLoaded = true,
                    mode = state.value.mode.takeIf { it in modes } ?: modes.first()
                )
            } catch (e: CancellationException) { throw e
            } catch (e: ApiException) {
                if (generation == requestGeneration) mutableState.value = state.value.copy(
                    modesLoaded = false, modesError = "Не удалось загрузить режимы. ${e.message}"
                )
            } finally {
                if (generation == requestGeneration) mutableState.value = state.value.copy(busy = false, loadingModes = false)
            }
        }
    }

    fun selectMode(mode: ProcessingMode) {
        if (!state.value.busy && mode in state.value.modes) {
            mutableState.value = state.value.copy(mode = mode, error = null, notice = null, canRetry = false, longProgress = null, longError = false)
        }
    }

    fun permissionMessage(granted: Boolean) {
        mutableState.value = state.value.copy(
            error = if (granted) null else "Разрешите доступ к устройствам поблизости в настройках приложения → Разрешения.",
            notice = if (granted) "Доступ разрешён. Нажмите нужную кнопку ещё раз." else null
        )
    }

    fun submit() = request(
        Request(
            text = state.value.material,
            fileName = state.value.fileName,
            fileBytes = state.value.fileBytes,
            mode = state.value.mode,
            healthOnly = false
        )
    )
    fun checkHealth() = request(
        Request(
            text = state.value.material,
            fileName = null,
            fileBytes = null,
            mode = state.value.mode,
            healthOnly = true
        )
    )

    fun retry() {
        if (state.value.canRetry && !state.value.busy) lastRequest?.let { request(it) }
    }

    private fun request(request: Request) {
        val healthOnly = request.healthOnly
        val submitted = state.value
        if (submitted.busy) return
        if (!healthOnly) {
            val hasFile = request.fileName != null && request.fileBytes != null
            val error = if (hasFile) {
                try { validateFileSize(request.fileBytes!!.size.toLong()); null } catch (e: ApiException) { e.message }
            } else validateMaterial(request.text)
            if (error != null || !submitted.modesLoaded || request.mode !in submitted.modes) {
                mutableState.value = submitted.copy(
                    error = error ?: "Режим пока недоступен.",
                    canRetry = false
                )
                return
            }
        }
        val requestGeneration = ++generation
        lastRequest = request
        mutableState.value = submitted.copy(busy = true, error = null, notice = null, canRetry = false, longProgress = null, longError = false)
        job = scope.launch {
            try {
                val response = when {
                    healthOnly -> health()
                    processMaterial != null -> processMaterial.invoke(request.text, request.fileName, request.fileBytes, request.mode) { progress ->
                        if (generation == requestGeneration) mutableState.value = state.value.copy(longProgress = progress)
                    }
                    request.fileName != null && request.fileBytes != null -> {
                        val upload = uploadTxt ?: error("uploadTxt не настроен")
                        upload(request.fileName, request.fileBytes, request.mode)
                    }
                    else -> process(request.text, request.mode)
                }
                if (generation == requestGeneration) mutableState.value = state.value.copy(
                    screen = if (healthOnly) submitted.screen else Screen.RESULT,
                    result = if (healthOnly) submitted.result else response,
                    resultMode = if (healthOnly) submitted.resultMode else request.mode,
                    notice = if (healthOnly) response else null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: LongMaterialException) {
                if (generation == requestGeneration) mutableState.value = state.value.copy(error = e.reason.presentation().message, canRetry = e.reason.presentation().canRetry, longError = true)
            } catch (e: ApiException) {
                if (generation == requestGeneration) {
                    mutableState.value = state.value.copy(
                        screen = if (!healthOnly && e.status in listOf(400, 413, 415, 422)) Screen.MATERIAL else state.value.screen,
                        error = e.message ?: "Не удалось выполнить запрос.",
                        canRetry = e.status == null || e.status >= 500 || e.status in listOf(408, 429)
                    )
                }
            } finally {
                if (generation == requestGeneration) mutableState.value = state.value.copy(busy = false)
            }
        }
    }

    fun cancel() {
        // Сервер может продолжать POST. Поздний ответ не должен менять новый экран/запрос.
        generation++
        job?.cancel()
        mutableState.value = state.value.copy(
            busy = false, loadingModes = false, loadingFile = false, error = null, canRetry = false, longProgress = null, longError = false,
            notice = if (state.value.loadingFile) "Выбор файла отменён." else "Ожидание отменено. Сервер может продолжать обработку."
        )
    }

    fun newMaterial() {
        if (!state.value.busy) mutableState.value = initialState()
    }

    fun back() {
        if (state.value.busy) { cancel(); return }
        mutableState.value = state.value.copy(
            screen = if (state.value.screen == Screen.RESULT) Screen.MODES else Screen.MATERIAL,
            error = null, notice = null, canRetry = false, longProgress = null, longError = false
        )
    }
}
