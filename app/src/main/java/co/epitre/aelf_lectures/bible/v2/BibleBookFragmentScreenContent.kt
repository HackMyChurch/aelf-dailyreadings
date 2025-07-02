package co.epitre.aelf_lectures.bible.v2

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.epitre.aelf_lectures.bible.data.BibleBookChapter
import co.epitre.aelf_lectures.bible.data.BibleVerse
import co.epitre.aelf_lectures.bible.v2.components.BibleVerseComponent
import co.epitre.aelf_lectures.bible.v2.components.TextWithZoom
import co.epitre.aelf_lectures.bible.v2.components.previewBibleVerse
import co.epitre.aelf_lectures.bible.v2.composeTheme.Typo
import co.epitre.aelf_lectures.bible.v2.composeTheme.colors
import co.epitre.aelf_lectures.bible.v2.composeTheme.spacing
import co.epitre.aelf_lectures.bible.v2.reimplemented.customDetectTransformGestures
import co.epitre.aelf_lectures.bible.v2.util.Space
import co.epitre.aelf_lectures.bible.v2.util.round
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleBookFragmentScreenContent(
    nbChapters: Int,
    selectedChapterIndex: Int,
    setSelectedChapterIndex: (Int) -> Unit,
    verses: suspend (chapterIndex: Int) -> List<BibleVerse>,
    chapter: (chapterIndex: Int) -> BibleBookChapter,
    modifier: Modifier = Modifier,
    zoom: Float = 1f,
    onPinchToZoom: (Float) -> Unit = {}
) {

    BoxWithConstraints {

        val tabWidth = this.maxWidth / 3

        Column(modifier = modifier) {
            ScrollableTabRow(
                selectedTabIndex = selectedChapterIndex,
                edgePadding = 0.dp,
                containerColor = colors.surfaceFilled,
                contentColor = colors.onSurfaceFilled,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedChapterIndex]),
                        color = colors.onSurfaceFilled
                    )
                }
            ) {
                for (i in 0..nbChapters) {
                    Tab(
                        modifier = Modifier.width(tabWidth),
                        selected = selectedChapterIndex == i,
                        onClick = {
                            setSelectedChapterIndex(i)
                        }, text = {
                            Text(chapter(i).chapterName)
                        })
                }
            }

            val pagerState = rememberPagerState(
                initialPage = 0,
                initialPageOffsetFraction = 0f,
                pageCount = { nbChapters })

            LaunchedEffect(pagerState.targetPage) {
                setSelectedChapterIndex(pagerState.targetPage)
            }

            LaunchedEffect(selectedChapterIndex) {
                pagerState.animateScrollToPage(
                    selectedChapterIndex,
                    animationSpec = tween(250)
                )
            }

            HorizontalPager(
                state = pagerState, modifier = Modifier.weight(1f)
            ) { index ->


                var displayedVerses by remember { mutableStateOf(listOf<BibleVerse>()) }

                LaunchedEffect(Unit) {
                    displayedVerses = verses(index)
                }

                val coroutineScope = rememberCoroutineScope()
                val scrollState = rememberLazyListState()
                val flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior()

                LazyColumn(
                    flingBehavior = flingBehavior,
                    userScrollEnabled = false
                ) { }



                LazyColumn(
                    state = scrollState,
                    contentPadding = PaddingValues(
                        start = spacing.s100,
                        end = spacing.s100,
                        top = spacing.s50,
                        bottom = spacing.s100
                    ),
                    userScrollEnabled = false, // scroll handled in pointerInput to work with zoom
                    modifier = Modifier
                        .pointerInput(Unit) {
                            customDetectTransformGestures(
                                onPanEnd = { pan, panDirection ->
                                    if (panDirection == 0) {
                                        coroutineScope.launch {
                                            println(pan)
                                            pagerState.animateScrollToPage(
                                                if (pagerState.currentPageOffsetFraction < -0.25
                                                    || pan > 2500) {
                                                    pagerState.currentPage - 1
                                                } else if (pagerState.currentPageOffsetFraction > 0.25
                                                    || pan < -2500
                                                ) {
                                                    pagerState.currentPage + 1
                                                } else {
                                                    pagerState.currentPage
                                                }
                                            )
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            scrollState.scroll {
                                                with(flingBehavior) {
                                                    performFling(-pan)
                                                }
                                            }
                                        }
                                    }
                                }
                            ) { _, pPan, pZoom, _, panDirection ->

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
                                            scrollState.scrollBy(-pPan.y)
                                        }


                                    }
                                }
                            }
                        }
                ) {
                    item {
                        Space(spacing.s50)
                        TextWithZoom(
                            chapter(index).chapterName, style = Typo.title,
                            color = colors.textNeutral,
                            zoom = zoom,
                        )
                        Space(spacing.s100)
                    }

                    items(displayedVerses) {
                        BibleVerseComponent(
                            ref = it.ref,
                            text = it.text,
                            zoom = zoom
                        )
                    }

                    item {
                        Box(Modifier.navigationBarsPadding())
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
        previewBibleVerse,
        previewBibleVerse,
        previewBibleVerse
    )

    BibleBookFragmentScreenContent(
        nbChapters = nbChapters,
        selectedChapterIndex = selectedChapterIndex,
        setSelectedChapterIndex = {
            selectedChapterIndex = it
        },
        verses = { verses },
        chapter = {
            BibleBookChapter("Gn", it.toString(), "Chapitre ${it + 1}")
        }
    )
}