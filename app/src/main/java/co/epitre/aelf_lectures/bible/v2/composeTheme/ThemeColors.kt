package co.epitre.aelf_lectures.bible.v2.composeTheme

import androidx.compose.ui.graphics.Color

interface ThemeColors {
    val textAnnotation: Color
    val textNeutral: Color
    val surfaceFilled: Color
    val onSurfaceFilled: Color
    val focusText: Color
    val tabText: Color
}

val lightThemeColors = object : ThemeColors {
    override val textAnnotation: Color = ColorPrimitives.DARK_RED
    override val textNeutral: Color = ColorPrimitives.DARK_BROWN
    override val surfaceFilled: Color = ColorPrimitives.DARK_RED
    override val onSurfaceFilled: Color = ColorPrimitives.WHITE
    override val focusText: Color = ColorPrimitives.DARK_RED
    override val tabText: Color = ColorPrimitives.LIGHT_BEIGE
}

val darkThemeColors = object : ThemeColors {
    override val textAnnotation: Color = ColorPrimitives.PINK_RED
    override val textNeutral: Color = ColorPrimitives.ALMOST_WHITE
    override val surfaceFilled: Color = ColorPrimitives.ALMOST_BLACK
    override val onSurfaceFilled: Color = ColorPrimitives.PINK_RED
    override val focusText: Color = ColorPrimitives.DARK_RED
    override val tabText: Color = ColorPrimitives.PINK_RED
}