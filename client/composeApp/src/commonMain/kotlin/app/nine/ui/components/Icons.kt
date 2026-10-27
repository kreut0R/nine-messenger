package app.nine.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

/**
 * Иконки приложения: тонкие линии на сетке 24×24, как в макете.
 * Рисуются чёрным цветом и перекрашиваются при показе, см. [NineIcon].
 */
object NineIcons {
    val Search = lineIcon(
        "Search",
        "M17.5 11a6.5 6.5 0 1 1 -13 0a6.5 6.5 0 1 1 13 0",
        "M16 16l4.5 4.5",
    )
    val PlusCircle = lineIcon(
        "PlusCircle",
        "M21 12a9 9 0 1 1 -18 0a9 9 0 1 1 18 0",
        "M12 8v8M8 12h8",
    )
    val SidePanel = lineIcon(
        "SidePanel",
        "M6.5 4.5h11a3 3 0 0 1 3 3v9a3 3 0 0 1 -3 3h-11a3 3 0 0 1 -3 -3v-9a3 3 0 0 1 3 -3z",
        "M14.5 4.5v15",
    )
    val Check = lineIcon("Check", "M5 12.5l4.2 4.2L19 7.5")
    val DoubleCheck = lineIcon("DoubleCheck", "M2.5 12.5l4.2 4.2L15 8", "M10 15.5l1.2 1.2L21 7.5")
    val Send = lineIcon("Send", "M12 19V5M6 11l6-6 6 6")
    val Eye = lineIcon(
        "Eye",
        "M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z",
        "M15 12a3 3 0 1 1 -6 0a3 3 0 1 1 6 0",
    )
    val EyeOff = lineIcon(
        "EyeOff",
        "M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z",
        "M15 12a3 3 0 1 1 -6 0a3 3 0 1 1 6 0",
        "M4 4l16 16",
    )
    val Person = lineIcon(
        "Person",
        "M16 8a4 4 0 1 1 -8 0a4 4 0 1 1 8 0",
        "M4.5 20c.9-3.4 3.7-5.2 7.5-5.2s6.6 1.8 7.5 5.2",
    )
}

private fun lineIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        for (path in paths) {
            addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/** Рисует иконку нужным цветом. [contentDescription] нужен для всего, что нажимается или несёт смысл. */
@Composable
fun NineIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    contentDescription: String? = null,
) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}
