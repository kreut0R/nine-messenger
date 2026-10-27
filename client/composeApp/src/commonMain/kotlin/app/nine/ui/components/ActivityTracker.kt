package app.nine.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Сообщает [onActivity] о любом движении мыши, нажатии и клавише внутри. Ничего не перехватывает:
 * события идут дальше как обычно. Нужен автоблокировке, чтобы знать, когда человек последний раз что-то делал.
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
