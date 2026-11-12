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
import ru.studyassistant.mobile.api.ApiException
import ru.studyassistant.mobile.api.MaterialErrorMessages
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class PdfErrorsUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun showsPdfMessagesAndKeepsFileForRetry() {
        lateinit var flow: StudyFlow
        var message = MaterialErrorMessages.NO_TEXT
        compose.setContent {
            val scope = rememberCoroutineScope()
            flow = remember { StudyFlow(scope, { _, _ -> "unused" }, { "ok" },
                uploadTxt = { _, _, _ -> throw ApiException(message) }) }
            AIStudyAssistantTheme { StudyApp(flow) }
        }
        val cases = mapOf("scan" to MaterialErrorMessages.NO_TEXT, "password" to MaterialErrorMessages.PASSWORD,
            "corrupted" to MaterialErrorMessages.CORRUPTED, "pages" to MaterialErrorMessages.PAGES,
            "file-size" to MaterialErrorMessages.FILE_SIZE, "text-length" to MaterialErrorMessages.TEXT_LENGTH)
        for ((name, text) in cases) {
            compose.runOnIdle {
                flow.newMaterial()
                message = text
                flow.selectFile("$name.pdf", byteArrayOf(1))
                flow.showModes(); flow.submit()
            }
            compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Повторить").performScrollTo().assertIsEnabled()
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            File(context.getExternalFilesDir(null), "pdf34-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            compose.runOnIdle { flow.retry() }
            compose.onNodeWithText(text).assertExists()
            compose.runOnIdle { check(flow.state.value.fileName == "$name.pdf") }
            compose.onNodeWithText("Назад").performScrollTo().performClick()
            compose.runOnIdle { check(flow.state.value.screen == Screen.MATERIAL) }
        }
    }
}
