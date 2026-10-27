package app.nine.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.nine.ui.theme.NineColors
import app.nine.ui.theme.NineTheme
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** An icon button with a hover highlight. For round buttons pass [shape] = CircleShape. */
@Composable
fun IconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NineColors.TextSecondary,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    shape: Shape = RoundedCornerShape(10.dp),
) {
    Box(
        modifier = modifier
            .size(size)
            .pressable(onClick, shape, hoverBackground = NineColors.ControlHover)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        NineIcon(icon, tint = tint)
    }
}

/** A one-line field with a magnifier icon. Used for searching chats and conversations. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val type = NineTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(PanelShape)
            .background(NineColors.Control)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NineIcon(NineIcons.Search, tint = NineColors.TextTertiary, size = 18.dp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = type.field,
            cursorBrush = SolidColor(NineColors.Accent),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) BasicText(placeholder, style = type.hint)
                    inner()
                }
            },
        )
    }
}

/**
 * A switch of several tabs on a gray strip. The selected tab "slides" smoothly.
 * A thin divider is drawn between unselected neighbors, as in the mockup.
 */
@Composable
fun SegmentedTabs(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = NineTheme.typography
    val tabHeight = 34.dp
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(NineColors.Control)
            .padding(3.dp),
    ) {
        val tabWidth = maxWidth / titles.size
        val thumbX by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        )
        Box(
            Modifier
                .offset { IntOffset(thumbX.roundToPx(), 0) }
                .width(tabWidth)
                .height(tabHeight)
                .clip(RoundedCornerShape(7.dp))
                .background(NineColors.TabThumb),
        )
        Row {
            titles.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                val showDivider = index > 0 && selectedIndex != index && selectedIndex != index - 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(tabHeight)
                        .pressable({ onSelect(index) }, RoundedCornerShape(7.dp), hoverBackground = Color.Transparent)
                        .drawBehind {
                            if (showDivider) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.12f),
                                    start = Offset(0f, size.height * 0.3f),
                                    end = Offset(0f, size.height * 0.7f),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(title, style = if (selected) type.tabSelected else type.tab, maxLines = 1)
                }
            }
        }
    }
}
