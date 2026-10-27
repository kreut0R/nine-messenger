package app.nine.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.nine.ui.theme.NineColors

/** Выключатель. Включённый горит зелёным: зелёный в приложении означает «действует». */
@Composable
fun ToggleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val track by animateColorAsState(if (checked) NineColors.Accent else Color.White.copy(alpha = 0.18f))
    val knobX by animateDpAsState(if (checked) 20.dp else 2.dp)
    Box(
        modifier = modifier
            .size(width = 42.dp, height = 24.dp)
            .clip(CircleShape)
            .background(track)
            .pointerHoverIcon(PointerIcon.Hand)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset { IntOffset(knobX.roundToPx(), 0) }
                .size(20.dp)
                .clip(CircleShape)
                .background(if (checked) NineColors.OnBadge else NineColors.TextPrimary),
        )
    }
}
