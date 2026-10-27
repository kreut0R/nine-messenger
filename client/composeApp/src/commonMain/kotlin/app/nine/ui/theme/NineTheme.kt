package app.nine.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Sets up the font and the text selection color. The whole app must live inside this function. */
@Composable
fun NineTheme(content: @Composable () -> Unit) {
    val typography = rememberTypography()
    val selection = TextSelectionColors(
        handleColor = NineColors.Accent,
        backgroundColor = NineColors.Accent.copy(alpha = 0.35f),
    )
    CompositionLocalProvider(
        provideTypography(typography),
        LocalTextSelectionColors provides selection,
        content = content,
    )
}
