package ru.studyassistant.mobile

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Opt-in: настоящий локальный backend, без подмены StudyApi или ответов LLM. */
class LiveBackendTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val arguments get() = InstrumentationRegistry.getArguments()
    private val flow get() = ViewModelProvider(compose.activity)[StudyViewModel::class.java].flow

    @Before fun enableOnlyWhenRequested() {
        assumeTrue("Требуется отдельно запущенный backend", arguments.getString("liveBackend") == "true")
        if (Build.VERSION.SDK_INT >= 37) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                compose.activity.packageName, Manifest.permission.ACCESS_LOCAL_NETWORK
            )
        }
    }

    @Test fun healthFromApp() {
        compose.onNodeWithText("Проверить связь с сервером").performScrollTo().performClick()
        compose.waitUntil(20_000) { flow.state.value.notice != null || flow.state.value.error != null }
        assertNull(flow.state.value.error)
        assertEquals("Сервер доступен: status = ok", flow.state.value.notice)
        capture("health", flow.state.value.notice!!)
    }

    @Test fun processTextThroughRealBackend() {
        val sample = "Митохондрии обеспечивают клетку энергией. В них происходит клеточное дыхание. " +
            "Полученная энергия запасается в молекулах АТФ и используется для работы клетки."
        compose.onNodeWithText("Учебный текст").performTextInput(sample)
        compose.onNodeWithText("Выбрать режим").performScrollTo().performClick()
        compose.onNodeWithText("Создать").performScrollTo().performClick()
        compose.waitUntil(100_000) {
            flow.state.value.screen == Screen.RESULT || flow.state.value.error != null
        }
        val state = flow.state.value
        if (arguments.getString("expectLlmSuccess") == "true") {
            assertNull("Нужен настоящий ответ LLM: ${state.error}", state.error)
            assertEquals(Screen.RESULT, state.screen)
            assertTrue(state.result.isNotBlank())
            capture("llm-success", state.result)
            compose.onNodeWithText("Копировать").performScrollTo().performClick()
            compose.onNodeWithText("Скопировано").assertExists()
            compose.onNodeWithText("Режимы").performScrollTo().performClick()
        } else {
            // Этот режим запускается только для сервера с заведомо пустым ключом.
            assertEquals(Screen.MODES, state.screen)
            assertEquals("Сервис обработки текста временно недоступен. Попробуйте позже.", state.error)
            capture("llm-unconfigured", state.error!!)
            compose.onNodeWithText("Повторить").assertExists()
        }
        compose.onNodeWithText("Назад").performScrollTo().performClick()
        compose.onNodeWithText("Учебный текст").assertTextContains(sample)
    }

    @Test fun serverUnavailable() {
        assumeTrue("Сервер должен быть отдельно остановлен", arguments.getString("backendOffline") == "true")
        val sample = "Учебный текст сохраняется при ошибке сети."
        compose.onNodeWithText("Учебный текст").performTextInput(sample)
        compose.onNodeWithText("Выбрать режим").performScrollTo().performClick()
        compose.onNodeWithText("Создать").performScrollTo().performClick()
        compose.waitUntil(20_000) { flow.state.value.error != null }
        assertFalse(flow.state.value.busy)
        assertEquals(Screen.MODES, flow.state.value.screen)
        assertEquals("Не удалось связаться с сервером. Проверьте сеть и запуск backend.", flow.state.value.error)
        compose.onNodeWithText("Повторить").assertExists()
        capture("server-unavailable", flow.state.value.error!!)
        compose.onNodeWithText("Назад").performScrollTo().performClick()
        compose.onNodeWithText("Учебный текст").assertTextContains(sample)
    }

    private fun capture(name: String, message: String) {
        compose.waitForIdle()
        val directory = arguments.getString("additionalTestOutputDir")?.let { File(it) }
            ?: File(compose.activity.getExternalFilesDir(null), "integration-evidence")
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        File(directory, "$name.json").writeText(JSONObject()
            .put("time", Instant.now().toString()).put("scenario", name)
            .put("backend", ru.studyassistant.mobile.api.ServerConfig.BASE_URL)
            .put("message", message).put("realBackend", true)
            .put("llmSuccess", name == "llm-success").toString(2))
    }
}
