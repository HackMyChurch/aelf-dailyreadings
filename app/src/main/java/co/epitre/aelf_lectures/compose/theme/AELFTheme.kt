package co.epitre.aelf_lectures.compose.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider


@Composable
fun AELFTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = colors.textSelectionHandle,
            backgroundColor = colors.textSelectionBackground
        )
    ) {
        content()
    }
}