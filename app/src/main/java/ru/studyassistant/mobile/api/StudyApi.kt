package ru.studyassistant.mobile.api

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import org.json.JSONException
import org.json.JSONObject

class ApiException(message: String) : IOException(message)

fun validateMaterial(text: String): String? = when {
    text.isBlank() -> "Введите учебный текст."
    text.codePointCount(0, text.length) > 10_000 -> "Допустимо не более 10 000 символов."
    else -> null
}

/** Блокирующий HTTP-клиент: вызывать только из Dispatchers.IO. POST не повторяется. */
class StudyApi(
    private val baseUrl: String = ServerConfig.BASE_URL,
    private val openConnection: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    }
) {
    fun health(): String {
        val response = request("GET", "/health")
        if (response.optString("status") != "ok") {
            throw ApiException("Сервер вернул неожиданный ответ проверки связи.")
        }
        return "Сервер доступен: status = ok"
    }

    fun process(text: String, mode: String = "SHORT_SUMMARY"): String {
        validateMaterial(text)?.let { throw ApiException(it) }
        if (mode != "SHORT_SUMMARY") throw ApiException("Режим пока недоступен.")
        val body = JSONObject().put("text", text).put("mode", mode)
        val response = request("POST", "/api/v1/text/process", body)
        val result = response.opt("result")
        if (response.optString("mode") != mode || result !is String || result.isBlank()) {
            throw ApiException("Сервер вернул ответ без корректного результата обработки.")
        }
        return result
    }

    private fun request(method: String, path: String, body: JSONObject? = null): JSONObject {
        val connection = openConnection(URL(baseUrl.trimEnd('/') + path))
        try {
            connection.requestMethod = method
            connection.connectTimeout = ServerConfig.CONNECT_TIMEOUT_MS
            connection.readTimeout = if (method == "POST") ServerConfig.PROCESS_TIMEOUT_MS
                else ServerConfig.CONNECT_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json, application/problem+json")
            if (body != null) {
                val payload = body.toString().toByteArray(Charsets.UTF_8)
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(payload.size)
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(payload) }
            }
            val status = connection.responseCode
            if (status != 200) throw ApiException(ApiMessages.http(status, method == "POST"))
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        } catch (e: ApiException) {
            throw e
        } catch (_: SocketTimeoutException) {
            throw ApiException(ApiMessages.TIMEOUT)
        } catch (_: JSONException) {
            throw ApiException(ApiMessages.INVALID_RESPONSE)
        } catch (_: IOException) {
            throw ApiException(ApiMessages.CONNECTION)
        } finally {
            connection.disconnect()
        }
    }
}
