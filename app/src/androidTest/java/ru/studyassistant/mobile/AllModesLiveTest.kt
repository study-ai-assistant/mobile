package ru.studyassistant.mobile

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import ru.studyassistant.mobile.api.StudyApi
import ru.studyassistant.mobile.ui.theme.AIStudyAssistantTheme
import java.io.File

/** Только явный запуск с -e liveModes true; реальные запросы расходуют квоту LLM. */
class AllModesLiveTest {
    @get:Rule val compose = createComposeRule()
    @Test fun allFiveModesReturnResults() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("liveModes") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= 37) instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName, Manifest.permission.ACCESS_LOCAL_NETWORK)
        val api = StudyApi(args.getString("backendUrl") ?: "http://10.0.2.2:8080")
        lateinit var flow: StudyFlow
        compose.setContent {
            val scope = rememberCoroutineScope()
            flow = androidx.compose.runtime.remember { StudyFlow(scope,
                { text, mode -> withContext(Dispatchers.IO) { api.process(text, mode.name) } },
                { withContext(Dispatchers.IO) { api.health() } },
                { withContext(Dispatchers.IO) { api.modes() } }) }
            AIStudyAssistantTheme { StudyApp(flow) }
        }
        compose.runOnIdle {
            flow.editMaterial("Митохондрии обеспечивают клетку энергией. В процессе клеточного дыхания они синтезируют АТФ. Эта энергия используется для работы клетки.")
            flow.showModes()
        }
        compose.waitUntil(20_000) { !flow.state.value.busy }
        assertEquals(5, flow.state.value.modes.size)
        for (mode in ProcessingMode.entries) {
            compose.onNodeWithText(mode.title).performScrollTo().performClick()
            compose.onNodeWithText("Создать").performScrollTo().performClick()
            compose.waitUntil(100_000) { !flow.state.value.busy }
            assertNull(flow.state.value.error)
            assertEquals(Screen.RESULT, flow.state.value.screen)
            assertEquals(mode, flow.state.value.resultMode)
            assertTrue(flow.state.value.result.isNotBlank())
            compose.onNodeWithText(mode.title).assertIsDisplayed()
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            File(context.getExternalFilesDir(null), "mode-${mode.name}.png").outputStream().use {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            screenshot.recycle()
            compose.onNodeWithText("Режимы").performClick()
        }
    }
}
