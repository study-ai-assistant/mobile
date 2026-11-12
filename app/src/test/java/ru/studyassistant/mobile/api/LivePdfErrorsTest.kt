package ru.studyassistant.mobile.api

import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Явно включаемая проверка с настоящим backend; в обычном CI пропускается. */
class LivePdfErrorsTest {
    @Test fun realPdfFilesProduceDistinctMessages() {
        val address = System.getenv("PDF_TEST_BASE_URL")
        val directory = System.getenv("PDF_TEST_FIXTURES")
        assumeTrue("Нужны PDF_TEST_BASE_URL и PDF_TEST_FIXTURES", !address.isNullOrBlank() && !directory.isNullOrBlank())
        val cases = mapOf("scan" to MaterialErrorMessages.NO_TEXT, "password" to MaterialErrorMessages.PASSWORD,
            "corrupted" to MaterialErrorMessages.CORRUPTED, "pages" to MaterialErrorMessages.PAGES,
            "file-size" to MaterialErrorMessages.FILE_SIZE, "text-length" to MaterialErrorMessages.TEXT_LENGTH)
        val api = StudyApi(address!!)
        for ((name, expected) in cases) {
            val file = File(directory!!, "$name.pdf")
            assertEquals(name, expected, assertThrows(ApiException::class.java) {
                api.uploadTxt(file.name, file.readBytes())
            }.message)
        }
    }
}
