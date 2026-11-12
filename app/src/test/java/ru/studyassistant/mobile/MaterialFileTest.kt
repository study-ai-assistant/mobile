package ru.studyassistant.mobile

import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import ru.studyassistant.mobile.api.ApiException

class MaterialFileTest {
    @Test fun boundsReadingAndPreservesOriginalEncoding() {
        val cp1251 = "Учебный материал".toByteArray(charset("windows-1251"))
        assertArrayEquals(cp1251, readMaterialBytes(ByteArrayInputStream(cp1251)))
        assertEquals(MAX_MATERIAL_FILE_BYTES, readMaterialBytes(ByteArrayInputStream(ByteArray(MAX_MATERIAL_FILE_BYTES))).size)
        assertThrows(ApiException::class.java) { readMaterialBytes(ByteArrayInputStream(ByteArray(MAX_MATERIAL_FILE_BYTES + 1))) }
        assertThrows(ApiException::class.java) { readMaterialBytes(ByteArrayInputStream(byteArrayOf())) }
    }

    @Test fun selectionUploadAndRemovalUseFileInsteadOfManualText() = runBlocking {
        var submittedName = ""
        val flow = StudyFlow(this, { _, _ -> error("Must upload file") }, { "ok" }, uploadTxt = { name, _, _ ->
            submittedName = name; "Ответ для файла"
        })
        flow.editMaterial("Предыдущий текст")
        flow.readFile { MaterialFile("lecture.txt", byteArrayOf(1, 2)) }
        yield()
        assertFalse(flow.state.value.busy)
        assertEquals("", flow.state.value.material)
        flow.showModes(); flow.submit(); yield()
        assertEquals("lecture.txt", submittedName)
        assertEquals(Screen.RESULT, flow.state.value.screen)
        flow.back(); flow.back(); flow.clearFile()
        assertNull(flow.state.value.fileName)
        assertEquals("", flow.state.value.result)
    }

    @Test fun unreadableFileKeepsPreviousMaterialAndEndsLoading() = runBlocking {
        val flow = StudyFlow(this, { _, _ -> "ok" }, { "ok" })
        flow.editMaterial("Сохранить текст")
        flow.readFile { throw IOException("private") }; yield()
        assertEquals("Сохранить текст", flow.state.value.material)
        assertFalse(flow.state.value.busy)
        assertFalse(flow.state.value.loadingFile)
        assertFalse(flow.state.value.error!!.contains("private"))
    }

    @Test fun cancelledFileReadDoesNotReplaceMaterial() = runBlocking {
        val pending = CompletableDeferred<MaterialFile>()
        val flow = StudyFlow(this, { _, _ -> "ok" }, { "ok" })
        flow.editMaterial("Исходный текст")
        flow.readFile { pending.await() }; yield()
        flow.cancel(); pending.complete(MaterialFile("late.txt", byteArrayOf(1))); yield()
        assertNull(flow.state.value.fileName)
        assertEquals("Исходный текст", flow.state.value.material)
        assertFalse(flow.state.value.loadingFile)
    }
}
