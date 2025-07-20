package co.epitre.aelf_lectures.bible.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.epitre.aelf_lectures.bible.data.BibleVerse
import co.epitre.aelf_lectures.bible.v2.composeTheme.Typo
import co.epitre.aelf_lectures.bible.v2.composeTheme.colors
import co.epitre.aelf_lectures.bible.v2.composeTheme.spacing
import co.epitre.aelf_lectures.bible.v2.util.Space

@Composable
fun BibleVerseComponent(
    ref: String,
    text: String,
    zoom: Float,
    isFocued: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier
            .height(IntrinsicSize.Max)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                onClick()
            }) {
        Row {
            DisableSelection {
                TextWithZoom(
                    ref,
                    color = colors.textAnnotation,
                    style = Typo.verse,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .alignByBaseline()
                        .widthIn((12 * zoom).dp),
                    zoom = zoom
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 1.dp, end = 3.dp)
                    .fillMaxHeight()
                    .width(1.5.dp)
                    .background(if (isFocued) colors.focusText else Color.Transparent)
            )

            TextWithZoom(
                text, color = colors.textNeutral,
                style = Typo.body,
                modifier = Modifier.alignByBaseline(),
                zoom = zoom
            )
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
        isFocued = true
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