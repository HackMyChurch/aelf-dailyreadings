package co.epitre.aelf_lectures.bible.biblebookfragment.content

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.epitre.aelf_lectures.bible.biblebookfragment.components.BibleVerseComponent
import co.epitre.aelf_lectures.bible.biblebookfragment.components.TextWithZoom
import co.epitre.aelf_lectures.bible.biblebookfragment.components.previewBibleVerse
import co.epitre.aelf_lectures.bible.data.BibleBookChapter
import co.epitre.aelf_lectures.bible.data.BibleVerse
import co.epitre.aelf_lectures.bible.data.LectureReference
import co.epitre.aelf_lectures.compose.theme.Typo
import co.epitre.aelf_lectures.compose.theme.colors
import co.epitre.aelf_lectures.compose.theme.spacing
import co.epitre.aelf_lectures.compose.utils.Space
import co.epitre.aelf_lectures.compose.utils.customDetectTransformGestures
import co.epitre.aelf_lectures.compose.utils.pxToDp
import co.epitre.aelf_lectures.utils.Utils.containsVerse
import co.epitre.aelf_lectures.utils.Utils.safeToInt
import co.epitre.aelf_lectures.utils.round
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun BibleBookFragmentScreenContent(
    chapters: List<BibleBookChapter>,
    selectedChapterIndex: Int,
    setSelectedChapterIndex: (Int) -> Unit,
    verses: suspend (chapterIndex: Int) -> List<BibleVerse>,
    chapter: (chapterIndex: Int) -> BibleBookChapter,
    modifier: Modifier = Modifier,
    zoom: Float = 1f,
    searchQuery: String? = null,
    lectureRefs: List<LectureReference>? = null,
    onPinchToZoom: (Float) -> Unit = {}
) {


    val scrollToRef = lectureRefs?.firstOrNull()?.verseStart
    val highlightChapter = lectureRefs?.firstOrNull()?.chapter

    val scrollStates = remember {
        List(chapters.size) { LazyListState(0) }
    }

    var focusedVerse by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var showChaptersDropdown by remember { mutableStateOf(false) }
    var displayedVerses by remember { mutableStateOf<Map<Int, List<BibleVerse>>>(emptyMap()) }
    var selectedTabXOffset by remember { mutableFloatStateOf(0f) }


    if (scrollToRef != null) {
        LaunchedEffect(Unit) {
            val listState = scrollStates[selectedChapterIndex]
            snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                .filter { it.isNotEmpty() }
                .first()

            delay(200)

            listState.animateScrollToItem(
                scrollToRef.safeToInt() ?: 0,
                scrollOffset = 0
            )
        }
    }


    BoxWithConstraints {

        val tabWidth = this.maxWidth / 3

        Column(modifier = modifier) {

            ScrollableTabRow(
                selectedTabIndex = selectedChapterIndex,
                edgePadding = 0.dp,
                containerColor = colors.surfaceFilled,
                contentColor = colors.onSurfaceFilled,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedChapterIndex]),
                        color = colors.onSurfaceFilled
                    )
                },
            ) {
                for (i in 0..<chapters.size) {
                    Tab(
                        modifier = Modifier
                            .width(tabWidth)
                            .onGloballyPositioned {
                                if (selectedChapterIndex == i) {
                                    selectedTabXOffset = it.positionInRoot().x
                                }

                            }, selected = selectedChapterIndex == i, onClick = {
                            if (selectedChapterIndex != i) {
                                setSelectedChapterIndex(i)
                            } else {
                                showChaptersDropdown = !showChaptersDropdown
                            }

                        }, text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (i == selectedChapterIndex && chapters.size > 1) {
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        modifier = Modifier.scale(1.2f),
                                        contentDescription = null,
                                        tint = colors.tabText
                                    )
                                }
                                Text(
                                    chapter(i).chapterName,
                                    color = colors.tabText,
                                    style = Typo.tab,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                        })
                }
            }
            Box {

                val pagerState = rememberPagerState(
                    initialPage = selectedChapterIndex,
                    initialPageOffsetFraction = 0f,
                    pageCount = { chapters.size })


                var tempDisablePan by remember { mutableStateOf(false) }

                LaunchedEffect(pagerState.targetPage) {
                    tempDisablePan = true
                    setSelectedChapterIndex(pagerState.targetPage)
                    delay(200)
                    tempDisablePan = false
                }



                LaunchedEffect(selectedChapterIndex) {
                    showChaptersDropdown = false
                    pagerState.animateScrollToPage(
                        selectedChapterIndex, animationSpec = tween(250)
                    )
                }

                val coroutineScope = rememberCoroutineScope()


                val flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior()


                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.Top
                ) { pagerIndex ->

                    if (displayedVerses[pagerIndex] == null) {
                        LaunchedEffect(Unit) {
                            displayedVerses += pagerIndex to verses(pagerIndex)
                        }
                    }

                    var isTextSelectable by remember { mutableStateOf(true) }

                    val wrapper: @Composable (@Composable () -> Unit) -> Unit =
                        if (isTextSelectable) {
                            { SelectionContainer { it() } }
                        } else {
                            { DisableSelection { it() } }
                        }


                    wrapper {
                        LazyColumn(
                            userScrollEnabled = false,
                            state = scrollStates[pagerIndex],
                            contentPadding = PaddingValues(
                                start = spacing.s25,
                                end = spacing.s50
                            ),
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    customDetectTransformGestures(
                                        onPanEnd = { velocity, panDirection ->
                                            if (panDirection == 0) {
                                                coroutineScope.launch {
                                                    scrollStates[pagerIndex].stopScroll()
                                                    if (tempDisablePan) {
                                                        pagerState.animateScrollToPage(pagerState.targetPage)
                                                        tempDisablePan = false
                                                    } else {
                                                        pagerState.animateScrollToPage(
                                                            if (pagerState.currentPageOffsetFraction < -0.25 || velocity > 1500) {
                                                                pagerState.currentPage - 1
                                                            } else if (pagerState.currentPageOffsetFraction > 0.25 || velocity < -1500) {
                                                                pagerState.currentPage + 1
                                                            } else {
                                                                pagerState.currentPage
                                                            }
                                                        )
                                                    }
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    scrollStates[pagerIndex].scroll {
                                                        with(flingBehavior) {
                                                            performFling(-velocity)
                                                        }
                                                    }
                                                }
                                            }
                                        }) { _, pPan, pZoom, _, panDirection ->

                                        if (pZoom != 1f) {
                                            val rounded = pZoom.round(2)
                                            if (rounded != zoom) {
                                                onPinchToZoom(pZoom)
                                            }

                                        } else {
                                            coroutineScope.launch {
                                                if (panDirection == 0) {
                                                    pagerState.scrollBy(-pPan.x)
                                                } else {
                                                    scrollStates[pagerIndex].scrollBy(-pPan.y)
                                                }
                                            }
                                        }
                                    }
                                }) {

                            item {
                                Column {
                                    Space(spacing.s150)
                                    TextWithZoom(
                                        chapter(pagerIndex).chapterName, style = Typo.title,
                                        modifier = Modifier.padding(horizontal = spacing.s100),
                                        color = colors.textNeutral,
                                        zoom = zoom,
                                    )
                                    Space(spacing.s100)
                                }

                            }
                            displayedVerses[pagerIndex]?.forEachIndexed { i, it ->

                                item {
                                    val chapterRef = chapters[pagerIndex].chapterRef
                                    BibleVerseComponent(
                                        ref = it.ref,
                                        text = it.text,
                                        zoom = zoom,
                                        searchQuery = searchQuery,
                                        isFocused = focusedVerse == selectedChapterIndex to i,
                                        isHighlighted = highlightChapter == chapterRef
                                                && lectureRefs.containsVerse(
                                            chapters[pagerIndex].chapterRef,
                                            it.ref,
                                        ),
                                        onClick = {
                                            val toFocus = selectedChapterIndex to i
                                            if (focusedVerse != toFocus) {
                                                focusedVerse = toFocus
                                            } else {
                                                focusedVerse = null
                                            }
                                        })
                                }
                            }

                            item {
                                Box(
                                    Modifier
                                        .navigationBarsPadding()
                                        .padding(spacing.s100)
                                )
                            }

                        }
                    }
                }

                if (showChaptersDropdown) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showChaptersDropdown = false })
                }

                Surface(
                    modifier = Modifier
                        .offset(x = selectedTabXOffset.pxToDp())
                        .width(tabWidth)
                        .animateContentSize(),
                    color = colors.elevatedSurface,
                ) {

                    if (showChaptersDropdown) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            chapters.forEachIndexed { i, it ->
                                DropdownMenuItem(
                                    colors = MenuDefaults.itemColors(
                                        textColor = colors.onElevatedSurface
                                    ),
                                    text = { Text(it.chapterName) },

                                    onClick = {
                                    coroutineScope.launch {
                                        delay(200)
                                        setSelectedChapterIndex(i)
                                        showChaptersDropdown = false
                                    }
                                })
                            }

                            Space(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .padding(bottom = spacing.s25)
                            )
                        }
                    }


                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0EAE2)
@Composable
fun PreviewBibleBookFragmentScreenContent() {

    val nbChapters = 10
    var selectedChapterIndex by remember { mutableIntStateOf(0) }

    val verses = listOf(
        previewBibleVerse, previewBibleVerse, previewBibleVerse
    )

    BibleBookFragmentScreenContent(
        chapters = List(5) { BibleBookChapter("", "", "Chapter") },
        selectedChapterIndex = selectedChapterIndex,
        setSelectedChapterIndex = {
            selectedChapterIndex = it
        },
        verses = { verses },
        chapter = {
            BibleBookChapter("Gn", it.toString(), "Chapitre ${it + 1}")
        })
}