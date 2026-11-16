package co.epitre.aelf_lectures.bible.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.epitre.aelf_lectures.R
import co.epitre.aelf_lectures.bible.BibleBookListAdapter
import co.epitre.aelf_lectures.bible.data.BibleBookEntryType
import co.epitre.aelf_lectures.bible.data.BibleBookList
import co.epitre.aelf_lectures.bible.data.BiblePart
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

@Composable
fun BibleMenuScreen(
    initialBiblePartId: Int = 0,
    onPageChanged: (Int) -> Unit = {}
) {
    val bibleBookList = remember { BibleBookList.getInstance() }
    val parts = bibleBookList.parts
    val initialPage = initialBiblePartId.coerceIn(0, (parts.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = initialPage) { parts.size }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    val isDark = isSystemInDarkTheme()

    val tabContainerColor = if (isDark) {
        colorResource(R.color.dark_aelf_primary_dark)
    } else {
        colorResource(R.color.light_aelf_accent)
    }
    val tabContentColor = if (isDark) {
        colorResource(R.color.dark_aelf_accent)
    } else {
        colorResource(R.color.light_aelf_lecture_background)
    }
    val tabIndicatorColor = if (isDark) {
        colorResource(R.color.dark_aelf_accent)
    } else {
        Color.White
    }

    val backgroundColor = if (isDark) {
        colorResource(R.color.dark_aelf_lecture_background)
    } else {
        colorResource(R.color.light_aelf_lecture_background)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (parts.isNotEmpty()) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = tabContainerColor,
                contentColor = tabContentColor,
                divider = {},
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = tabIndicatorColor,
                            height = 3.dp
                        )
                    }
                }
            ) {
                parts.forEachIndexed { index, part ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            AutoSizeText(
                                text = part.name,
                                color = if (pagerState.currentPage == index) {
                                    tabContentColor
                                } else {
                                    tabContentColor.copy(alpha = 0.7f)
                                },
                                targetFontSize = 14.sp,
                                minFontSize = 9.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val part = parts[page]
                BibleBookGrid(
                    part = part,
                    biblePartId = page
                )
            }
        }
    }
}

@Composable
private fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    targetFontSize: TextUnit = 14.sp,
    minFontSize: TextUnit = 9.sp,
    fontWeight: FontWeight? = FontWeight.Normal,
    maxLines: Int = 1
) {
    var fontSize by remember(text) { mutableStateOf(targetFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        softWrap = false,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth && fontSize > minFontSize) {
                fontSize = (fontSize.value - 0.5f).sp
            } else {
                readyToDraw = true
            }
        }
    )
}

@Composable
fun BibleBookGrid(
    part: BiblePart,
    biblePartId: Int,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val spanCount = (if (isTablet) 2 else 1) * (if (isLandscape) 2 else 1)

    val entries = part.bibleBookEntries
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isDark = isSystemInDarkTheme()

    val accentColor = if (isDark) {
        colorResource(R.color.dark_aelf_accent)
    } else {
        colorResource(R.color.light_aelf_accent)
    }

    val textColor = if (isDark) {
        colorResource(R.color.dark_aelf_lecture_text)
    } else {
        colorResource(R.color.light_aelf_lecture_text)
    }

    val dividerColor = if (isDark) {
        Color(0xFF3B3C42)
    } else {
        colorResource(R.color.light_aelf_lecture_background_darker)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(spanCount),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 0.dp,
            end = 0.dp,
            bottom = 40.dp + navBarPadding
        )
    ) {
        items(
            count = entries.size,
            span = { index ->
                val entry = entries[index]
                if (entry.type == BibleBookEntryType.SECTION) {
                    GridItemSpan(spanCount)
                } else {
                    GridItemSpan(1)
                }
            }
        ) { index ->
            val entry = entries[index]
            if (entry.type == BibleBookEntryType.SECTION) {
                BibleSectionItem(
                    title = entry.entryName,
                    accentColor = accentColor
                )
            } else {
                BibleBookItem(
                    title = entry.entryName,
                    textColor = textColor,
                    dividerColor = dividerColor,
                    onClick = {
                        EventBus.getDefault().post(
                            BibleBookListAdapter.OnBibleEntryClickEvent(biblePartId, index)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun BibleSectionItem(
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .defaultMinSize(minHeight = 48.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = title,
            color = accentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun BibleBookItem(
    title: String,
    textColor: Color,
    dividerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .defaultMinSize(minHeight = 72.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                color = textColor,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
        HorizontalDivider(
            color = dividerColor,
            thickness = 1.dp
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_8")
@Composable
private fun BibleMenuScreenPreview() {
    BibleMenuScreen()
}
