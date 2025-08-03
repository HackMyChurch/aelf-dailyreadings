package co.epitre.aelf_lectures.bible.v2.composeTheme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable


val colors
    @Composable
    get() = if (isSystemInDarkTheme()) darkThemeColors else lightThemeColors


val spacing
    get() = Spacing


@Composable
fun ComposeTheme() {
    MaterialTheme {

    }
}