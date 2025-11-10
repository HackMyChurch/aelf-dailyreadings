package co.epitre.aelf_lectures.compose.theme

import androidx.compose.ui.graphics.Color

fun rgba(red: Int, green: Int, blue: Int, alpha: Double): Color =
    Color(red, green, blue, (alpha * 255).toInt())


object ColorPrimitives {
    val DARK_RED = rgba(191, 35, 41, 1.0)
    val DARK_BROWN = rgba(93, 69, 26, 1.0)
    val WHITE = Color(0xFFFFFFFF)
    val LIGHT_BEIGE = Color(0xFFEFE3CE)
    val ALMOST_BLACK = Color(0xFF1D1E23)
    val ALMOST_WHITE = Color(0xFFF8F7FA)
    val PINK_RED = Color(0xFFF9787E)
    val CYAN = Color(0xFFA5CFD9)
    val PETROL_BLUE = Color(0xFF1E475A)
    val DARK_GRAY = Color(0xFF303030)
}