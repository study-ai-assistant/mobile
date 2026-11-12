package ru.studyassistant.mobile

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CompletableDeferred
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import ru.studyassistant.mobile.api.ApiException
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class StudyFlowUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var flow: StudyFlow

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(instrumentation.targetContext.getExternalFilesDir(null), "theme-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test fun entersTextShowsModesResultAndReturnsWithoutLosingText() {
        compose.setContent {
            val scope = rememberCoroutineScope()
            flow = androidx.compose.runtime.remember {
                StudyFlow(scope, { _, _ -> "Проверочный конспект" }, { "ok" },
                    loadModes = { ProcessingMode.entries })
            }
            AIStudyAssistantTheme { StudyApp(flow) }
        }
        capture("material")
        compose.onNodeWithText("Выбрать режим").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Учебный текст").performTextInput("Материал для проверки")
        compose.onNodeWithText("Выбрать режим").performScrollTo().performClick()
        compose.onNodeWithText("Подробный конспект").assertExists()
        capture("modes")
        // Сетевое разрешение проверяется вручную; здесь проверяем навигацию с тестовым ответом.
        compose.runOnIdle { flow.submit() }
        compose.onNodeWithText("Проверочный конспект").assertExists()
        capture("result")
        compose.onNodeWithText("Копировать").performClick()
        compose.onNodeWithText("Скопировано").assertExists()
        compose.onNodeWithText("Режимы").performClick()
        compose.onNodeWithText("Назад").performScrollTo().performClick()
        compose.onNodeWithText("Учебный текст").assertTextContains("Материал для проверки")
    }

    @Test fun loadingCanBeCancelledAndServerErrorKeepsMaterial() {
        val pending = CompletableDeferred<String>()
        var attempt = 0
        compose.setContent {
            val scope = rememberCoroutineScope()
            flow = androidx.compose.runtime.remember {
                StudyFlow(scope, { _, _ ->
                    when (++attempt) {
                        1 -> pending.await()
                        2 -> throw ApiException("Ошибка сервера")
                        else -> "Результат после повтора"
                    }
                }, { "ok" })
            }
            AIStudyAssistantTheme { StudyApp(flow) }
        }
        compose.runOnIdle {
            flow.editMaterial("Сохранённый материал")
            flow.showModes()
            flow.submit()
        }
        compose.onNodeWithText("Создать").assertIsNotEnabled()
        capture("waiting")
        compose.onNodeWithText("Отмена").performScrollTo().performClick()
        compose.onNodeWithText("Создать").assertExists()
        compose.runOnIdle { flow.submit() }
        compose.onNodeWithText("Ошибка сервера").assertExists()
        compose.onNodeWithText("Повторить").assertExists()
        capture("error")
        compose.runOnIdle { flow.retry() }
        compose.onNodeWithText("Результат после повтора").assertExists()
        compose.onNodeWithText("Режимы").performClick()
        compose.onNodeWithText("Назад").performScrollTo().performClick()
        compose.onNodeWithText("Учебный текст").assertTextContains("Сохранённый материал")
    }
}
