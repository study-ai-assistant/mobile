package ru.studyassistant.mobile.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Decorative vector symbols; the adjacent text provides the accessible label. */
@Composable
fun StudySymbol(kind: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier.size(28.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round)
        val path = Path()
        when (kind) {
            "warning" -> {
                path.moveTo(w*.5f,h*.08f); path.lineTo(w*.94f,h*.86f); path.lineTo(w*.06f,h*.86f); path.close()
                path.moveTo(w*.5f,h*.34f); path.lineTo(w*.5f,h*.55f)
                drawCircle(color, radius = 1.5.dp.toPx(), center = Offset(w*.5f,h*.71f))
            }
            "offline" -> {
                // Broken lightning outline, with a separate diagonal strike through it.
                path.moveTo(w*.46f,h*.19f)
                path.lineTo(w*.59f,h*.07f)
                path.cubicTo(w*.65f,h*.02f,w*.72f,h*.07f,w*.69f,h*.15f)
                path.lineTo(w*.59f,h*.40f)
                path.lineTo(w*.76f,h*.40f)
                path.cubicTo(w*.84f,h*.40f,w*.85f,h*.46f,w*.80f,h*.51f)
                path.lineTo(w*.75f,h*.56f)
                path.moveTo(w*.34f,h*.31f)
                path.lineTo(w*.20f,h*.48f)
                path.cubicTo(w*.15f,h*.54f,w*.18f,h*.59f,w*.25f,h*.59f)
                path.lineTo(w*.45f,h*.59f)
                path.lineTo(w*.35f,h*.84f)
                path.cubicTo(w*.32f,h*.92f,w*.39f,h*.96f,w*.45f,h*.90f)
                path.lineTo(w*.64f,h*.70f)
                path.moveTo(w*.10f,h*.07f); path.lineTo(w*.91f,h*.92f)
                drawPath(path, color, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                return@Canvas
            }
            "error-filled" -> {
                drawCircle(color, radius = w * .48f)
                drawLine(TextOnPrimary, Offset(w*.5f,h*.27f), Offset(w*.5f,h*.53f),
                    strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(TextOnPrimary, radius = 1.2.dp.toPx(), center = Offset(w*.5f,h*.71f))
                return@Canvas
            }
            "error" -> {
                drawCircle(color, radius = w * .43f, style = stroke)
                path.moveTo(w*.5f,h*.27f); path.lineTo(w*.5f,h*.55f)
                drawCircle(color, radius = 1.5.dp.toPx(), center = Offset(w*.5f,h*.72f))
            }
            "clock" -> {
                drawCircle(color, radius = w * .43f, style = stroke)
                path.moveTo(w*.5f,h*.2f); path.lineTo(w*.5f,h*.5f); path.lineTo(w*.7f,h*.65f)
            }
            "delete" -> {
                path.moveTo(w*.2f,h*.25f); path.lineTo(w*.8f,h*.25f)
                path.moveTo(w*.38f,h*.25f); path.lineTo(w*.38f,h*.1f); path.lineTo(w*.62f,h*.1f); path.lineTo(w*.62f,h*.25f)
                path.moveTo(w*.28f,h*.3f); path.lineTo(w*.32f,h*.9f); path.lineTo(w*.68f,h*.9f); path.lineTo(w*.72f,h*.3f)
                path.moveTo(w*.45f,h*.4f); path.lineTo(w*.45f,h*.75f)
                path.moveTo(w*.57f,h*.4f); path.lineTo(w*.57f,h*.75f)
            }
            "copy" -> {
                path.moveTo(w*.65f,h*.1f); path.lineTo(w*.15f,h*.1f); path.lineTo(w*.15f,h*.7f)
                path.moveTo(w*.35f,h*.3f); path.lineTo(w*.85f,h*.3f); path.lineTo(w*.85f,h*.9f); path.lineTo(w*.35f,h*.9f); path.close()
            }
            "download" -> {
                path.moveTo(w*.5f,h*.1f); path.lineTo(w*.5f,h*.7f)
                path.moveTo(w*.25f,h*.45f); path.lineTo(w*.5f,h*.7f); path.lineTo(w*.75f,h*.45f)
                path.moveTo(w*.15f,h*.9f); path.lineTo(w*.85f,h*.9f)
            }
            "upload" -> {
                path.moveTo(w*.5f,h*.65f); path.lineTo(w*.5f,h*.1f)
                path.moveTo(w*.25f,h*.35f); path.lineTo(w*.5f,h*.1f); path.lineTo(w*.75f,h*.35f)
                path.moveTo(w*.1f,h*.6f); path.lineTo(w*.1f,h*.9f); path.lineTo(w*.9f,h*.9f); path.lineTo(w*.9f,h*.6f)
            }
            "file" -> {
                path.moveTo(w*.2f,h*.05f); path.lineTo(w*.6f,h*.05f); path.lineTo(w*.85f,h*.3f)
                path.lineTo(w*.85f,h*.95f); path.lineTo(w*.2f,h*.95f); path.close()
                path.moveTo(w*.6f,h*.05f); path.lineTo(w*.6f,h*.3f); path.lineTo(w*.85f,h*.3f)
                for (y in listOf(.5f,.65f,.8f)) { path.moveTo(w*.35f,h*y); path.lineTo(w*.7f,h*y) }
            }
            "success" -> {
                drawCircle(Success)
                path.moveTo(w*.22f,h*.5f); path.lineTo(w*.43f,h*.72f); path.lineTo(w*.8f,h*.28f)
            }
            else -> {
                path.moveTo(w*.5f,h*.05f)
                path.quadraticTo(w*.56f,h*.44f,w*.95f,h*.5f)
                path.quadraticTo(w*.56f,h*.56f,w*.5f,h*.95f)
                path.quadraticTo(w*.44f,h*.56f,w*.05f,h*.5f)
                path.quadraticTo(w*.44f,h*.44f,w*.5f,h*.05f); path.close()
            }
        }
        drawPath(path, color, style = stroke)
    }
}
