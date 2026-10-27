package app.nine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import app.nine.ui.theme.NineColors

/**
 * A window over the whole app: a dim layer and [content] in the center. Closes on Escape and on a click on the dim layer.
 * @param focusOnOpen whether to take focus on open so that Escape works while no field is selected.
 *   A window that puts focus on its own field itself should pass false.
 */
@Composable
fun Modal(onDismiss: () -> Unit, focusOnOpen: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val focus = remember { FocusRequester() }
    if (focusOnOpen) LaunchedEffect(Unit) { focus.requestFocus() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NineColors.Scrim)
            // The dim layer catches all touches, otherwise they would reach the panels under the window.
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .then(if (focusOnOpen) Modifier.focusRequester(focus).focusable() else Modifier)
            .onPreviewKeyEvent { event ->
                val escape = event.key == Key.Escape && event.type == KeyEventType.KeyDown
                if (escape) onDismiss()
                escape
            },
        contentAlignment = Alignment.Center,
    ) {
        // A click inside the window must not close it through the dim layer.
        Box(Modifier.pointerInput(Unit) { detectTapGestures { } }, content = content)
    }
}
