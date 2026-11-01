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
    SHORT_SUMMARY("Краткий конспект", "Главные мысли материала в нескольких предложениях."),
    DETAILED_SUMMARY("Подробный конспект", "Развёрнутый конспект с основными подробностями."),
    SIMPLE_EXPLANATION("Объяснение простыми словами", "Понятное объяснение сложных идей."),
    KEY_TERMS("Ключевые термины", "Основные понятия и их определения."),
    SELF_CHECK_QUESTIONS("Вопросы для самопроверки", "Вопросы для проверки понимания материала.");

    companion object {
        fun fromId(id: String): ProcessingMode? = entries.find { it.name == id }
    }
}

data class StudyState(
    val screen: Screen = Screen.MATERIAL,
    val material: String = "",
    val mode: ProcessingMode = ProcessingMode.SHORT_SUMMARY,
    val busy: Boolean = false,
    val loadingModes: Boolean = false,
    val modes: List<ProcessingMode> = listOf(ProcessingMode.SHORT_SUMMARY),
    val modesLoaded: Boolean = true,
    val modesError: String? = null,
    val result: String = "",
    val resultMode: ProcessingMode = ProcessingMode.SHORT_SUMMARY,
    val error: String? = null,
    val notice: String? = null,
    val canRetry: Boolean = false
)

/** Все методы вызываются на UI-потоке; зависимости запросов подменяются в тестах. */
class StudyFlow(
    private val scope: CoroutineScope,
    private val process: suspend (String, ProcessingMode) -> String,
    private val health: suspend () -> String,
    private val loadModes: (suspend () -> List<ProcessingMode>)? = null
) {
    private val mutableState = MutableStateFlow(initialState())
    val state: StateFlow<StudyState> = mutableState.asStateFlow()
    private var job: Job? = null
    private var generation = 0
    private data class Request(val text: String, val mode: ProcessingMode, val healthOnly: Boolean)
    private var lastRequest: Request? = null

    private fun initialState() = StudyState(
        modes = if (loadModes == null) listOf(ProcessingMode.SHORT_SUMMARY) else emptyList(),
        modesLoaded = loadModes == null
    )

    fun editMaterial(text: String) {
        if (!state.value.busy) mutableState.value = state.value.copy(
            material = text, result = "", error = null, notice = null, canRetry = false
        )
    }

    fun showModes() {
        if (state.value.busy) return
        val error = validateMaterial(state.value.material)
        mutableState.value = state.value.copy(
            screen = if (error == null) Screen.MODES else Screen.MATERIAL,
            error = error, notice = null, canRetry = false
        )
        if (error == null && !state.value.modesLoaded) refreshModes()
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
            mutableState.value = state.value.copy(mode = mode, error = null, notice = null, canRetry = false)
        }
    }

    fun permissionMessage(granted: Boolean) {
        mutableState.value = state.value.copy(
            error = if (granted) null else "Разрешите доступ к устройствам поблизости в настройках приложения → Разрешения.",
            notice = if (granted) "Доступ разрешён. Нажмите нужную кнопку ещё раз." else null
        )
    }

    fun submit() = request(Request(state.value.material, state.value.mode, healthOnly = false))
    fun checkHealth() = request(Request(state.value.material, state.value.mode, healthOnly = true))

    fun retry() {
        if (state.value.canRetry && !state.value.busy) lastRequest?.let { request(it) }
    }

    private fun request(request: Request) {
        val healthOnly = request.healthOnly
        val submitted = state.value
        if (submitted.busy) return
        if (!healthOnly) {
            val error = validateMaterial(request.text)
            if (error != null || !submitted.modesLoaded || request.mode !in submitted.modes) {
                mutableState.value = submitted.copy(error = error ?: "Режим пока недоступен.", canRetry = false)
                return
            }
        }
        val requestGeneration = ++generation
        lastRequest = request
        mutableState.value = submitted.copy(busy = true, error = null, notice = null, canRetry = false)
        job = scope.launch {
            try {
                val response = if (healthOnly) health() else process(request.text, request.mode)
                if (generation == requestGeneration) mutableState.value = state.value.copy(
                    screen = if (healthOnly) submitted.screen else Screen.RESULT,
                    result = if (healthOnly) submitted.result else response,
                    resultMode = if (healthOnly) submitted.resultMode else request.mode,
                    notice = if (healthOnly) response else null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (generation == requestGeneration) {
                    mutableState.value = state.value.copy(
                        error = e.message ?: "Не удалось выполнить запрос.", canRetry = true
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
            busy = false, loadingModes = false, error = null, canRetry = false,
            notice = "Ожидание отменено. Сервер может продолжать обработку."
        )
    }

    fun newMaterial() {
        if (!state.value.busy) mutableState.value = initialState()
    }

    fun back() {
        if (state.value.busy) { cancel(); return }
        mutableState.value = state.value.copy(
            screen = if (state.value.screen == Screen.RESULT) Screen.MODES else Screen.MATERIAL,
            error = null, notice = null, canRetry = false
        )
    }
}
