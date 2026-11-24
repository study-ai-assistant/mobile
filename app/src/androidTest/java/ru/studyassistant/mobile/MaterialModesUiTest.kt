package ru.studyassistant.mobile

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class MaterialModesUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var flow: StudyFlow
    private fun start() {
        compose.setContent {
            val scope = rememberCoroutineScope()
            flow = remember { StudyFlow(scope, { _, _ -> "Ответ" }, { "ok" }, loadModes = { ProcessingMode.entries }) }
            AIStudyAssistantTheme { StudyApp(flow) }
        }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), "screen35-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun materialValidationAndFileRemoval() {
        start()
        capture("material-empty")
        compose.onNodeWithText("Выбрать режим").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { flow.editMaterial("a".repeat(24001)) }
        compose.onNodeWithText("Допустимо не более 24 000 символов. Сократите текст.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Выбрать режим").assertIsNotEnabled()
        capture("material-error")
        compose.runOnIdle { flow.selectFile("Лекция.txt", byteArrayOf(1)) }
        compose.onNodeWithText("Лекция.txt").performScrollTo().assertIsDisplayed()
        capture("material-file")
        compose.onNodeWithContentDescription("Учебный текст").assertIsNotEnabled()
        compose.onNodeWithText("Выбрать режим").performScrollTo().assertIsEnabled()
        compose.onNodeWithContentDescription("Убрать файл").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Учебный текст").assertIsEnabled()
        compose.onNodeWithText("Выбрать режим").performScrollTo().assertIsNotEnabled()
    }
    @Test fun allModeCardsCanBeReachedAndSelectionIsVisible() {
        start()
        compose.runOnIdle { flow.editMaterial("Учебный материал"); flow.showModes() }
        capture("modes-top")
        for (mode in ProcessingMode.entries) {
            compose.onNodeWithText(mode.title).performScrollTo().performClick()
            compose.onNode(isSelectable() and hasText(mode.title, substring = true)).assertIsSelected()
        }
        capture("modes-bottom")
        compose.onNodeWithText("Создать").performScrollTo().assertIsDisplayed().assertIsEnabled()
        capture("create-button-visible")
        compose.onNodeWithText("Назад").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Учебный текст").assertTextContains("Учебный материал")
    }
}
