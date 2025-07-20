package co.epitre.aelf_lectures.bible.v2.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.takeOrElse
import androidx.preference.PreferenceManager
import co.epitre.aelf_lectures.bible.v2.composeTheme.Typo
import co.epitre.aelf_lectures.bible.v2.composeTheme.colors
import co.epitre.aelf_lectures.settings.SettingsActivity
import java.text.Normalizer

@Composable
fun TextWithZoom(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
    zoom: Float = 1f
) {

    val textColor = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }

    BasicText(
        text,
        modifier,
        style.merge(
            color = textColor,
            fontSize = (fontSize.takeOrElse { style.fontSize }) * zoom,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing,
            lineBreak = LineBreak.Simple
        ),
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        minLines,
    )
}



@Composable
fun TextWithZoomAndHighlights(
    text: String,
    searchRegex: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
    zoom: Float = 1f
) {

    val textColor = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    val normalizedText = normalize(text)

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        for (match in wildcardToRegex(searchRegex).findAll(normalizedText)) {
            val start = match.range.first
            val end = match.range.last + 1

            append(text.substring(lastIndex, start))
            withStyle(SpanStyle(
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            )) {
                append(text.substring(start, end))
            }
            lastIndex = end
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    BasicText(
        annotatedString,
        modifier,
        style.merge(
            color = textColor,
            fontSize = (fontSize.takeOrElse { style.fontSize }) * zoom,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing,
            lineBreak = LineBreak.Simple
        ),
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        minLines,
    )
}



@Preview(showBackground = true, backgroundColor = 0xFFF0EAE2)
@Composable
private fun PreviewHighlightedText(
    text: String = previewBibleVerse.text + " " + previewBibleVerse.text
) {
    TextWithZoomAndHighlights(
        text, color = colors.textNeutral,
        style = Typo.body,
        zoom = 1f,
        searchRegex = "dieu cre"
    )
}

private fun normalize(str: String): String {
    return Normalizer.normalize(str, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}

private fun wildcardToRegex(pattern: String): Regex {
    val escaped = Regex.escape(normalize(pattern.removeSuffix("*").trim()))
    return Regex("\\b$escaped[\\p{L}]*", RegexOption.IGNORE_CASE)
}
