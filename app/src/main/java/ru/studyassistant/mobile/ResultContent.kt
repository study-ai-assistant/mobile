package ru.studyassistant.mobile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.studyassistant.mobile.ui.theme.*
import ru.studyassistant.mobile.ui.theme.StudySpacing

private val SaveBackgroundColor = Background
private val SaveAccentPurple = Primary
private val SaveSecondaryFill = PrimarySurface
private val SaveSuccessGreen = Success
private val SaveErrorFill = ErrorSurface
private val SaveErrorBorder = ErrorText
private val SaveErrorText = ErrorText

internal enum class ExportFormat(
    val label: String,
    val extension: String,
    val mimeType: String
) {
    TXT("TXT (.txt)", "txt", "text/plain"),
    MARKDOWN("Markdown\n(.md)", "md", "text/markdown")
}

private sealed interface SaveScreenState {
    data object Result : SaveScreenState
    data class Success(val fileName: String) : SaveScreenState
    data class Error(val format: ExportFormat) : SaveScreenState
}

internal fun buildShareText(modeTitle: String, result: String): String {
    val cleanTitle = modeTitle.trim()
    val cleanResult = result.trim()
    return when {
        cleanTitle.isEmpty() -> cleanResult
        cleanResult.isEmpty() -> cleanTitle
        else -> "$cleanTitle\n\n$cleanResult"
    }
}

internal fun formatExportContent(
    modeTitle: String,
    result: String,
    format: ExportFormat
): String {
    val cleanTitle = modeTitle.trim()
    val cleanResult = result.trim()
    return when (format) {
        ExportFormat.TXT -> when {
            cleanTitle.isEmpty() -> cleanResult
            cleanResult.isEmpty() -> cleanTitle
            else -> "$cleanTitle\n\n$cleanResult"
        }

        ExportFormat.MARKDOWN -> when {
            cleanTitle.isEmpty() -> cleanResult
            cleanResult.isEmpty() -> "# $cleanTitle"
            else -> "# $cleanTitle\n\n$cleanResult"
        }
    }
}

internal fun encodeExportUtf8(content: String): ByteArray =
    content.toByteArray(Charsets.UTF_8)

internal fun buildExportFileName(
    modeTitle: String,
    extension: String,
    date: Date = Date()
): String {
    val dateStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
    val safeMode = modeTitle
        .trim()
        .replace(Regex("[\\\\/:*?\"<>|]"), "")
        .replace(Regex("\\s+"), "_")
        .ifEmpty { "result" }
    return "${safeMode}_${dateStamp}.$extension"
}

internal suspend fun writeUtf8TextToUri(
    context: Context,
    uri: Uri,
    text: String
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(encodeExportUtf8(text))
            stream.flush()
        } ?: error("Не удалось открыть выбранный файл для записи")
    }
}

private fun resolveDisplayName(context: Context, uri: Uri, fallback: String): String {
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback
}

private fun shareResultText(context: Context, modeTitle: String, result: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, modeTitle.trim())
        putExtra(Intent.EXTRA_TEXT, buildShareText(modeTitle, result))
    }
    context.startActivity(Intent.createChooser(sendIntent, "Поделиться результатом"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultContent(
    result: String,
    modeTitle: String,
    onModes: () -> Unit,
    onNewMaterial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copied by remember(result) { mutableStateOf(false) }
    var showFormatSheet by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.TXT) }
    var pendingFileName by remember { mutableStateOf("") }
    var saveScreenState by remember(result) { mutableStateOf<SaveScreenState>(SaveScreenState.Result) }

    val handleSaveUri: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            scope.launch {
                val content = formatExportContent(modeTitle, result, selectedFormat)
                val writeResult = writeUtf8TextToUri(context, uri, content)
                saveScreenState = if (writeResult.isSuccess) {
                    val savedName = resolveDisplayName(context, uri, pendingFileName)
                    SaveScreenState.Success(savedName)
                } else {
                    SaveScreenState.Error(selectedFormat)
                }
            }
        }
    }

    val txtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.TXT.mimeType),
        onResult = handleSaveUri
    )
    val mdLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.MARKDOWN.mimeType),
        onResult = handleSaveUri
    )

    val launchSaveDialog: (ExportFormat) -> Unit = { format ->
        selectedFormat = format
        val fileName = buildExportFileName(modeTitle, format.extension)
        pendingFileName = fileName
        when (format) {
            ExportFormat.TXT -> txtLauncher.launch(fileName)
            ExportFormat.MARKDOWN -> mdLauncher.launch(fileName)
        }
    }

    when (val state = saveScreenState) {
        is SaveScreenState.Success -> {
            SaveSuccessScreen(
                fileName = state.fileName,
                onShare = { shareResultText(context, modeTitle, result) },
                onDone = { saveScreenState = SaveScreenState.Result },
                modifier = modifier
            )
            return
        }

        is SaveScreenState.Error -> {
            SaveErrorScreen(
                onRetry = { launchSaveDialog(state.format) },
                onBack = { saveScreenState = SaveScreenState.Result },
                modifier = modifier
            )
            return
        }

        SaveScreenState.Result -> Unit
    }

    var showActions by remember { mutableStateOf(false) }
    val isQuestions = modeTitle == ProcessingMode.SELF_CHECK_QUESTIONS.title
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth()) {
            ScreenTitle("Результат")
            Box(Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { showActions = true }, modifier = Modifier.testTag("result-more").semantics { contentDescription = "Другие действия" }) {
                    Text("⋮", style = MaterialTheme.typography.titleLarge)
                }
                DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                    if (isQuestions) DropdownMenuItem(text = { Text(if (copied) "Скопировано" else "Копировать") }, onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Результат", result))
                        copied = true
                    })
                    if (isQuestions) DropdownMenuItem(text = { Text("Сохранить") }, onClick = { showActions = false; showFormatSheet = true })
                    DropdownMenuItem(text = { Text("Поделиться") }, onClick = { showActions = false; shareResultText(context, modeTitle, result) }, modifier = Modifier.testTag("share-result-button"))
                    DropdownMenuItem(text = { Text("Режимы") }, onClick = { showActions = false; onModes() })
                    if (isQuestions) DropdownMenuItem(text = { Text("Новый материал") }, onClick = { showActions = false; onNewMaterial() })
                }
            }
        }
        ModeBadge(modeTitle)

        Column(Modifier.weight(1f).fillMaxWidth()
            .then(if (isQuestions) Modifier else Modifier.background(PrimarySurface, MaterialTheme.shapes.large))
            .testTag("result-scroll").verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isQuestions) {
                result.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }.forEach { paragraph ->
                    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = PrimarySurface) {
                        Text(paragraph, Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else Text(result, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp).testTag("result-text"))
        }
        if (isQuestions) SecondaryButton("Назад", onModes, Modifier.fillMaxWidth()) else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StudyButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Результат", result)); copied = true
            }, modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("copy-result").semantics { contentDescription = "Копировать" }) {
                StudySymbol("copy", Modifier.size(22.dp), TextOnPrimary)
                Spacer(Modifier.width(6.dp))
                Text(if (copied) "Готово" else "Копир.", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            OutlinedButton(onClick = { showFormatSheet = true }, shape = MaterialTheme.shapes.medium,
                border = BorderStroke(2.dp, Primary), colors = ButtonDefaults.outlinedButtonColors(containerColor = PrimarySurface, contentColor = TextPrimary),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("save-file-button").semantics { contentDescription = "Сохранить" }) {
                StudySymbol("download", Modifier.size(22.dp), TextPrimary)
                Spacer(Modifier.width(6.dp)); Text("Сохр.")
            }
        }
        SecondaryButton("Новый материал", onNewMaterial, Modifier.fillMaxWidth())
        }
    }
    if (showFormatSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFormatSheet = false },
            sheetState = sheetState,
            containerColor = SaveBackgroundColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            FormatPickerSheetContent(
                selectedFormat = selectedFormat,
                onSelectFormat = { selectedFormat = it },
                onConfirm = {
                    showFormatSheet = false
                    launchSaveDialog(selectedFormat)
                },
                onCancel = { showFormatSheet = false }
            )
        }
    }
}

@Composable
internal fun FormatPickerSheetContent(
    selectedFormat: ExportFormat,
    onSelectFormat: (ExportFormat) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(ExportFormat.TXT, ExportFormat.MARKDOWN).forEach { format ->
            val isSelected = format == selectedFormat
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) SaveSecondaryFill else Background,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) SaveAccentPurple else Inactive
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clickable { onSelectFormat(format) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = format.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        StudyButton(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("confirm-save-format")
        ) {
            Text("Сохранить", style = MaterialTheme.typography.titleMedium)
        }

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, SaveAccentPurple),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SaveSecondaryFill,
                contentColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Отменить", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
internal fun SaveSuccessScreen(fileName: String, onShare: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    AdaptiveScreen(modifier, footer = {
        StudyButton(onShare, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("save-success-share")) { Text("Поделиться") }
        SecondaryButton("Готово", onDone, Modifier.fillMaxWidth().testTag("save-success-done"))
    }) {
        ScreenTitle("Сохранение")
        Spacer(Modifier.height(48.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            StudySymbol("success", Modifier.size(80.dp), TextPrimary)
        }
        Spacer(Modifier.height(32.dp))
        Surface(Modifier.fillMaxWidth().testTag("save-success-card"), shape = MaterialTheme.shapes.large,
            color = Success, border = BorderStroke(2.dp, Primary)) {
            Text("Файл сохранён\n$fileName", Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun SaveErrorScreen(onRetry: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    AdaptiveScreen(modifier, footer = {
        StudyButton(onRetry, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("save-error-retry")) { Text("Повторить") }
        SecondaryButton("Назад", onBack, Modifier.fillMaxWidth().testTag("save-error-back"))
    }) {
        ScreenTitle("Сохранение")
        Spacer(Modifier.height(48.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(80.dp).background(ErrorSurface, CircleShape), contentAlignment = Alignment.Center) {
                StudySymbol("warning", Modifier.size(40.dp), ErrorText)
            }
        }
        Spacer(Modifier.height(32.dp))
        Surface(Modifier.fillMaxWidth().testTag("save-error-card"), shape = MaterialTheme.shapes.large,
            color = ErrorSurface, border = BorderStroke(1.dp, ErrorText)) {
            Text("Не удалось сохранить.\nНет места или доступа к хранилищу.\nПовторите попытку", Modifier.padding(24.dp),
                color = ErrorText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}
