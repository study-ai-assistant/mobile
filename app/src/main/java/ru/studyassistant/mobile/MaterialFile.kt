package ru.studyassistant.mobile

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.InputStream
import ru.studyassistant.mobile.api.ApiException

internal const val MAX_MATERIAL_FILE_BYTES = 10 * 1024 * 1024
data class MaterialFile(val name: String, val bytes: ByteArray)

/** Ограничение проверяется при чтении: провайдер может не сообщать размер. */
internal fun readMaterialBytes(input: InputStream): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = input.read(buffer)
        if (count == -1) break
        if (output.size() + count > MAX_MATERIAL_FILE_BYTES) throw ApiException("Файл слишком большой. Максимум — 10 МиБ.")
        output.write(buffer, 0, count)
    }
    if (output.size() == 0) throw ApiException("Файл пуст. Выберите другой файл.")
    return output.toByteArray()
}

internal fun readMaterialFile(resolver: ContentResolver, uri: Uri): MaterialFile {
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
    } ?: throw ApiException("Не удалось определить имя файла. Выберите другой TXT-файл.")
    if (!name.endsWith(".txt", ignoreCase = true) || name.any { it == '"' || it == '\r' || it == '\n' }) {
        throw ApiException("Выберите файл в формате TXT.")
    }
    val bytes = resolver.openInputStream(uri)?.use(::readMaterialBytes)
        ?: throw ApiException("Не удалось открыть файл. Выберите его снова.")
    return MaterialFile(name, bytes)
}
