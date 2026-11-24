package ru.studyassistant.mobile

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class FinalScreensUiTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        File(dir, "final-$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
    @Test fun generationCanBeCancelledAndRetried() {
        var cancelled = false
        var retried = false
        var failed by mutableStateOf(false)
        compose.setContent { AIStudyAssistantTheme {
            GenerationContent(StudyState(busy = !failed, error = if (failed) "Сервер недоступен. Попробуйте ещё раз." else null, canRetry = failed),
                false, { cancelled = true }, { retried = true }, {})
        } }
        compose.onNodeWithText("Отмена").assertIsDisplayed().performClick()
        compose.runOnIdle { check(cancelled) }
        capture("loading")
        compose.runOnIdle { failed = true }
        compose.onNodeWithText("Повторить").performClick()
        compose.runOnIdle { check(retried) }
        capture("server-error")
    }
    @Test fun progressHasRealPartCountAndCancel() {
        var cancelled = false
        compose.setContent { AIStudyAssistantTheme {
            LongProgressContent(LongProcessingState(running = true, progress = LongProgress(2, 5, 240, queued = false)),
                { cancelled = true }, {}, materialName = "Лекция_ИИ.txt")
        } }
        compose.onNodeWithText("Часть 3 из 5").assertExists()
        compose.onNodeWithText("Отменить").performScrollTo().performClick()
        compose.runOnIdle { check(cancelled) }
        capture("progress")
    }
    @Test fun resultHasExportActionsAndScrollableText() {
        compose.setContent { AIStudyAssistantTheme {
            ResultContent("Основная идея\nФотосинтез преобразует энергию света в химическую энергию.\n\nСветовая стадия\nОбразуется кислород.\n\nТемновая стадия\nУглекислый газ используется для синтеза органических веществ.", "Краткий конспект", {}, {})
        } }
        compose.onNodeWithTag("mode-star", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("save-file-button").assertIsDisplayed()
        capture("result")
    }
}
