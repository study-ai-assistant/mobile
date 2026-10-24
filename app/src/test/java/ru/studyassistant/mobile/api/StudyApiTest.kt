package ru.studyassistant.mobile.api

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.ServerSocket
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
        for (status in listOf(400, 413, 502, 500)) {
            withServer(status, "private-internal-error") { api ->
                val error = assertThrows(ApiException::class.java) { api.process("Текст") }
                assertFalse(error.message!!.contains("private-internal-error"))
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
        assertTrue(error.message!!.contains("Не удалось связаться"))
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
