package ru.studyassistant.mobile.api

import java.net.URI

/** Only an origin is accepted: API paths are appended by StudyApi. */
fun normalizeServerAddress(value: String, allowHttp: Boolean): String {
    val uri = try { URI(value.trim()) } catch (_: Exception) {
        throw IllegalArgumentException("Введите адрес, например https://server.example:8080")
    }
    require(uri.scheme == "https" || (allowHttp && uri.scheme == "http")) {
        if (allowHttp) "Адрес должен начинаться с http:// или https://" else "В этой сборке разрешён только HTTPS. Для локального HTTP используйте debug-сборку."
    }
    require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawQuery == null &&
        uri.rawFragment == null && (uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") &&
        (uri.port == -1 || uri.port in 1..65535) && !uri.rawAuthority.endsWith(":")) {
        "Укажите только адрес сервера и порт, без /health, пути, пароля и параметров."
    }
    return uri.toASCIIString().trimEnd('/')
}
