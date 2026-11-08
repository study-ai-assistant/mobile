package ru.studyassistant.mobile

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class ServerChangeTest {
    @Test fun changingServerKeepsSelectedFile() = runBlocking {
        val flow = StudyFlow(this, { _, _ -> "" }, { "ok" })
        val bytes = "Учебный текст".toByteArray()
        flow.selectFile("lecture.txt", bytes)
        flow.serverChanged()
        assertEquals("lecture.txt", flow.state.value.fileName)
        assertArrayEquals(bytes, flow.state.value.fileBytes)
    }
    @Test fun changingServerKeepsMaterialButReloadsModesAndClearsResult() = runBlocking {
        var loads = 0
        val flow = StudyFlow(this, { _, _ -> "Результат" }, { "ok" },
            loadModes = { loads++; ProcessingMode.entries })
        flow.editMaterial("Учебный текст")
        flow.showModes(); yield()
        flow.submit(); yield()
        assertEquals(Screen.RESULT, flow.state.value.screen)
        flow.serverChanged()
        assertEquals("Учебный текст", flow.state.value.material)
        assertEquals(Screen.MATERIAL, flow.state.value.screen)
        assertEquals("", flow.state.value.result)
        assertFalse(flow.state.value.modesLoaded)
        assertFalse(flow.state.value.canRetry)
        flow.showModes(); yield()
        assertEquals(2, loads)
    }
}
