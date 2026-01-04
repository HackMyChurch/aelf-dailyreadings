package co.epitre.aelf_lectures.bible.biblebookfragment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import co.epitre.aelf_lectures.bible.data.BibleVerse
import co.epitre.aelf_lectures.compose.theme.Typo
import co.epitre.aelf_lectures.compose.theme.colors
import co.epitre.aelf_lectures.compose.theme.spacing
import co.epitre.aelf_lectures.compose.utils.Space
import co.epitre.aelf_lectures.compose.utils.ifThen

@Composable
fun BibleVerseComponent(
    ref: String,
    text: String,
    zoom: Float,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    isHighlighted: Boolean = false,
    searchQuery: String? = null,
    onClick: () -> Unit = {}
) {
    Column(
        modifier
            .height(IntrinsicSize.Max)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val up = waitForUpOrCancellation()
                        if (up != null && !up.isConsumed) {
                            onClick()
                        }
                    }
                }
            }) {
        Row {
            DisableSelection {
                TextWithZoom(
                    ref,
                    color = colors.textAnnotation,
                    style = Typo.verse,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .align(Alignment.Top)
                        .padding(top = 1.dp * zoom)
                        .widthIn((12 * zoom).dp),
                    zoom = zoom
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 1.dp, end = 3.dp)
                    .fillMaxHeight()
                    .width(1.5.dp)
                    .background(if (isFocused) colors.focusText else Color.Transparent)
            )

            if (searchQuery != null && searchQuery.isNotEmpty()) {
                TextWithZoomAndHighlights(
                    text,
                    color = colors.textNeutral,
                    searchRegex = searchQuery,
                    style = Typo.body,
                    modifier = Modifier
                        .alignByBaseline()
                        .ifThen(isHighlighted) { background(colors.highlightBackground) },
                    zoom = zoom
                )
            } else {
                TextWithZoom(
                    text, color = colors.textNeutral,
                    style = Typo.body,
                    modifier = Modifier
                        .alignByBaseline()
                        .ifThen(isHighlighted) { background(colors.highlightBackground) },
                    zoom = zoom
                )
            }

        }
        Space(spacing.s38)
    }
}


val previewBibleVerse = BibleVerse(
    "1",
    "AU COMMENCEMENT, Dieu créa le ciel et la terre."
)


@Preview(showBackground = true, backgroundColor = 0xFFF0EAE2)
@Composable
fun PreviewBibleVerse(
    ref: String = previewBibleVerse.ref,
    text: String = previewBibleVerse.text
) {
    BibleVerseComponent(
        ref = ref,
        text = text,
        zoom = 1f,
        isFocused = true,
        isHighlighted = true
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0EAE2)
@Composable
fun PreviewBibleVerse2(
    ref: String = previewBibleVerse.ref,
    text: String = previewBibleVerse.text
) {
    PreviewBibleVerse("12")
}

@Composable
fun Int.charsWidth(fontSize: TextUnit): Dp {
    val density = LocalDensity.current
    return with(density) {
        (this@charsWidth * fontSize.toPx() * 0.6f).toDp()
    }
}
