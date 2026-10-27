package app.nine.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The app mark: an asterisk of three thick lines with round ends, crossing in the center. */
@Composable
fun NineLogo(color: Color, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Canvas(modifier.size(size)) {
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val reach = this.size.width / 2 * 0.86f
        for (degrees in listOf(90.0, 30.0, 150.0)) {
            val angle = degrees * PI / 180
            val dx = (cos(angle) * reach).toFloat()
            val dy = (sin(angle) * reach).toFloat()
            drawLine(color, center - Offset(dx, dy), center + Offset(dx, dy), strokeWidth = this.size.width * 0.17f, cap = StrokeCap.Round)
        }
    }
}
