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

private suspend fun writeUtf8TextToUri(
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

    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(StudySpacing.compact)) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) {
            Text(
                modeTitle,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(StudySpacing.compact)
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
                .testTag("result-scroll")
                .verticalScroll(rememberScrollState())
        ) {
            Text(result, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(StudySpacing.screen).testTag("result-text"))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StudySpacing.compact)) {
            StudyButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Результат", result))
                    copied = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (copied) "Скопировано" else "Копировать",
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            OutlinedButton(
                onClick = { showFormatSheet = true },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, SaveAccentPurple),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SaveSecondaryFill,
                    contentColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("save-file-button")
            ) {
                Text("Сохранить")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StudySpacing.compact)) {
            OutlinedButton(
                onClick = { shareResultText(context, modeTitle, result) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("share-result-button")
            ) {
                Text("Поделиться")
            }
            OutlinedButton(onClick = onModes, modifier = Modifier.weight(1f)) { Text("Режимы") }
        }
        OutlinedButton(onClick = onNewMaterial, shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth()) { Text("Новый материал") }
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
private fun FormatPickerSheetContent(
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
                color = if (isSelected) SaveSecondaryFill else Color.White,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) SaveAccentPurple else Color(0xFFC5C2D6)
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
private fun SaveSuccessScreen(
    fileName: String,
    onShare: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SaveBackgroundColor)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Сохранение",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(88.dp)
                .background(SaveSuccessGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(36.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.18f, size.height * 0.52f)
                    lineTo(size.width * 0.42f, size.height * 0.76f)
                    lineTo(size.width * 0.84f, size.height * 0.26f)
                }
                drawPath(
                    path = path,
                    color = TextPrimary,
                    style = Stroke(
                        width = 6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SaveSuccessGreen,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save-success-card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Файл сохранён",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        StudyButton(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save-success-share")
        ) {
            Text("Поделиться", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onDone,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, SaveAccentPurple),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SaveSecondaryFill,
                contentColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save-success-done")
        ) {
            Text("Готово", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SaveErrorScreen(
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SaveBackgroundColor)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Сохранение",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(88.dp)
                .background(SaveErrorFill, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(38.dp)) {
                val triangle = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.12f)
                    lineTo(size.width * 0.9f, size.height * 0.86f)
                    lineTo(size.width * 0.1f, size.height * 0.86f)
                    close()
                }
                drawPath(
                    path = triangle,
                    color = SaveErrorText,
                    style = Stroke(width = 5f, join = StrokeJoin.Round)
                )
                drawLine(
                    color = SaveErrorText,
                    start = Offset(size.width * 0.5f, size.height * 0.38f),
                    end = Offset(size.width * 0.5f, size.height * 0.62f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = SaveErrorText,
                    radius = 3.2f,
                    center = Offset(size.width * 0.5f, size.height * 0.74f)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SaveErrorFill,
            border = BorderStroke(1.5.dp, SaveErrorBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save-error-card")
        ) {
            Text(
                text = "Не удалось сохранить.\nНет места или доступа к хранилищу.\nПовторите попытку",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = SaveErrorText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        StudyButton(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save-error-retry")
        ) {
            Text("Повторить", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, SaveAccentPurple),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SaveSecondaryFill,
                contentColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save-error-back")
        ) {
            Text("Назад", style = MaterialTheme.typography.titleMedium)
        }
    }
}
