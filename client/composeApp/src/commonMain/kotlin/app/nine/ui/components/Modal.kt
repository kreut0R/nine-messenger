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
 * Окно поверх всего приложения: затемнение и [content] по центру. Закрывается по Escape и по клику на затемнение.
 * @param focusOnOpen забирать ли фокус при открытии, чтобы Escape работал, пока ни одно поле не выбрано.
 *   Окну, которое само ставит фокус на своё поле, нужно передать false.
 */
@Composable
fun Modal(onDismiss: () -> Unit, focusOnOpen: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val focus = remember { FocusRequester() }
    if (focusOnOpen) LaunchedEffect(Unit) { focus.requestFocus() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NineColors.Scrim)
            // Затемнение ловит все касания, иначе они дошли бы до панелей под окном.
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .then(if (focusOnOpen) Modifier.focusRequester(focus).focusable() else Modifier)
            .onPreviewKeyEvent { event ->
                val escape = event.key == Key.Escape && event.type == KeyEventType.KeyDown
                if (escape) onDismiss()
                escape
            },
        contentAlignment = Alignment.Center,
    ) {
        // Клик внутри окна не должен закрывать его через затемнение.
        Box(Modifier.pointerInput(Unit) { detectTapGestures { } }, content = content)
    }
}
