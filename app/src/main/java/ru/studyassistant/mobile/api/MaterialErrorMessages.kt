package ru.studyassistant.mobile.api

import org.json.JSONException
import org.json.JSONObject

/** Соответствие примерам Problem из backend PR #38 (d9bd656a).
 * У контракта пока нет code: сопоставляем только известные detail + HTTP status.
 * Неизвестный detail никогда не показываем пользователю.
 */
internal object MaterialErrorMessages {
    const val NO_TEXT = "В PDF нет текстового слоя: возможно, это скан. Выберите PDF с выделяемым текстом или TXT-файл."
    const val PASSWORD = "PDF защищён паролем. Выберите доступную вам копию без пароля или другой файл."
    const val CORRUPTED = "Не удалось прочитать PDF: файл повреждён. Скачайте его заново или выберите другой файл."
    const val PAGES = "В PDF больше 50 страниц. Выберите документ до 50 страниц."
    const val FILE_SIZE = "Файл слишком большой. Выберите файл размером не больше 10 МиБ."
    const val TEXT_LENGTH = "В материале больше 10 000 символов. Выберите более короткий материал."
    const val PARSING_TIMEOUT = "Не удалось разобрать файл вовремя. Выберите меньший или более простой PDF."

    private val known = mapOf(
        (422 to "PDF-файл не содержит текстового слоя (возможно, это скан). Загрузите PDF с распознаваемым текстом или TXT-файл.") to NO_TEXT,
        (422 to "PDF-файл защищён паролем. Снимите защиту и загрузите файл снова.") to PASSWORD,
        (422 to "PDF-файл повреждён и не может быть прочитан. Проверьте файл и загрузите снова.") to CORRUPTED,
        (413 to "PDF содержит слишком много страниц. Максимум: 50 страниц.") to PAGES,
        (413 to "Размер файла превышает максимальный лимит в 10 МБ.") to FILE_SIZE,
        (413 to "Максимальный размер файла — 10 МиБ, запроса — 30 МиБ.") to FILE_SIZE,
        (413 to "Максимальная длина текста — 10000 символов.") to TEXT_LENGTH,
        (413 to "Текст в файле превышает максимальную длину в 10000 символов.") to TEXT_LENGTH,
        (400 to "Файл пуст или не содержит текста.") to "Файл пуст или не содержит текста. Выберите другой файл.",
        (400 to "Неподдерживаемый режим обработки.") to "Этот режим недоступен. Вернитесь к выбору режима.",
        (504 to "Превышено время обработки файла. Попробуйте файл меньшего размера или сложности.") to PARSING_TIMEOUT
    )

    fun message(status: Int, body: String?): String {
        val problem = try { body?.let(::JSONObject) } catch (_: JSONException) { null }
        val bodyStatus = problem?.opt("status")
        val matchingStatus = bodyStatus is Number && bodyStatus.toDouble() == status.toDouble()
        if (matchingStatus && problem?.optString("type") == "about:blank") {
            known[status to problem.optString("detail")]?.let { return it }
        }
        return when (status) {
            415 -> "Этот формат файла не поддерживается сервером. Выберите TXT или поддерживаемый сервером PDF."
            422 -> "Не удалось обработать файл. Проверьте, что он открывается и содержит текст, или выберите другой файл."
            504 -> "Обработка файла заняла слишком много времени. Попробуйте меньший файл."
            else -> ApiMessages.http(status, processing = true)
        }
    }
}
