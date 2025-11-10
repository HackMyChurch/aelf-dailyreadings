package co.epitre.aelf_lectures.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

interface ThemeColors {
    val textAnnotation: Color
    val textNeutral: Color
    val surfaceFilled: Color
    val onSurfaceFilled: Color
    val focusText: Color
    val tabText: Color
    val highlightBackground: Color
    val textSelectionHandle: Color
    val textSelectionBackground: Color
    val elevatedSurface: Color
    val onElevatedSurface: Color
}

val lightThemeColors = object : ThemeColors {
    override val textAnnotation: Color = ColorPrimitives.DARK_RED
    override val textNeutral: Color = ColorPrimitives.DARK_BROWN
    override val surfaceFilled: Color = ColorPrimitives.DARK_RED
    override val onSurfaceFilled: Color = ColorPrimitives.WHITE
    override val focusText: Color = ColorPrimitives.DARK_RED
    override val tabText: Color = ColorPrimitives.LIGHT_BEIGE
    override val highlightBackground: Color = ColorPrimitives.DARK_RED.copy(alpha = .12f)
    override val textSelectionHandle: Color = ColorPrimitives.DARK_RED
    override val textSelectionBackground: Color = ColorPrimitives.CYAN
    override val elevatedSurface: Color = ColorPrimitives.WHITE
    override val onElevatedSurface: Color = ColorPrimitives.ALMOST_BLACK
}

val darkThemeColors = object : ThemeColors {
    override val textAnnotation: Color = ColorPrimitives.PINK_RED
    override val textNeutral: Color = ColorPrimitives.ALMOST_WHITE
    override val surfaceFilled: Color = ColorPrimitives.ALMOST_BLACK
    override val onSurfaceFilled: Color = ColorPrimitives.PINK_RED
    override val focusText: Color = ColorPrimitives.PINK_RED
    override val tabText: Color = ColorPrimitives.PINK_RED
    override val highlightBackground: Color = ColorPrimitives.PINK_RED.copy(alpha = .12f)
    override val textSelectionHandle: Color = ColorPrimitives.PINK_RED
    override val textSelectionBackground: Color = ColorPrimitives.PETROL_BLUE
    override val elevatedSurface: Color = ColorPrimitives.DARK_GRAY
    override val onElevatedSurface: Color = ColorPrimitives.ALMOST_WHITE
}


val colors
    @Composable
    get() = if (isSystemInDarkTheme()) darkThemeColors else lightThemeColors