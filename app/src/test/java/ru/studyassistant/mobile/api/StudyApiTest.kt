package ru.studyassistant.mobile.api

import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.net.URL
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class StudyApiTest {
    @Test fun validatesBlankAndUnicodeLength() {
        assertNotNull(validateMaterial(" \n\t"))
        assertNull(validateMaterial("😀".repeat(10_000)))
        assertNotNull(validateMaterial("😀".repeat(10_001)))
    }

    @Test fun healthChecksActualStatus() = withServer(200, "{\"status\":\"ok\"}") { api ->
        assertTrue(api.health().contains("status = ok"))
    }

    @Test fun rejectsUnexpectedHealth() = withServer(200, "{\"status\":\"down\"}") { api ->
        assertTrue(assertThrows(ApiException::class.java) { api.health() }.message!!.contains("неожиданный"))
    }

    @Test fun sendsUtf8TextAndModeAndReadsResult() {
        var requestCount = 0
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        var method = ""
        var path = ""
        var requestBody = ""
        var contentType = ""
        server.createContext("/") { exchange ->
            requestCount++
            method = exchange.requestMethod
            path = exchange.requestURI.path
            contentType = exchange.requestHeaders.getFirst("Content-Type")
            requestBody = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val response = "{\"mode\":\"SHORT_SUMMARY\",\"result\":\"Краткий ответ\"}".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        try {
            assertEquals("Краткий ответ", StudyApi("http://127.0.0.1:${server.address.port}").process("Текст \"пример\"\n😀"))
            assertEquals("POST", method)
            assertEquals("/api/v1/text/process", path)
            assertTrue(contentType.contains("application/json"))
            val json = JSONObject(requestBody)
            assertEquals("Текст \"пример\"\n😀", json.getString("text"))
            assertEquals("SHORT_SUMMARY", json.getString("mode"))
            assertEquals(1, requestCount)
        } finally { server.stop(0) }
    }

    @Test fun explainsUnimplementedEndpoint() = withServer(404, "") { api ->
        assertTrue(assertThrows(ApiException::class.java) { api.process("Текст") }.message!!.contains("пока не подключена"))
    }

    @Test fun translatesContractErrorsWithoutExposingServerBody() {
        val messages = mapOf(
            400 to "Сервер отклонил запрос. Проверьте текст и выбранный режим.",
            413 to "Материал слишком большой для сервера. Сократите текст или выберите файл меньшего размера.",
            502 to "Сервис обработки текста временно недоступен. Попробуйте позже.",
            500 to "Ошибка сервера (HTTP 500). Попробуйте позже."
        )
        for ((status, expected) in messages) {
            withServer(status, "private-internal-error") { api ->
                val error = assertThrows(ApiException::class.java) { api.process("Текст") }
                assertFalse(error.message!!.contains("private-internal-error"))
                assertEquals(expected, error.message)
            }
        }
    }

    @Test fun rejectsMalformedResponse() = withServer(200, "not-json") { api ->
        assertTrue(assertThrows(ApiException::class.java) { api.process("Текст") }.message!!.contains("неверном формате"))
    }

    @Test fun rejectsMissingResult() = withServer(200, "{\"mode\":\"SHORT_SUMMARY\"}") { api ->
        assertThrows(ApiException::class.java) { api.process("Текст") }
        Unit
    }

    @Test fun reportsUnavailableServer() {
        val port = ServerSocket(0).use { it.localPort }
        val error = assertThrows(ApiException::class.java) { StudyApi("http://127.0.0.1:$port").health() }
        assertEquals("Не удалось связаться с сервером. Проверьте подключение к сети и попробуйте снова.", error.message)
    }

    @Test fun processingTimeoutIs90SecondsAndHasItsOwnMessage() {
        val connection = FailingConnection(SocketTimeoutException("internal timeout details"))
        val api = StudyApi(openConnection = { connection })
        val error = assertThrows(ApiException::class.java) { api.process("Материал") }
        assertEquals(90_000, connection.readTimeout)
        assertEquals(10_000, connection.connectTimeout)
        assertEquals("Сервер не ответил вовремя. Попробуйте ещё раз позже.", error.message)
        assertEquals(1, connection.attempts)
        assertTrue(connection.disconnected)
    }

    @Test fun healthKeepsShortTimeoutAndNetworkFailureHasItsOwnMessage() {
        val connection = FailingConnection(IOException("internal network details"))
        val error = assertThrows(ApiException::class.java) {
            StudyApi(openConnection = { connection }).health()
        }
        assertEquals(10_000, connection.readTimeout)
        assertEquals("Не удалось связаться с сервером. Проверьте подключение к сети и попробуйте снова.", error.message)
        assertTrue(connection.disconnected)
    }

    private class FailingConnection(private val failure: IOException) :
        HttpURLConnection(URL("http://localhost")) {
        var attempts = 0
        var disconnected = false
        override fun connect() = Unit
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun getResponseCode(): Int { attempts++; throw failure }
    }

    private fun withServer(status: Int, body: String, test: (StudyApi) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            exchange.requestBody.close()
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try { test(StudyApi("http://127.0.0.1:${server.address.port}")) }
        finally { server.stop(0) }
    }
}
