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

/** Радиус скругления панелей, пузырей и полей. Углы намеренно острые, круглыми остаются только аватарки. */
val PanelShape = RoundedCornerShape(12.dp)

/**
 * Стеклянная панель: тёмная подложка, тонкий светлый контур и блик на верхней кромке.
 * Настоящего размытия того, что под панелью, здесь нет: Compose Multiplatform пока не умеет размывать фон.
 * Стекло передаётся контуром и бликами, а шапка с полем ввода в переписке полупрозрачны.
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
