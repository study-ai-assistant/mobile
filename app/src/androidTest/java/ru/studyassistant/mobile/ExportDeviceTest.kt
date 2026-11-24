package ru.studyassistant.mobile

import android.content.ContentValues
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme

class ExportDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun writesActualUtf8FileAndReportsUnwritableDestination() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "export-check.md")
            put(MediaStore.Downloads.MIME_TYPE, "text/markdown")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/StudyAssistantQA")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
        val text = formatExportContent("Краткий конспект", "Русский текст и формулы: CO₂ → O₂ 🌱", ExportFormat.MARKDOWN)
        try {
            assertTrue(writeUtf8TextToUri(context, uri, text).isSuccess)
            assertEquals(text, resolver.openInputStream(uri)!!.bufferedReader(Charsets.UTF_8).use { it.readText() })
            val readOnly = android.net.Uri.parse("android.resource://${context.packageName}/${R.font.jetbrains_mono_regular}")
            assertTrue(writeUtf8TextToUri(context, readOnly, text).isFailure)
        } finally { resolver.delete(uri, null, null) }
    }
    @Test fun actualSystemSaveDialogCanSaveAndReturnToResult() {
        compose.setContent { AIStudyAssistantTheme { ResultContent("Проверка системного сохранения файла.", "Проверка экспорта", {}, {}) } }
        compose.onNodeWithTag("save-file-button").performClick()
        compose.onNodeWithText("TXT (.txt)").performClick()
        compose.onNodeWithTag("confirm-save-format").performClick()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun saveNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (root == null) return null
            val text = root.text?.toString().orEmpty()
            if (text.equals("Save", true) || text.equals("Сохранить", true)) {
                if (root.isClickable && root.isEnabled) return root
            }
            for (i in 0 until root.childCount) saveNode(root.getChild(i))?.let { return it }
            return null
        }
        var save: AccessibilityNodeInfo? = null
        compose.waitUntil(15_000) { save = saveNode(automation.rootInActiveWindow); save != null }
        assertTrue(save!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("save-success-card").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Файл сохранён", substring = true).assertExists()
        compose.onNodeWithTag("save-success-done").performClick()
        compose.onNodeWithTag("save-file-button").assertIsDisplayed()
    }
}

