package co.epitre.aelf_lectures.bible.v2

import android.text.InputFilter.AllCaps
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
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
import co.epitre.aelf_lectures.bible.v2.util.pxToDp
import co.epitre.aelf_lectures.bible.v2.util.round
import kotlinx.coroutines.delay
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
    onPinchToZoom: (Float) -> Unit = {}
) {


    val scrollStates = remember {
        List(chapters.size) { ScrollState(0) }
    }

    var focusedVerse by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var showChaptersDropdown by remember { mutableStateOf(false) }
    var displayedVerses by remember { mutableStateOf<Map<Int, List<BibleVerse>>>(emptyMap()) }
    var selectedTabXOffset by remember { mutableStateOf(0f) }

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

                            },
                        selected = selectedChapterIndex == i,
                        onClick = {
                            if (selectedChapterIndex != i) {
                                setSelectedChapterIndex(i)
                            } else {
                                showChaptersDropdown = true
                            }

                        }, text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (i == selectedChapterIndex && chapters.size > 1) {
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        modifier = Modifier.scale(1.2f),
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                Text(chapter(i).chapterName)
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
                        selectedChapterIndex,
                        animationSpec = tween(250)
                    )
                }

                val coroutineScope = rememberCoroutineScope()


                val flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior()


                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.Top
                ) { index ->


                    if (displayedVerses[index] == null) {
                        LaunchedEffect(Unit) {
                            displayedVerses += index to verses(index)
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
                        Column(
                            modifier = Modifier
                                .padding(
                                    horizontal = spacing.s100
                                )
                                .pointerInput(Unit) {
                                    customDetectTransformGestures(
                                        onPanEnd = { velocity, panDirection ->
                                            if (panDirection == 0) {
                                                coroutineScope.launch {
                                                    scrollStates[index].stopScroll()
                                                    if (tempDisablePan) {
                                                        pagerState.animateScrollToPage(pagerState.targetPage)
                                                        tempDisablePan = false
                                                    } else {
                                                        pagerState.animateScrollToPage(
                                                            if (pagerState.currentPageOffsetFraction < -0.25
                                                                || velocity > 1500
                                                            ) {
                                                                pagerState.currentPage - 1
                                                            } else if (pagerState.currentPageOffsetFraction > 0.25
                                                                || velocity < -1500
                                                            ) {
                                                                pagerState.currentPage + 1
                                                            } else {
                                                                pagerState.currentPage
                                                            }
                                                        )
                                                    }
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    scrollStates[index].scroll {
                                                        with(flingBehavior) {
                                                            performFling(-velocity)
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
                                                    scrollStates[index].scrollBy(-pPan.y)
                                                }
                                            }
                                        }
                                    }
                                }
                                .verticalScroll(state = scrollStates[index], enabled = false)
                        ) {
                            Space(spacing.s150)
                            TextWithZoom(
                                chapter(index).chapterName, style = Typo.title,
                                color = colors.textNeutral,
                                zoom = zoom,
                            )
                            Space(spacing.s100)

                            displayedVerses.get(index)?.forEachIndexed { i, it ->
                                BibleVerseComponent(
                                    ref = it.ref,
                                    text = it.text,
                                    zoom = zoom,
                                    isFocued = focusedVerse == selectedChapterIndex to i,
                                    onClick = {
                                        val toFocus = selectedChapterIndex to i
                                        if (focusedVerse != toFocus) {
                                            focusedVerse = toFocus
                                        } else {
                                            focusedVerse = null
                                        }
                                    }
                                )
                            }

                            Box(
                                Modifier
                                    .navigationBarsPadding()
                                    .padding(spacing.s100)
                            )
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
                        .navigationBarsPadding()
                        .animateContentSize(),
                    color = Color.White,
                ) {

                    if (showChaptersDropdown) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            chapters.forEachIndexed { i, it ->
                                DropdownMenuItem(
                                    text = { Text(it.chapterName) },
                                    onClick = {
                                        coroutineScope.launch {
                                            delay(200)
                                            setSelectedChapterIndex(i)
                                            showChaptersDropdown = false
                                        }
                                    }
                                )
                            }
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
        previewBibleVerse,
        previewBibleVerse,
        previewBibleVerse
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
        }
    )
}