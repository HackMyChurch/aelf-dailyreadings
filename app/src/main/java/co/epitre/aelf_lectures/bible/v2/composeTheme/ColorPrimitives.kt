package co.epitre.aelf_lectures.bible.v2.composeTheme

import androidx.compose.ui.graphics.Color

fun rgba(red: Int, green: Int, blue: Int, alpha: Double): Color =
    Color(red, green, blue, (alpha * 255).toInt())


object ColorPrimitives {
    val DARK_RED = rgba(191, 35, 41, 1.0)
    val DARK_BROWN = rgba(93, 69, 26, 1.0)
    val WHITE = Color(0xFFFFFFFF)
}