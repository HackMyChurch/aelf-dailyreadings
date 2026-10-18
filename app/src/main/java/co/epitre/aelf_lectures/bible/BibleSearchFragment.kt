package co.epitre.aelf_lectures.bible

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import co.epitre.aelf_lectures.LecturesActivity
import co.epitre.aelf_lectures.R
import co.epitre.aelf_lectures.bible.data.BibleController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

data class BibleSearchResult(
    val title: String,
    val reference: String,
    val snippetHtml: String,
    val link: String
)

class BibleSearchFragment : BibleFragment(), SearchView.OnQueryTextListener {

    private var actionBar: ActionBar? = null
    private var activity: LecturesActivity? = null
    private var mSearchView: SearchView? = null

    private var queryState = mutableStateOf("")
    private var sortState = mutableStateOf(BibleController.Sort.Relevance)

    private var initialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        activity = requireActivity() as LecturesActivity
        actionBar = activity?.supportActionBar

        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)

        actionBar?.title = title

        if (!initialized) {
            val args = savedInstanceState ?: arguments
            if (args != null) {
                queryState.value = args.getString(BIBLE_SEARCH_QUERY, "") ?: ""
                val sortName = args.getString(BIBLE_SEARCH_SORT, "") ?: ""
                try {
                    if (sortName.isNotEmpty()) {
                        sortState.value = BibleController.Sort.valueOf(sortName)
                    }
                } catch (ignored: IllegalArgumentException) {
                }
            }
            initialized = true
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val query by queryState
                val sort by sortState
                var searchResults by remember { mutableStateOf<List<BibleSearchResult>>(emptyList()) }

                LaunchedEffect(query, sort) {
                    val results = withContext(Dispatchers.IO) {
                        performSearch(query, sort)
                    }
                    searchResults = results
                }

                BibleSearchScreen(
                    sort = sort,
                    searchResults = searchResults,
                    onSortChanged = { newSort ->
                        sortState.value = newSort
                    },
                    onItemClick = { link ->
                        onItemClick(link)
                    }
                )
            }
        }
    }

    private fun performSearch(rawQuery: String, sort: BibleController.Sort): List<BibleSearchResult> {
        if (rawQuery.isEmpty()) {
            return emptyList()
        }
        var query = rawQuery
        if (query.length >= 3 && !query.endsWith("*")) {
            query += "*"
        }

        val cursor = BibleController.getInstance().search(query, sort) ?: return emptyList()
        val results = mutableListOf<BibleSearchResult>()
        cursor.use { c ->
            if (c.moveToFirst()) {
                val colBook = c.getColumnIndex("book")
                val colTitle = c.getColumnIndex("title")
                val colChapter = c.getColumnIndex("chapter")
                val colSnippet = c.getColumnIndex("snippet")
                val colSkipped = c.getColumnIndex("skipped")

                do {
                    val book = if (colBook >= 0) c.getString(colBook) ?: "" else ""
                    val title = if (colTitle >= 0) c.getString(colTitle) ?: "" else ""
                    val chapter = if (colChapter >= 0) c.getString(colChapter) ?: "" else ""
                    var preview = if (colSnippet >= 0) c.getString(colSnippet) ?: "" else ""
                    val skipped = if (colSkipped >= 0) c.getString(colSkipped) ?: "" else ""

                    var reference = "$book $chapter"
                    val link = "https://www.aelf.org/bible/$book/$chapter?query=${URLEncoder.encode(rawQuery, "UTF-8")}"

                    if (book == "Ps") {
                        reference = "Ps $chapter"
                    }

                    if (skipped.isNotEmpty()) {
                        preview += "<br/><br/><i>Terme manquant: <s>$skipped</s></i>"
                    }

                    results.add(BibleSearchResult(title, reference, preview, link))
                } while (c.moveToNext())
            }
        }
        return results
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        val searchMenuItem = menu.findItem(R.id.action_search) ?: return
        searchMenuItem.expandActionView()

        val searchView = searchMenuItem.actionView as? SearchView ?: return
        mSearchView = searchView
        searchView.setIconifiedByDefault(false)
        searchView.isSubmitButtonEnabled = false
        searchView.setQuery(queryState.value, false)

        searchView.setOnQueryTextListener(this)

        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return false
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                try {
                    parentFragmentManager.popBackStack()
                } catch (ignored: IllegalStateException) {
                }
                return false
            }
        })
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        queryState.value = query ?: ""
        val view = view ?: return true
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        queryState.value = newText ?: ""
        return true
    }

    override fun onStart() {
        super.onStart()
        mSearchView?.setOnQueryTextListener(this)
    }

    override fun onStop() {
        super.onStop()
        mSearchView?.setOnQueryTextListener(null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BIBLE_SEARCH_QUERY, queryState.value)
        outState.putString(BIBLE_SEARCH_SORT, sortState.value.name)
    }

    override fun getRoute(): String {
        return try {
            "/search?query=${URLEncoder.encode(queryState.value, "UTF-8")}&sort=${URLEncoder.encode(sortState.value.name, "UTF-8")}"
        } catch (e: Exception) {
            "/search"
        }
    }

    override fun getTitle(): String = "Recherche"

    private fun onItemClick(link: String) {
        val mainActivity = activity ?: return
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        mainActivity.onIntent(intent)
    }

    companion object {
        private const val BIBLE_SEARCH_QUERY = "bibleSearchQuery"
        private const val BIBLE_SEARCH_SORT = "bibleSearchSort"

        @JvmStatic
        fun newInstance(uri: Uri): BibleSearchFragment {
            val fragment = BibleSearchFragment()
            val args = Bundle().apply {
                putString(BIBLE_SEARCH_QUERY, uri.getQueryParameter("query"))
                putString(BIBLE_SEARCH_SORT, uri.getQueryParameter("sort"))
            }
            fragment.arguments = args
            return fragment
        }
    }
}

@Composable
fun BibleSearchScreen(
    sort: BibleController.Sort,
    searchResults: List<BibleSearchResult>,
    onSortChanged: (BibleController.Sort) -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = getLectureBackgroundColor()
    val dividerColor = getDividerColor()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 0.dp)
            .clickable(enabled = false) {}
    ) {
        Surface(color = bgColor, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                SortBar(
                    sort = sort,
                    onSortChanged = onSortChanged
                )
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
                SearchResultsList(
                    results = searchResults,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

@Composable
private fun SortBar(
    sort: BibleController.Sort,
    onSortChanged: (BibleController.Sort) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = getAccentColor()
    val lectureBgColor = getLectureBackgroundColor()

    val indicatorOffset by animateDpAsState(
        targetValue = if (sort == BibleController.Sort.Bible) 0.dp else 160.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "sortIndicatorOffset"
    )

    val bibleTextColor by animateColorAsState(
        targetValue = if (sort == BibleController.Sort.Bible) lectureBgColor else accentColor,
        animationSpec = tween(durationMillis = 250),
        label = "bibleTextColor"
    )

    val relevanceTextColor by animateColorAsState(
        targetValue = if (sort == BibleController.Sort.Relevance) lectureBgColor else accentColor,
        animationSpec = tween(durationMillis = 250),
        label = "relevanceTextColor"
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
                // Sliding background indicator
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(160.dp)
                        .height(32.dp)
                        .background(accentColor)
                )

                // Middle divider
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(1.dp)
                        .height(32.dp)
                        .background(accentColor)
                )

                // Clickable options layer
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(160.dp)
                            .height(32.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSortChanged(BibleController.Sort.Bible) }
                            )
                    ) {
                        Text(
                            text = "Ordre Biblique",
                            color = bibleTextColor,
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
                                onClick = { onSortChanged(BibleController.Sort.Relevance) }
                            )
                    ) {
                        Text(
                            text = "Pertinence",
                            color = relevanceTextColor,
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
private fun SearchResultsList(
    results: List<BibleSearchResult>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(results) {
        if (results.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

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
            items = results,
            key = { it.link + it.reference }
        ) { result ->
            SearchResultItem(
                result = result,
                onClick = { onItemClick(result.link) }
            )
        }
    }
}

@Composable
private fun SearchResultItem(
    result: BibleSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = getAccentColor()
    val textColor = getLectureTextColor()
    val dividerColor = getDividerColor()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = result.title,
                color = accentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = result.reference,
                color = textColor,
                fontSize = 12.sp,
                textAlign = TextAlign.End
            )
        }

        if (result.snippetHtml.isNotEmpty()) {
            Spacer(modifier = Modifier.height(7.dp))
            HtmlText(
                html = result.snippetHtml,
                color = textColor,
                fontSize = 14.sp
            )
        }
    }
    HorizontalDivider(color = dividerColor, thickness = 1.dp)
}

@Composable
private fun HtmlText(
    html: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val annotatedString = remember(html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            AnnotatedString.fromHtml(html)
        } else {
            AnnotatedString(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toString())
        }
    }
    Text(
        text = annotatedString,
        color = color,
        fontSize = fontSize,
        modifier = modifier
    )
}

@Composable
private fun getLectureBackgroundColor(): Color {
    return if (isSystemInDarkTheme()) {
        colorResource(R.color.dark_aelf_lecture_background)
    } else {
        colorResource(R.color.light_aelf_lecture_background)
    }
}

@Composable
private fun getLectureTextColor(): Color {
    return if (isSystemInDarkTheme()) {
        colorResource(R.color.dark_aelf_lecture_text)
    } else {
        colorResource(R.color.light_aelf_lecture_text)
    }
}

@Composable
private fun getAccentColor(): Color {
    return if (isSystemInDarkTheme()) {
        colorResource(R.color.dark_aelf_accent)
    } else {
        colorResource(R.color.light_aelf_accent)
    }
}

@Composable
private fun getDividerColor(): Color {
    return if (isSystemInDarkTheme()) {
        Color(0xFF3B3C42)
    } else {
        colorResource(R.color.light_aelf_lecture_background_darker)
    }
}

@Preview(showBackground = true)
@Composable
private fun BibleSearchScreenPreview() {
    val sampleResults = listOf(
        BibleSearchResult(
            title = "Première aux Corinthiens, Chapitre 13",
            reference = "1Co 13",
            snippetHtml = "L'amour prend patience, l'amour rend service...",
            link = "https://www.aelf.org/bible/1Co/13"
        )
    )
    BibleSearchScreen(
        sort = BibleController.Sort.Relevance,
        searchResults = sampleResults,
        onSortChanged = {},
        onItemClick = {}
    )
}
