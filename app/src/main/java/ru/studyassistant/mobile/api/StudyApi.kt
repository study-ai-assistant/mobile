package ru.studyassistant.mobile.api

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import org.json.JSONArray
import ru.studyassistant.mobile.ProcessingMode
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
        val response = requestObject("GET", "/health")
        if (response.optString("status") != "ok") {
            throw ApiException("Сервер вернул неожиданный ответ проверки связи.")
        }
        return "Сервер доступен: status = ok"
    }

    fun process(text: String, mode: String = "SHORT_SUMMARY"): String {
        validateMaterial(text)?.let { throw ApiException(it) }
        if (ProcessingMode.fromId(mode) == null) throw ApiException("Режим пока недоступен.")
        val body = JSONObject().put("text", text).put("mode", mode)
        val response = requestObject("POST", "/api/v1/text/process", body)
        val result = response.opt("result")
        if (response.optString("mode") != mode || result !is String || result.isBlank()) {
            throw ApiException("Сервер вернул ответ без корректного результата обработки.")
        }
        return result
    }
    fun uploadTxt(fileName: String, fileBytes: ByteArray, mode: String = "SHORT_SUMMARY"): String {
        if (fileBytes.isEmpty()) {
            throw ApiException("Файл пуст или не содержит текста.")
        }
        if (ProcessingMode.fromId(mode) == null) {
            throw ApiException("Режим пока недоступен.")
        }
        val response = requestMultipart(
            path = "/api/v1/materials/process",
            fileName = fileName,
            fileBytes = fileBytes,
            mode = mode
        )
        val result = response.opt("result")
        if (response.optString("mode") != mode || result !is String || result.isBlank()) {
            throw ApiException("Сервер вернул ответ без корректного результата обработки.")
        }
        return result
    }
    fun modes(): List<ProcessingMode> = try {
        val array = JSONArray(request("GET", "/api/v1/modes"))
        val modes = (0 until array.length()).mapNotNull { ProcessingMode.fromId(array.getString(it)) }.distinct()
        if (modes.isEmpty()) throw ApiException("Сервер не вернул доступные режимы.")
        modes
    } catch (_: JSONException) { throw ApiException("Сервер вернул список режимов в неверном формате.") }

    private fun requestObject(method: String, path: String, body: JSONObject? = null): JSONObject = try {
        JSONObject(request(method, path, body))
    } catch (_: JSONException) { throw ApiException(ApiMessages.INVALID_RESPONSE) }
    private fun requestMultipart(
        path: String,
        fileName: String,
        fileBytes: ByteArray,
        mode: String
    ): JSONObject = try {
        val boundary = "Boundary-" + System.currentTimeMillis()
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        val connection = openConnection(URL(baseUrl.trimEnd('/') + path))
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = ServerConfig.CONNECT_TIMEOUT_MS
            connection.readTimeout = ServerConfig.PROCESS_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json, application/problem+json")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            connection.outputStream.use { output ->
                // 1. Поле mode
                output.write((twoHyphens + boundary + lineEnd).toByteArray(Charsets.UTF_8))
                output.write(("Content-Disposition: form-data; name=\"mode\"" + lineEnd + lineEnd).toByteArray(Charsets.UTF_8))
                output.write((mode + lineEnd).toByteArray(Charsets.UTF_8))

                // 2. Поле file
                output.write((twoHyphens + boundary + lineEnd).toByteArray(Charsets.UTF_8))
                output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"" + lineEnd).toByteArray(Charsets.UTF_8))
                output.write(("Content-Type: text/plain; charset=utf-8" + lineEnd + lineEnd).toByteArray(Charsets.UTF_8))
                output.write(fileBytes)
                output.write(lineEnd.toByteArray(Charsets.UTF_8))

                // Закрывающий boundary
                output.write((twoHyphens + boundary + twoHyphens + lineEnd).toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = connection.responseCode
            if (status != 200) {
                throw ApiException(MaterialErrorMessages.message(status, readProblem(connection)))
            }

            val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONObject(responseText)
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
    } catch (_: JSONException) {
        throw ApiException(ApiMessages.INVALID_RESPONSE)
    }

    private fun readProblem(connection: HttpURLConnection): String? = try {
        // Ограничиваем тело ошибки; HTML прокси и повреждённый JSON обработает fallback.
        connection.errorStream?.use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (output.size() <= 64 * 1024) {
                val count = stream.read(buffer, 0, minOf(buffer.size, 64 * 1024 + 1 - output.size()))
                if (count == -1) break
                output.write(buffer, 0, count)
            }
            if (output.size() > 64 * 1024) null else output.toString("UTF-8")
        }
    } catch (_: IOException) { null }

    private fun request(method: String, path: String, body: JSONObject? = null): String {
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
            if (status != 200) throw ApiException(
                if (method == "GET" && path == "/api/v1/modes" && status in listOf(404, 501))
                    "Эндпоинт $path не найден. Проверьте адрес сервера."
                else ApiMessages.http(status, method == "POST")
            )
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
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
