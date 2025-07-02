package co.epitre.aelf_lectures.bible.v2.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.padding(bottom = spacing.s38)) {
        DisableSelection {
            TextWithZoom(
                ref,
                color = colors.textAnnotation,
                style = Typo.verse,
                modifier = Modifier.alignByBaseline(),
                zoom = zoom
            )
            Space(spacing.s38)
        }

        TextWithZoom(
            text, color = colors.textNeutral,
            style = Typo.body,
            modifier = Modifier.alignByBaseline(),
            zoom = zoom
        )
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
        zoom = 1f
    )
}