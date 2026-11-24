package ru.studyassistant.mobile

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import ru.studyassistant.mobile.ui.theme.*

/** UI fixtures only: deterministic screenshots of production composables, not proof of live LLM. */
class DesignGalleryTest {
    @get:Rule val compose = createComposeRule()
    @Test fun captureEveryDesignState() {
        var screen by mutableStateOf("01-material-empty")
        val text = "Фотосинтез — процесс, при котором растения превращают световую энергию в химическую. При этом из воды и углекислого газа образуются органические вещества и кислород."
        val result = "Основная идея\n$text\n\nДве стадии\nСветовая фаза идёт на мембранах тилакоидов: хлорофилл поглощает свет, расщепляется вода, выделяется кислород. Темновая фаза (цикл Кальвина) идёт в строме: углекислый газ связывается и превращается в углеводы.\n\nЧто нужно растению\nСвет, вода, углекислый газ и хлорофилл.\n\nГде происходит\nВ хлоропластах клеток листа.\n\nИтог\nРастения производят кислород и органические вещества."
        val materialErrors = mapOf("07-file-error" to "Не удалось загрузить файл. Проверьте формат и попробуйте ещё раз.",
            "08-pdf-corrupted" to "Файл повреждён.", "09-pdf-scan" to "Документ похож на скан.",
            "10-pdf-password" to "Файл защищён паролем.", "11-file-too-large" to "Файл слишком большой. Максимум — 10 МиБ.",
            "12-material-empty-error" to "Материал пуст.")
        val generationErrors = mapOf("18-no-connection" to "Нет связи. Проверьте интернет и повторите попытку.",
            "19-server-error" to "Сервер недоступен. Не удалось получить результат. Попробуйте ещё раз.",
            "20-result-too-large" to "Материал слишком большой. Сократите текст или выберите файл меньшего размера.",
            "21-timeout" to "Время ожидания истекло. Попробуйте ещё раз позже.")
        val longErrors = mapOf("23-part-error" to LongMaterialFailure.PART_FAILED, "24-expired" to LongMaterialFailure.EXPIRED,
            "25-long-too-large" to LongMaterialFailure.TOO_LONG, "26-quota" to LongMaterialFailure.QUOTA_EXHAUSTED)
        val names = listOf("01-material-empty", "02-material-text", "03-file-loading", "04-file-txt", "05-file-pdf", "06-text-too-large") +
            materialErrors.keys + listOf("13-modes-text", "14-modes-file", "15-mode-selected", "16-generating", "17-long-wait") +
            generationErrors.keys + listOf("22-progress") + longErrors.keys + listOf("27-result", "28-questions", "29-export-format", "30-save-success", "31-save-error", "32-selected-txt-error", "33-selected-pdf-error")
        compose.setContent { AIStudyAssistantTheme {
            Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 400.dp).fillMaxSize().padding(16.dp)) {
                    key(screen) {
                        when {
                            screen in names.take(12) || screen.startsWith("32-") || screen.startsWith("33-") -> {
                                val state = StudyState(material = when(screen) { "02-material-text" -> text; "06-text-too-large" -> text.repeat(180); else -> "" },
                                    fileName = when(screen) { "04-file-txt", "32-selected-txt-error" -> "Лекция_ИИ.txt"; "05-file-pdf", "33-selected-pdf-error" -> "Лекция_ИИ.pdf"; else -> null },
                                    fileBytes = if(screen in listOf("04-file-txt", "05-file-pdf")) byteArrayOf(1) else null,
                                    busy = screen == "03-file-loading", loadingFile = screen == "03-file-loading", error = if (screen.startsWith("32-") || screen.startsWith("33-")) "Не удалось загрузить файл. Проверьте формат и попробуйте ещё раз." else materialErrors[screen])
                                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                    MaterialContent(state, {}, {}, {}, {})
                                }
                            }
                            screen.startsWith("13-") || screen.startsWith("14-") || screen.startsWith("15-") -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ScreenTitle("Выбор режима")
                                ModesContent(StudyState(material = text, fileName = if (screen == "14-modes-file") "Лекция_ИИ.txt" else null,
                                    modes = ProcessingMode.entries, mode = if (screen == "15-mode-selected") ProcessingMode.KEY_TERMS else ProcessingMode.SHORT_SUMMARY), {}, {}, {})
                            }
                            screen in listOf("16-generating", "17-long-wait") || screen in generationErrors -> GenerationContent(
                                StudyState(busy = screen !in generationErrors, error = generationErrors[screen], canRetry = screen in generationErrors), screen == "17-long-wait", {}, {}, {})
                            screen == "22-progress" -> LongProgressContent(LongProcessingState(running = true, progress = LongProgress(2, 5, 240, false)), {}, {}, materialName = "Лекция_ИИ.txt")
                            screen in longErrors -> {
                                val failure = longErrors.getValue(screen).presentation()
                                LongProgressContent(LongProcessingState(error = failure.message, canRetry = failure.canRetry, progress = LongProgress(2, 5, queued = false)), {}, {}, materialName = "Лекция_ИИ.txt")
                            }
                            screen == "27-result" -> ResultContent(result, "Краткий конспект", {}, {})
                            screen == "28-questions" -> ResultContent("Где в клетке происходит фотосинтез?\n\nКакие два главных продукта образуются в результате фотосинтеза?\n\nКакие компоненты необходимы растению для протекания фотосинтеза?\n\nКакие процессы происходят с участием хлорофилла во время световой фазы?\n\nКакое альтернативное название имеет темновая фаза фотосинтеза?", ProcessingMode.SELF_CHECK_QUESTIONS.title, {}, {})
                            screen == "29-export-format" -> ResultContent(result, "Краткий конспект", {}, {})
                            screen == "30-save-success" -> SaveSuccessScreen("konspekt.md", {}, {})
                            screen == "31-save-error" -> SaveErrorScreen({}, {})
                        }
                    }
                }
            }
        } }
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design-gallery").apply { mkdirs() }
        names.forEach { name ->
            compose.runOnIdle { screen = name }
            compose.waitForIdle()
            if (name == "29-export-format") { compose.onNodeWithTag("save-file-button").performClick(); compose.waitForIdle() }
            val bitmap = if (name == "29-export-format") InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                else compose.onRoot().captureToImage().asAndroidBitmap()
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
