package app.nine.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Reports any mouse movement, press or key inside to [onActivity]. Intercepts nothing:
 * events go on as usual. The autolock needs it to know when the person last did anything.
 */
fun Modifier.trackActivity(onActivity: () -> Unit): Modifier = this
    .onPreviewKeyEvent {
        onActivity()
        false
    }
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                onActivity()
            }
        }
    }
