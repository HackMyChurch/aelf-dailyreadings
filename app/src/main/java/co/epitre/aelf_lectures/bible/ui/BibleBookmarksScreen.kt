package co.epitre.aelf_lectures.bible.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.epitre.aelf_lectures.R
import co.epitre.aelf_lectures.bible.BibleBookFilterSheet
import co.epitre.aelf_lectures.bible.BibleTreeRepository
import co.epitre.aelf_lectures.bible.computeFilterResult
import co.epitre.aelf_lectures.bible.data.bible.BibleBookList
import co.epitre.aelf_lectures.bible.data.bookmark.BibleBookmark
import co.epitre.aelf_lectures.bible.data.bookmark.BibleBookmarkRepository
import co.epitre.aelf_lectures.bible.data.bookmark.MockBibleBookmarkRepository
import co.epitre.aelf_lectures.bible.data.bookmark.SortOrder
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleBookmarksScreen(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    repository: BibleBookmarkRepository? = null
) {
    val context = LocalContext.current
    val actualRepository = repository ?: remember(context) { BibleBookmarkRepository.getInstance(context) }
    val tree = remember { BibleTreeRepository.getTree() }

    var sortOrder by remember { mutableStateOf(SortOrder.BIBLE_ORDER) }
    var selectedParts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSections by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedBooks by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isFilterSheetVisible by remember { mutableStateOf(false) }

    var bookmarks by remember { mutableStateOf(actualRepository.getAllBookmarks(sortOrder)) }

    DisposableEffect(actualRepository, sortOrder) {
        val listener = object : BibleBookmarkRepository.OnBookmarkChangeListener {
            override fun onBookmarksChanged() {
                bookmarks = actualRepository.getAllBookmarks(sortOrder)
            }
        }
        actualRepository.registerListener(listener)
        bookmarks = actualRepository.getAllBookmarks(sortOrder)
        onDispose {
            actualRepository.unregisterListener(listener)
        }
    }

    val (filterLabel, calculatedFilter) = remember(tree, selectedParts, selectedSections, selectedBooks) {
        computeFilterResult(tree, selectedParts, selectedSections, selectedBooks)
    }

    val filteredBookmarks = remember(bookmarks, calculatedFilter) {
        if (calculatedFilter == null || calculatedFilter.isEmpty()) {
            bookmarks
        } else {
            bookmarks.filter { bm -> calculatedFilter.contains(bm.bookRef) }
        }
    }

    val bgColor = if (isSystemInDarkTheme()) colorResource(R.color.dark_aelf_lecture_background) else colorResource(R.color.light_aelf_lecture_background)
    val dividerColor = if (isSystemInDarkTheme()) Color(0xFF3B3C42) else colorResource(R.color.light_aelf_lecture_background_darker)
    val accentColor = if (isSystemInDarkTheme()) colorResource(R.color.dark_aelf_accent) else colorResource(R.color.light_aelf_accent)

    val rippleConfiguration = RippleConfiguration(
        color = accentColor,
        rippleAlpha = RippleAlpha(.20f, .20f, .20f, .20f)
    )

    CompositionLocalProvider(LocalRippleConfiguration provides rippleConfiguration) {
        Surface(color = bgColor, modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (bookmarks.isEmpty()) {
                        BookmarksHint()
                    } else {
                        BookmarkHeader(
                            filterLabel = filterLabel,
                            sortOrder = sortOrder,
                            onOpenFilter = { isFilterSheetVisible = true },
                            onSortChanged = { sortOrder = it }
                        )

                        HorizontalDivider(color = dividerColor, thickness = 1.dp)

                        BookmarksList(
                            bookmarks = filteredBookmarks,
                            onItemClick = onItemClick
                        )
                    }
                }

                if (isFilterSheetVisible) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    val scope = rememberCoroutineScope()

                    ModalBottomSheet(
                        onDismissRequest = { isFilterSheetVisible = false },
                        sheetState = sheetState,
                        containerColor = bgColor,
                        scrimColor = Color.Black.copy(alpha = 0.5f),
                        dragHandle = { }
                    ) {
                        BibleBookFilterSheet(
                            tree = tree,
                            selectedParts = selectedParts,
                            selectedSections = selectedSections,
                            selectedBooks = selectedBooks,
                            onSelectedPartsChange = { selectedParts = it },
                            onSelectedSectionsChange = { selectedSections = it },
                            onSelectedBooksChange = { selectedBooks = it },
                            onDismiss = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    isFilterSheetVisible = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkHeader(
    filterLabel: String,
    sortOrder: SortOrder,
    onOpenFilter: () -> Unit,
    onSortChanged: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isSystemInDarkTheme()) colorResource(R.color.dark_aelf_accent) else colorResource(R.color.light_aelf_accent)
    val lectureBgColor = if (isSystemInDarkTheme()) colorResource(R.color.dark_aelf_lecture_background) else colorResource(R.color.light_aelf_lecture_background)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onOpenFilter,
            modifier = Modifier
                .width(320.dp)
                .height(32.dp),
            shape = RectangleShape,
            color = lectureBgColor,
            border = BorderStroke(1.dp, accentColor)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = filterLabel,
                    color = accentColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        BookmarkSortBar(
            sortOrder = sortOrder,
            onSortChanged = onSortChanged
        )
    }
}

@Composable
private fun BookmarkSortBar(
    sortOrder: SortOrder,
    onSortChanged: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isSystemInDarkTheme()) colorResource(R.color.dark_aelf_accent) else colorResource(R.color.light_aelf_accent)
    val lectureBgColor = if (isSystemInDarkTheme()) colorResource(R.color.dark_aelf_lecture_background) else colorResource(R.color.light_aelf_lecture_background)

    val isBibleOrder = sortOrder == SortOrder.BIBLE_ORDER
    val indicatorOffset by animateDpAsState(
        targetValue = if (isBibleOrder) 0.dp else 160.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "bookmarkSortIndicatorOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .height(32.dp),
            shape = RectangleShape,
            color = lectureBgColor,
            border = BorderStroke(1.dp, accentColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(160.dp)
                        .height(32.dp)
                        .background(accentColor)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(1.dp)
                        .height(32.dp)
                        .background(accentColor)
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(160.dp)
                            .height(32.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSortChanged(SortOrder.BIBLE_ORDER) }
                            )
                    ) {
                        Text(
                            text = "Ordre Biblique",
                            color = if (isBibleOrder) lectureBgColor else accentColor,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(160.dp)
                            .height(32.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSortChanged(SortOrder.CREATION_DATE) }
                            )
                    ) {
                        Text(
                            text = "Date de création",
                            color = if (!isBibleOrder) lectureBgColor else accentColor,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksList(
    bookmarks: List<BibleBookmark>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dividerColor = if (isSystemInDarkTheme()) Color(0xFF3B3C42) else colorResource(R.color.light_aelf_lecture_background_darker)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            bottom = 8.dp + bottomInset
        )
    ) {
        items(
            items = bookmarks,
            key = { it.id }
        ) { bookmark ->
            BookmarkItem(
                bookmark = bookmark,
                onClick = {
                    val link = "https://www.aelf.org/bible/${bookmark.bookRef}/${bookmark.chapterRef}?bookmark=${URLEncoder.encode(bookmark.id, "UTF-8")}"
                    onItemClick(link)
                }
            )
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
        }
    }
}

@Composable
private fun BookmarksHint(
    modifier: Modifier = Modifier,
) {
    val textColor = if (isSystemInDarkTheme()) colorResource(R.color.dark_aelf_lecture_text) else colorResource(R.color.light_aelf_lecture_text)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val targetCenterY = constraints.maxHeight / 3f
                    val y = (targetCenterY - placeable.height / 2f)
                        .toInt()
                        .coerceAtLeast(0)
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, y)
                    }
                }
        ) {
            Text(
                text = "Aucun signet... pour le moment \uD83D\uDE07",
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pour créer votre premier signet:",
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "" +
                        " 1. Ouvrez la Bible\n" +
                        " 2. Sélectionnez un passage\n" +
                        " 3. Cliquez sur « Ajouter un signet »\n",
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun BookmarkItem(
    bookmark: BibleBookmark,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lectureBgColor = if (isSystemInDarkTheme()) colorResource(R.color.dark_aelf_lecture_background) else colorResource(R.color.light_aelf_lecture_background)
    val accentColor = if (isSystemInDarkTheme()) colorResource(R.color.dark_aelf_accent) else colorResource(R.color.light_aelf_accent)
    val textColor = if (isSystemInDarkTheme()) colorResource(R.color.dark_aelf_lecture_text) else colorResource(R.color.light_aelf_lecture_text)

    val bookEntry = BibleBookList.getInstance().findBookEntry(bookmark.bookRef)
    val bookName = bookEntry?.bookName ?: bookmark.bookRef
    val chapterObj = try {
        bookEntry?.chapters?.find { it.chapterRef.equals(bookmark.chapterRef, ignoreCase = true) }
    } catch (_: Exception) {
        null
    }
    val chapterName = chapterObj?.chapterName ?: "Chapitre ${bookmark.chapterRef}"

    val title = "$bookName, $chapterName"

    val reference = buildString {
        append("${bookmark.bookRef} ${bookmark.chapterRef}")
        if (bookmark.startVerse > 0) {
            val end = if (bookmark.endVerse > bookmark.startVerse) bookmark.endVerse else bookmark.startVerse
            if (bookmark.startVerse == end) {
                append(", ${bookmark.startVerse}")
            } else {
                append(", ${bookmark.startVerse}-$end")
            }
        }
    }

    val sanitizedText = sanitizeText(bookmark.exact)

    Surface(
        onClick = onClick,
        color = lectureBgColor,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = title,
                    color = accentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = reference,
                    color = textColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End
                )
            }

            if (sanitizedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = middleEllipsis(sanitizedText),
                    color = textColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun sanitizeText(text: String): String {
    return text.replace(Regex("""[\r\n]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private fun middleEllipsis(text: String, maxLen: Int = 140): String {
    if (text.length <= maxLen) return text
    val targetHalf = (maxLen - 7) / 2

    var leftIdx = text.lastIndexOf(' ', targetHalf)
    if (leftIdx < targetHalf / 2) {
        leftIdx = targetHalf
    }
    val start = text.take(leftIdx).trimEnd()

    val rightTarget = text.length - targetHalf
    var rightIdx = text.indexOf(' ', rightTarget)
    if (rightIdx == -1 || rightIdx > rightTarget + targetHalf) {
        rightIdx = rightTarget
    }
    val end = text.substring(rightIdx).trimStart()

    return "$start (...) $end"
}

@Preview(showBackground = true, device = "id:pixel_8")
@Composable
private fun BibleBookmarksScreenPreview() {
    BibleBookmarksScreen(
        onItemClick = {},
        repository = MockBibleBookmarkRepository()
    )
}
