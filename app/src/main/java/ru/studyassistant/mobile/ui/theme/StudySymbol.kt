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
                path.moveTo(w*.5f,0f); path.lineTo(w*.65f,h*.35f); path.lineTo(w,h*.5f)
                path.lineTo(w*.65f,h*.65f); path.lineTo(w*.5f,h); path.lineTo(w*.35f,h*.65f)
                path.lineTo(0f,h*.5f); path.lineTo(w*.35f,h*.35f); path.close()
            }
        }
        drawPath(path, color, style = stroke)
    }
}
