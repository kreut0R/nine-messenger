package app.nine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import app.nine.ui.theme.NineColors

/** Corner radius of panels, bubbles and fields. The corners are deliberately sharp, only avatars stay round. */
val PanelShape = RoundedCornerShape(12.dp)

/**
 * A glass panel: a dark backing, a thin light outline and a highlight on the top edge.
 * There is no real blur of what is under the panel: Compose Multiplatform cannot blur the background yet.
 * The glass is conveyed by the outline and highlights, and the header and the input field in a conversation are translucent.
 */
@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(PanelShape)
            .background(NineColors.Panel)
            .border(1.dp, NineColors.Rim, PanelShape)
            .drawWithContent {
                drawContent()
                val inset = 12.dp.toPx()
                drawLine(
                    color = NineColors.RimHighlight,
                    start = Offset(inset, 0.5f),
                    end = Offset(size.width - inset, 0.5f),
                    strokeWidth = 1f,
                )
            },
        content = content,
    )
}
