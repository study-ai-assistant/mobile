package ru.studyassistant.mobile

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class ResultContentTest {
    @get:Rule val compose = createComposeRule()
    @Test fun longResultScrollsCopiesInFullAndHasNavigation() {
        val result = (1..150).joinToString("\n") { "Абзац $it. Учебный текст на русском языке." }
        var modes = false
        var newMaterial = false
        compose.setContent { AIStudyAssistantTheme {
            ResultContent(result, "Подробный конспект", { modes = true }, { newMaterial = true })
        } }
        compose.onNodeWithText("Подробный конспект").assertIsDisplayed()
        compose.onNodeWithTag("result-text").assertTextEquals(result)
        compose.onNodeWithTag("result-scroll").performTouchInput { swipeUp() }
        val range = compose.onNodeWithTag("result-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(range.value() > 0f)
        compose.onNodeWithText("Копировать").assertIsDisplayed().performClick()
        compose.onNodeWithText("Скопировано").assertIsDisplayed()
        compose.runOnIdle {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(result, clipboard.primaryClip!!.getItemAt(0).text.toString())
        }
        compose.onNodeWithText("Режимы").performClick()
        compose.onNodeWithText("Новый материал").performClick()
        compose.runOnIdle { assertTrue(modes); assertTrue(newMaterial) }
    }
}
