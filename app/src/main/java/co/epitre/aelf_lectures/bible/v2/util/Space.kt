package co.epitre.aelf_lectures.bible.v2.util

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@SuppressLint("ModifierParameter")
@Composable
fun ColumnScope.Space(height: Dp? = null, weight: Float? = null, modifier: Modifier = Modifier) {
    Spacer(
        modifier = Modifier
            .ifNotNull(height) { height(it) }
            .ifNotNull(weight) { weight(it) }
            .then(modifier),
    )
}

@SuppressLint("ModifierParameter")
@Composable
fun RowScope.Space(width: Dp? = null, weight: Float? = null, modifier: Modifier = Modifier) {
    Spacer(
        modifier = Modifier
            .ifNotNull(width) { width(it) }
            .ifNotNull(weight) { weight(it) }
            .then(modifier),
    )
}