package ru.studyassistant.mobile

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import ru.studyassistant.mobile.api.ApiException
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class StudyFlowUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var flow: StudyFlow

    @Test fun entersTextShowsModesResultAndReturnsWithoutLosingText() {
        compose.setContent {
            val scope = rememberCoroutineScope()
            flow = androidx.compose.runtime.remember {
                StudyFlow(scope, { _, _ -> "Проверочный конспект" }, { "ok" })
            }
            AIStudyAssistantTheme { StudyApp(flow) }
        }
        compose.onNodeWithText("Выбрать режим").performScrollTo().performClick()
        compose.onNodeWithText("Введите учебный текст.").assertExists()
        compose.onNodeWithText("Учебный текст").performTextInput("Материал для проверки")
        compose.onNodeWithText("Выбрать режим").performScrollTo().performClick()
        compose.onNodeWithText("Подробный конспект — скоро").assertIsNotEnabled()
        // Сетевое разрешение проверяется вручную; здесь проверяем навигацию с тестовым ответом.
        compose.runOnIdle { flow.submit() }
        compose.onNodeWithText("Проверочный конспект").assertExists()
        compose.onNodeWithText("Копировать").performScrollTo().performClick()
        compose.onNodeWithText("Скопировано").assertExists()
        compose.onNodeWithText("Режимы").performScrollTo().performClick()
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
                    if (++attempt == 1) pending.await() else throw ApiException("Ошибка сервера")
                }, { "ok" })
            }
            AIStudyAssistantTheme { StudyApp(flow) }
        }
        compose.runOnIdle {
            flow.editMaterial("Сохранённый материал")
            flow.showModes()
            flow.submit()
        }
        compose.onNodeWithText("Отмена").performScrollTo().performClick()
        compose.onNodeWithText("Создать").assertExists()
        compose.runOnIdle { flow.submit() }
        compose.onNodeWithText("Ошибка сервера").assertExists()
        compose.onNodeWithText("Повторить").assertExists()
        compose.onNodeWithText("Назад").performScrollTo().performClick()
        compose.onNodeWithText("Учебный текст").assertTextContains("Сохранённый материал")
    }
}
