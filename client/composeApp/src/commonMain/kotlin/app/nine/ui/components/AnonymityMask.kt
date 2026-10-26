package app.nine.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import app.nine.ui.theme.NineColors

// Контур маски в координатах 400×200. Два выреза под глаза работают за счёт правила чёт-нечет (EvenOdd).
private const val MASK_PATH =
    "M10 80C40 38 120 28 200 68C280 28 360 38 390 80C396 134 352 176 296 172C250 169 226 142 200 142" +
        "C174 142 150 169 104 172C48 176 4 134 10 80Z" +
        "M66 100C92 80 138 84 160 108C140 130 94 134 66 100Z" +
        "M334 100C308 80 262 84 240 108C260 130 306 134 334 100Z"

private const val MASK_WIDTH = 400f
private const val MASK_HEIGHT = 200f

/**
 * Маска анонимности: знак того, что в Nine не нужны телефон и почта.
 * Рисуется бледной и слегка размытой, чтобы быть фоном, а не элементом управления.
 */
@Composable
fun AnonymityMask(modifier: Modifier = Modifier) {
    val path = remember { PathParser().parsePathString(MASK_PATH).toPath().apply { fillType = PathFillType.EvenOdd } }
    Canvas(
        modifier = modifier
            .aspectRatio(MASK_WIDTH / MASK_HEIGHT)
            .alpha(0.36f)
            .blur(1.2.dp),
    ) {
        scale(scaleX = size.width / MASK_WIDTH, scaleY = size.height / MASK_HEIGHT, pivot = Offset.Zero) {
            drawPath(path, NineColors.MaskFill)
            drawPath(path, NineColors.MaskStroke, style = Stroke(width = 2f))
        }
    }
}
