package app.nine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role

/**
 * Makes an area a button: hand cursor, its own background at rest and another on mouse hover.
 * Colors are passed explicitly because a selected list item has a special background that hover does not change.
 */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    shape: Shape,
    hoverBackground: Color,
    background: Color = Color.Transparent,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    return this
        .clip(shape)
        .background(if (hovered) hoverBackground else background)
        .hoverable(source)
        .pointerHoverIcon(PointerIcon.Hand)
        .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
}
