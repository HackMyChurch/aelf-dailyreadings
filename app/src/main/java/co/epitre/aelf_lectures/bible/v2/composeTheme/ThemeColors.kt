package co.epitre.aelf_lectures.bible.v2.composeTheme

import androidx.compose.ui.graphics.Color

interface ThemeColors {
    val textAnnotation: Color
    val textNeutral: Color
    val surfaceFilled: Color
    val onSurfaceFilled: Color
}

val lightThemeColors = object : ThemeColors {
    override val textAnnotation: Color = ColorPrimitives.DARK_RED
    override val textNeutral: Color = ColorPrimitives.DARK_BROWN
    override val surfaceFilled: Color = ColorPrimitives.DARK_RED
    override val onSurfaceFilled: Color = ColorPrimitives.WHITE
}