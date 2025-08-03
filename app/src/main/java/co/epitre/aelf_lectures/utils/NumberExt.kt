package co.epitre.aelf_lectures.utils

import kotlin.math.pow
import kotlin.math.roundToInt

fun Float.round(decimals: Int): Float {
    require(decimals >= 0)
    val factor = 10.0.pow(decimals)
    return (this * factor).roundToInt() / factor.toFloat()
}