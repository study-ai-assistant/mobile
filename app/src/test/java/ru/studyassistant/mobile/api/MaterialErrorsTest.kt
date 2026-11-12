package ru.studyassistant.mobile.api

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MaterialErrorsTest {
    private val cases = listOf(
        Triple(422, "PDF-файл не содержит текстового слоя (возможно, это скан). Загрузите PDF с распознаваемым текстом или TXT-файл.", MaterialErrorMessages.NO_TEXT),
        Triple(422, "PDF-файл защищён паролем. Снимите защиту и загрузите файл снова.", MaterialErrorMessages.PASSWORD),
        Triple(422, "PDF-файл повреждён и не может быть прочитан. Проверьте файл и загрузите снова.", MaterialErrorMessages.CORRUPTED),
        Triple(413, "PDF содержит слишком много страниц. Максимум: 50 страниц.", MaterialErrorMessages.PAGES),
        Triple(413, "Размер файла превышает максимальный лимит в 10 МБ.", MaterialErrorMessages.FILE_SIZE),
        Triple(413, "Максимальный размер файла — 10 МиБ, запроса — 30 МиБ.", MaterialErrorMessages.FILE_SIZE),
        Triple(413, "Максимальная длина текста — 10000 символов.", MaterialErrorMessages.TEXT_LENGTH),
        Triple(504, "Превышено время обработки файла. Попробуйте файл меньшего размера или сложности.", MaterialErrorMessages.PARSING_TIMEOUT)
    )
    private fun problem(status: Int, detail: String) = JSONObject()
        .put("type", "about:blank").put("status", status).put("detail", detail).toString()

    @Test fun mapsEveryPdfContractExampleThroughHttpClient() {
        for ((status, detail, expected) in cases) {
            val connection = ErrorConnection(status, problem(status, detail).byteInputStream())
            val api = StudyApi(openConnection = { connection })
            assertEquals(expected, assertThrows(ApiException::class.java) {
                api.uploadTxt("document.pdf", byteArrayOf(1))
            }.message)
            assertTrue(connection.disconnected)
        }
    }

    @Test fun handlesMissingMalformedUnknownAndOversizedProblemsWithoutExposingBody() {
        for (body in listOf(null, "<html>private</html>", "[]", "{}", "x".repeat(70_000),
            problem(422, "private stacktrace"), problem(400, cases.first().second))) {
            val api = StudyApi(openConnection = { ErrorConnection(422, body?.byteInputStream()) })
            val message = assertThrows(ApiException::class.java) { api.uploadTxt("file.pdf", byteArrayOf(1)) }.message!!
            assertEquals(MaterialErrorMessages.message(422, null), message)
            assertFalse(message.contains("private"))
        }
    }

    @Test fun unreadableErrorBodyStillExplainsHttpFailure() {
        val stream = object : InputStream() { override fun read(): Int = throw IOException("private") }
        val api = StudyApi(openConnection = { ErrorConnection(413, stream) })
        assertEquals(ApiMessages.http(413, true), assertThrows(ApiException::class.java) {
            api.uploadTxt("file.txt", byteArrayOf(1))
        }.message)
    }

    @Test fun rejectsInconsistentStatusOrProblemType() {
        val valid = problem(422, cases.first().second)
        assertEquals(MaterialErrorMessages.message(413, null), MaterialErrorMessages.message(413, valid))
        assertEquals(MaterialErrorMessages.message(422, null), MaterialErrorMessages.message(422,
            valid.replace("about:blank", "https://unknown.example/problem")))
    }

    @Test fun txtValidationAndGenericErrorsStillWork() {
        assertEquals("Файл пуст или не содержит текста. Выберите другой файл.",
            MaterialErrorMessages.message(400, problem(400, "Файл пуст или не содержит текста.")))
        for (status in listOf(400, 500, 502)) assertEquals(ApiMessages.http(status, true),
            MaterialErrorMessages.message(status, null))
        assertTrue(MaterialErrorMessages.message(415, null).contains("формат"))
    }

    private class ErrorConnection(private val status: Int, private val body: InputStream?) :
        HttpURLConnection(URL("http://localhost/api/v1/materials/process")) {
        var disconnected = false
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun getErrorStream() = body
        override fun getResponseCode() = status
        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
    }
}
