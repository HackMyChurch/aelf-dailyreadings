package co.epitre.aelf_lectures.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

@Composable
fun Modifier.ifThen(condition: Boolean, modifier: @Composable Modifier.() -> Modifier) = if (condition) this then modifier() else this

@Composable
fun <T> Modifier.ifNotNull(value: T?, modifier: @Composable Modifier.(T) -> Modifier) = if (value != null) this then modifier(value) else this

@Composable
fun Number.pxToDp() = with(LocalDensity.current) { this@pxToDp.toFloat().toDp() }