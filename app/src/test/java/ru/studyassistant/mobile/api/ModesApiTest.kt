package ru.studyassistant.mobile.api

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import ru.studyassistant.mobile.ProcessingMode

class ModesApiTest {
    @Test fun loadsServerListAndSendsAllFiveIds() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val ids = mutableListOf<String>()
        server.createContext("/api/v1/modes") { ex ->
            val body = ProcessingMode.entries.joinToString(",", "[", "]") { "\"${it.name}\"" }.toByteArray()
            ex.sendResponseHeaders(200, body.size.toLong()); ex.responseBody.use { it.write(body) }
        }
        server.createContext("/api/v1/text/process") { ex ->
            val input = JSONObject(ex.requestBody.bufferedReader().readText())
            ids += input.getString("mode")
            val body = JSONObject().put("mode", ids.last()).put("result", "Ответ").toString().toByteArray()
            ex.sendResponseHeaders(200, body.size.toLong()); ex.responseBody.use { it.write(body) }
        }
        server.start()
        try {
            val api = StudyApi("http://127.0.0.1:${server.address.port}")
            assertEquals(ProcessingMode.entries, api.modes())
            for (mode in api.modes()) assertEquals("Ответ", api.process("Материал", mode.name))
            assertEquals(ProcessingMode.entries.map { it.name }, ids)
        } finally { server.stop(0) }
    }

    @Test fun invalidEmptyAndUnsupportedListsAreExplained() {
        for (body in listOf("not-json", "[]", "[\"FUTURE_MODE\"]")) {
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.createContext("/") { ex ->
                val bytes = body.toByteArray()
                ex.sendResponseHeaders(200, bytes.size.toLong()); ex.responseBody.use { it.write(bytes) }
            }
            server.start()
            try { assertThrows(ApiException::class.java) { StudyApi("http://127.0.0.1:${server.address.port}").modes() } }
            finally { server.stop(0) }
        }
    }
}
