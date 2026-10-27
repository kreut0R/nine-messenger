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
    val Close = lineIcon("Close", "M6 6l12 12M18 6L6 18")
    val Sliders = lineIcon(
        "Sliders",
        "M4 7h7M17 7h3M4 17h3M13 17h7",
        "M17 7a2 2 0 1 1 -4 0a2 2 0 1 1 4 0",
        "M13 17a2 2 0 1 1 -4 0a2 2 0 1 1 4 0",
    )
    val Lock = lineIcon(
        "Lock",
        "M6.5 10.5h11a1.5 1.5 0 0 1 1.5 1.5v6.5a1.5 1.5 0 0 1 -1.5 1.5h-11a1.5 1.5 0 0 1 -1.5 -1.5V12a1.5 1.5 0 0 1 1.5 -1.5z",
        "M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5",
    )
    val SignOut = lineIcon(
        "SignOut",
        "M10 5H6.5A1.5 1.5 0 0 0 5 6.5v11A1.5 1.5 0 0 0 6.5 19H10",
        "M15 8l4 4l-4 4M19 12H9",
    )
    val Shield = lineIcon("Shield", "M12 3.5l7 2.5v5.5c0 4.2 -2.9 7.4 -7 9c-4.1 -1.6 -7 -4.8 -7 -9V6z")
    val ChevronLeft = lineIcon("ChevronLeft", "M14.5 6l-6 6l6 6")
    val Key = lineIcon(
        "Key",
        "M14.5 9.5a4 4 0 1 1 -8 0a4 4 0 1 1 8 0",
        "M13.3 12.3L19.5 18.5M16.5 15.5l2 -2",
    )
    val Clock = lineIcon(
        "Clock",
        "M20.5 12a8.5 8.5 0 1 1 -17 0a8.5 8.5 0 1 1 17 0",
        "M12 7.5V12l3 2",
    )
    val Warning = lineIcon("Warning", "M12 4.5l9 15.5H3z", "M12 10.5v4M12 17.3v.01")
    val Trash = lineIcon("Trash", "M5 7h14M10 7V5h4v2M7 7l1 12h8l1 -12")
    val Alert = lineIcon(
        "Alert",
        "M20.5 12a8.5 8.5 0 1 1 -17 0a8.5 8.5 0 1 1 17 0",
        "M12 7.5v5M12 16v.01",
    )
    val NoScreen = lineIcon(
        "NoScreen",
        "M5 7h14a1 1 0 0 1 1 1v9a1 1 0 0 1 -1 1h-14a1 1 0 0 1 -1 -1v-9a1 1 0 0 1 1 -1z",
        "M4 4l16 16",
    )
    val Link = lineIcon(
        "Link",
        "M10 14a4 4 0 0 0 5.7 0l3 -3a4 4 0 0 0 -5.7 -5.7l-1 1",
        "M14 10a4 4 0 0 0 -5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1 -1",
    )
    val Clipboard = lineIcon(
        "Clipboard",
        "M9 4.5h6a1 1 0 0 1 1 1V7H8V5.5a1 1 0 0 1 1 -1z",
        "M16 6h1.5A1.5 1.5 0 0 1 19 7.5v11a1.5 1.5 0 0 1 -1.5 1.5h-11A1.5 1.5 0 0 1 5 18.5v-11A1.5 1.5 0 0 1 6.5 6H8",
    )
    val Chat = lineIcon(
        "Chat",
        "M5 6.5A1.5 1.5 0 0 1 6.5 5h11A1.5 1.5 0 0 1 19 6.5v8a1.5 1.5 0 0 1 -1.5 1.5H11l-4 3.5V16h-.5A1.5 1.5 0 0 1 5 14.5z",
    )
    val Photo = lineIcon(
        "Photo",
        "M5 5.5h14a1.5 1.5 0 0 1 1.5 1.5v10a1.5 1.5 0 0 1 -1.5 1.5H5A1.5 1.5 0 0 1 3.5 17V7A1.5 1.5 0 0 1 5 5.5z",
        "M8.5 10a1.5 1.5 0 1 1 -3 0a1.5 1.5 0 1 1 3 0",
        "M4 17l5 -5l4 4l2.5 -2.5L20 18",
    )
    val Keyboard = lineIcon(
        "Keyboard",
        "M4.5 7h15a1 1 0 0 1 1 1v8a1 1 0 0 1 -1 1h-15a1 1 0 0 1 -1 -1V8a1 1 0 0 1 1 -1z",
        "M7 10.5h.01M10 10.5h.01M13 10.5h.01M16 10.5h.01M8 14h8",
    )
    val Globe = lineIcon(
        "Globe",
        "M20.5 12a8.5 8.5 0 1 1 -17 0a8.5 8.5 0 1 1 17 0",
        "M3.5 12h17",
        "M12 3.5c2.5 2.4 3.5 5.3 3.5 8.5s-1 6.1 -3.5 8.5c-2.5 -2.4 -3.5 -5.3 -3.5 -8.5s1 -6.1 3.5 -8.5z",
    )
    val Moon = lineIcon("Moon", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z")
    val Database = lineIcon(
        "Database",
        "M5 7c0 -1.4 3.1 -2.5 7 -2.5s7 1.1 7 2.5s-3.1 2.5 -7 2.5s-7 -1.1 -7 -2.5z",
        "M5 7v10c0 1.4 3.1 2.5 7 2.5s7 -1.1 7 -2.5V7",
        "M5 12c0 1.4 3.1 2.5 7 2.5s7 -1.1 7 -2.5",
    )
    val Info = lineIcon(
        "Info",
        "M20.5 12a8.5 8.5 0 1 1 -17 0a8.5 8.5 0 1 1 17 0",
        "M12 11v5M12 8v.01",
    )
    val Users = lineIcon(
        "Users",
        "M12.5 8a3.5 3.5 0 1 1 -7 0a3.5 3.5 0 1 1 7 0",
        "M3 19c.6 -3 3 -4.5 6 -4.5s5.4 1.5 6 4.5",
        "M16 5.2a3.2 3.2 0 0 1 0 6.1",
        "M17.5 14.6c1.8 .5 3 1.9 3.5 4.4",
    )
    val Download = lineIcon("Download", "M12 4v11M7.5 11l4.5 4.5l4.5 -4.5M5 19.5h14")
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
