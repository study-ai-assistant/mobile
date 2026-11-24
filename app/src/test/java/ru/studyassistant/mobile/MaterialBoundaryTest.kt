package ru.studyassistant.mobile

import java.io.InputStream
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import ru.studyassistant.mobile.api.ApiException

class MaterialBoundaryTest {
    @Test fun rejects101MiBBeforeAllocatingOrReadingEverything() {
        var read = 0
        val input = object : InputStream() {
            override fun read(): Int = if (read++ < 101 * 1024 * 1024) 65 else -1
            override fun read(bytes: ByteArray, off: Int, len: Int): Int {
                val count = minOf(len, 101 * 1024 * 1024 - read)
                if (count <= 0) return -1
                bytes.fill(65, off, off + count); read += count; return count
            }
        }
        assertThrows(ApiException::class.java) { readMaterialBytes(input) }
        assertTrue(read <= MAX_MATERIAL_FILE_BYTES + 8192)
        assertThrows(ApiException::class.java) { validateFileSize(101L * 1024 * 1024) }
        validateFileSize(null); validateFileSize(-1); validateFileSize(MAX_MATERIAL_FILE_BYTES.toLong())
    }
    @Test fun invalidReplacementCannotSendPreviousFile() = runBlocking {
        var calls = 0
        val flow = StudyFlow(this, { _, _ -> "text" }, { "ok" }, uploadTxt = { _, _, _ -> calls++; "result" })
        flow.selectFile("previous.txt", byteArrayOf(65))
        flow.readFile { throw ApiException("Файл слишком большой. Максимум — 10 МиБ.") }; yield()
        flow.showModes(); yield()
        assertEquals(Screen.MATERIAL, flow.state.value.screen)
        assertNull(flow.state.value.fileBytes)
        assertNotNull(flow.state.value.error)
        assertEquals(0, calls)
        flow.readFile { MaterialFile("new.txt", byteArrayOf(66)) }; yield()
        flow.showModes(); flow.submit(); yield()
        assertEquals(Screen.RESULT, flow.state.value.screen)
        assertEquals(1, calls)
    }
    @Test fun invalidFileResponseReturnsToMaterialWithoutRetryingSameRequest() = runBlocking {
        val flow = StudyFlow(this, { _, _ -> "text" }, { "ok" }, uploadTxt = { _, _, _ -> throw ApiException("Документ похож на скан", 422) })
        flow.selectFile("scan.pdf", byteArrayOf(1)); flow.showModes(); flow.submit(); yield()
        assertEquals(Screen.MATERIAL, flow.state.value.screen)
        assertFalse(flow.state.value.canRetry)
        assertNotNull(flow.state.value.error)
    }
    @Test fun fileNamesAndTextBoundaries() {
        validateFileName("лекция.PDF"); validateFileName("конспект.TXT")
        assertThrows(ApiException::class.java) { validateFileName("lecture.exe") }
        assertThrows(ApiException::class.java) { validateFileName("bad\r\n.txt") }
        assertNull(ru.studyassistant.mobile.api.validateMaterial("😀".repeat(24000)))
        assertNotNull(ru.studyassistant.mobile.api.validateMaterial("😀".repeat(24001)))
        assertNotNull(ru.studyassistant.mobile.api.validateMaterial(" \t\n"))
    }
}
