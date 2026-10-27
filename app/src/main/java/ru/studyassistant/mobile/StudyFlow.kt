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

enum class ProcessingMode(val title: String, val available: Boolean = false) {
    SHORT_SUMMARY("Краткий конспект", true),
    DETAILED_SUMMARY("Подробный конспект"),
    SIMPLE_EXPLANATION("Простое объяснение"),
    KEY_TERMS("Ключевые термины"),
    SELF_CHECK_QUESTIONS("Самопроверка")
}

data class StudyState(
    val screen: Screen = Screen.MATERIAL,
    val material: String = "",
    val mode: ProcessingMode = ProcessingMode.SHORT_SUMMARY,
    val busy: Boolean = false,
    val result: String = "",
    val error: String? = null,
    val notice: String? = null
)

/** Все методы вызываются на UI-потоке; зависимости запросов подменяются в тестах. */
class StudyFlow(
    private val scope: CoroutineScope,
    private val process: suspend (String, ProcessingMode) -> String,
    private val health: suspend () -> String
) {
    private val mutableState = MutableStateFlow(StudyState())
    val state: StateFlow<StudyState> = mutableState.asStateFlow()
    private var job: Job? = null
    private var generation = 0

    fun editMaterial(text: String) {
        if (!state.value.busy) mutableState.value = state.value.copy(
            material = text, result = "", error = null, notice = null
        )
    }

    fun showModes() {
        if (state.value.busy) return
        val error = validateMaterial(state.value.material)
        mutableState.value = state.value.copy(
            screen = if (error == null) Screen.MODES else Screen.MATERIAL,
            error = error, notice = null
        )
    }

    fun selectMode(mode: ProcessingMode) {
        if (!state.value.busy && mode.available) {
            mutableState.value = state.value.copy(mode = mode, error = null, notice = null)
        }
    }

    fun permissionMessage(granted: Boolean) {
        mutableState.value = state.value.copy(
            error = if (granted) null else "Разрешите доступ к устройствам поблизости в настройках приложения → Разрешения.",
            notice = if (granted) "Доступ разрешён. Нажмите нужную кнопку ещё раз." else null
        )
    }

    fun submit() = request(healthOnly = false)
    fun checkHealth() = request(healthOnly = true)

    private fun request(healthOnly: Boolean) {
        val submitted = state.value
        if (submitted.busy) return
        if (!healthOnly) {
            val error = validateMaterial(submitted.material)
            if (error != null || !submitted.mode.available) {
                mutableState.value = submitted.copy(error = error ?: "Режим пока недоступен.")
                return
            }
        }
        val requestGeneration = ++generation
        mutableState.value = submitted.copy(busy = true, error = null, notice = null)
        job = scope.launch {
            try {
                val response = if (healthOnly) health() else process(submitted.material, submitted.mode)
                if (generation == requestGeneration) mutableState.value = state.value.copy(
                    screen = if (healthOnly) submitted.screen else Screen.RESULT,
                    result = if (healthOnly) submitted.result else response,
                    notice = if (healthOnly) response else null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (generation == requestGeneration) {
                    mutableState.value = state.value.copy(error = e.message ?: "Не удалось выполнить запрос.")
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
            busy = false, error = null,
            notice = "Ожидание отменено. Сервер может продолжать обработку."
        )
    }

    fun back() {
        if (state.value.busy) { cancel(); return }
        mutableState.value = state.value.copy(
            screen = if (state.value.screen == Screen.RESULT) Screen.MODES else Screen.MATERIAL,
            error = null, notice = null
        )
    }
}
