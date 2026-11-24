package ru.studyassistant.mobile

import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import ru.studyassistant.mobile.api.ApiException

class MaterialDeviceTest {
    @Test fun actualDocumentsReject101MiBAndEmptyAndPreserveCp1251() {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        fun create(name: String, write: (java.io.OutputStream) -> Unit): android.net.Uri {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/StudyAssistantQA")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
            resolver.openOutputStream(uri)!!.use(write)
            return uri
        }
        val huge = create("oversized-101MiB.txt") { out ->
            val buffer = ByteArray(1024 * 1024) { 65 }; repeat(101) { out.write(buffer) }
        }
        val empty = create("empty.txt") { }
        val bytes = "Учебный материал на русском языке".toByteArray(charset("windows-1251"))
        val cp1251 = create("windows-1251.txt") { it.write(bytes) }
        val utf8 = create("utf8.txt") { it.write("Фотосинтез — образование органических веществ на свету.".toByteArray()) }
        try {
            assertTrue(assertThrows(ApiException::class.java) { readMaterialFile(resolver, huge) }.message!!.contains("10 МиБ"))
            assertTrue(assertThrows(ApiException::class.java) { readMaterialFile(resolver, empty) }.message!!.contains("пуст"))
            assertArrayEquals(bytes, readMaterialFile(resolver, cp1251).bytes)
            assertTrue(readMaterialFile(resolver, utf8).bytes.toString(Charsets.UTF_8).startsWith("Фотосинтез"))
        } finally {
            listOf(huge, empty, cp1251, utf8).forEach { resolver.delete(it, null, null) }
        }
    }
}
