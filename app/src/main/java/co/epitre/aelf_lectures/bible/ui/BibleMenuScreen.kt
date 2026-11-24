package co.epitre.aelf_lectures.bible.ui

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import co.epitre.aelf_lectures.LecturesActivity
import co.epitre.aelf_lectures.R
import co.epitre.aelf_lectures.bible.data.bible.BibleBookEntryType
import co.epitre.aelf_lectures.bible.data.bible.BibleBookList
import co.epitre.aelf_lectures.bible.data.bible.BiblePart
import kotlinx.coroutines.launch

@Composable
fun BibleMenuScreen(
    initialBiblePartId: Int = 0,
    onPageChanged: (Int) -> Unit = {},
    onItemClick: (String) -> Unit = {}
) {
    val bibleBookList = remember { BibleBookList.getInstance() }
    val parts = bibleBookList.parts
    val totalPages = parts.size + 1 // 3 parts + 1 bookmarks tab
    val initialPage = initialBiblePartId.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = initialPage) { totalPages }
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
            BibleTabRow(
                selectedTabIndex = pagerState.currentPage,
                parts = parts,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                containerColor = tabContainerColor,
                contentColor = tabContentColor,
                indicatorColor = tabIndicatorColor
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                if (page < parts.size) {
                    val part = parts[page]
                    BibleBookGrid(
                        part = part,
                        biblePartId = page
                    )
                } else {
                    BibleBookmarksScreen(
                        onItemClick = onItemClick
                    )
                }
            }
        }
    }
}

private data class TabMetrics(
    val fontSize: TextUnit,
    val weights: List<Float>
)

@Composable
private fun BibleTabRow(
    selectedTabIndex: Int,
    parts: List<BiblePart>,
    onTabSelected: (Int) -> Unit,
    containerColor: Color,
    contentColor: Color,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    require(parts.size == 3) { "BibleTabRow expects exactly 3 Bible parts" }

    val part0 = parts[0]
    val part1 = parts[1]
    val part2 = parts[2]

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
    ) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val totalWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }

        val iconTabWidthDp = 48.dp
        val iconTabWidthPx = with(density) { iconTabWidthDp.toPx() }

        val metrics = remember(
            part0.name, part1.name, part2.name, totalWidthPx, iconTabWidthPx, density
        ) {
            val availableForTextPx = (totalWidthPx - iconTabWidthPx).coerceAtLeast(100f)
            if (availableForTextPx <= 0f) {
                TabMetrics(11.sp, listOf(1f / 3f, 1f / 3f, 1f / 3f))
            } else {
                val targetFontSizeSp = 12f
                val minFontSizeSp = 7f
                val minPaddingPx = density.run { 4.dp.toPx() }

                var chosenFontSizeSp = targetFontSizeSp
                var weights = listOf(1f / 3f, 1f / 3f, 1f / 3f)

                var currentSp = targetFontSizeSp
                while (currentSp >= minFontSizeSp) {
                    val style = TextStyle(
                        fontSize = currentSp.sp,
                        fontWeight = FontWeight.Normal
                    )

                    val w0 = textMeasurer.measure(part0.name, style = style).size.width.toFloat()
                    val w1 = textMeasurer.measure(part1.name, style = style).size.width.toFloat()
                    val w2 = textMeasurer.measure(part2.name, style = style).size.width.toFloat()

                    val maxOuterPx = maxOf(w0, w2)
                    val minRequiredPx = 2 * maxOuterPx + w1 + 3 * minPaddingPx

                    if (minRequiredPx <= availableForTextPx || currentSp <= minFontSizeSp) {
                        chosenFontSizeSp = currentSp
                        val extraSpacePx = (availableForTextPx - w1 - 2 * maxOuterPx) / 3f
                        val outerPx = maxOuterPx + extraSpacePx
                        val middlePx = availableForTextPx - 2 * outerPx

                        weights = listOf(
                            outerPx / totalWidthPx,
                            middlePx / totalWidthPx,
                            outerPx / totalWidthPx
                        )
                        break
                    }

                    currentSp -= 0.5f
                }

                TabMetrics(chosenFontSizeSp.sp, weights)
            }
        }

        val calculatedFontSize = metrics.fontSize
        val textWeights = metrics.weights

        val w0Dp = this@BoxWithConstraints.maxWidth * textWeights[0]
        val w1Dp = this@BoxWithConstraints.maxWidth * textWeights[1]
        val w2Dp = this@BoxWithConstraints.maxWidth * textWeights[2]
        val iconDp = iconTabWidthDp

        val tabLeftsDp = listOf(0.dp, w0Dp, w0Dp + w1Dp, w0Dp + w1Dp + w2Dp)
        val tabWidthsDp = listOf(w0Dp, w1Dp, w2Dp, iconDp)

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.width(w0Dp),
                    selectedContentColor = contentColor,
                    unselectedContentColor = contentColor.copy(alpha = 0.7f),
                    text = {
                        Text(
                            text = part0.name,
                            fontSize = calculatedFontSize,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.width(w1Dp),
                    selectedContentColor = contentColor,
                    unselectedContentColor = contentColor.copy(alpha = 0.7f),
                    text = {
                        Text(
                            text = part1.name,
                            fontSize = calculatedFontSize,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { onTabSelected(2) },
                    modifier = Modifier.width(w2Dp),
                    selectedContentColor = contentColor,
                    unselectedContentColor = contentColor.copy(alpha = 0.7f),
                    text = {
                        Text(
                            text = part2.name,
                            fontSize = calculatedFontSize,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { onTabSelected(3) },
                    modifier = Modifier.width(iconDp),
                    selectedContentColor = contentColor,
                    unselectedContentColor = contentColor.copy(alpha = 0.7f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Signets",
                            tint = if (selectedTabIndex == 3) contentColor else contentColor.copy(alpha = 0.7f)
                        )
                    }
                )
            }

            val validIndex = selectedTabIndex.coerceIn(0, 3)
            val indicatorLeft by animateDpAsState(
                targetValue = tabLeftsDp[validIndex],
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "tabIndicatorLeft"
            )
            val indicatorWidth by animateDpAsState(
                targetValue = tabWidthsDp[validIndex],
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "tabIndicatorWidth"
            )

            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = indicatorLeft)
                    .width(indicatorWidth),
                color = indicatorColor,
                height = 2.dp
            )
        }
    }
}

@Composable
fun BibleBookGrid(
    part: BiblePart,
    biblePartId: Int,
    modifier: Modifier = Modifier
) {
    val lecturesActivity = LocalActivity.current as? LecturesActivity

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
                        val uri = "https://www.aelf.org/bible/${entry.bookRef}/${entry.chapterRef}".toUri()
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        lecturesActivity?.onIntent(intent)
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
