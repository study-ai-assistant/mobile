package ru.studyassistant.mobile.api

/** Пользовательские сообщения без внутренних подробностей ответа сервера. */
internal object ApiMessages {
    const val RATE_LIMIT = "Лимит сервиса обработки текста исчерпан. Повторите запрос позже."

    fun knownRateLimit(status: Int, body: String?): Boolean {
        if (status != 502 || body == null) return false
        val problem = try { org.json.JSONObject(body) } catch (_: org.json.JSONException) { return false }
        return problem.optInt("status") == status && problem.optString("type") == "about:blank" &&
            problem.optString("detail") == RATE_LIMIT
    }
    const val TIMEOUT = "Сервер не ответил вовремя. Попробуйте ещё раз позже."
    const val CONNECTION = "Нет связи. Проверьте интернет и повторите попытку"
    const val INVALID_RESPONSE = "Сервер вернул ответ в неверном формате."

    fun http(status: Int, processing: Boolean): String = when (status) {
        400 -> "Сервер отклонил запрос. Проверьте текст и выбранный режим."
        404, 501 -> if (processing)
            "Обработка текста на сервере пока не подключена. Используйте проверку связи."
            else "Эндпоинт /health не найден. Проверьте адрес сервера."
        413 -> "Материал слишком большой для сервера. Сократите текст или выберите файл меньшего размера."
        502 -> "Сервер недоступен. Не удалось получить результат. Попробуйте ещё раз или зайдите позже"
        else -> "Ошибка сервера (HTTP $status). Попробуйте позже."
    }
}
