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
import androidx.fragment.app.FragmentManager
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.SearchView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseOutBounce
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import co.epitre.aelf_lectures.LecturesActivity
import co.epitre.aelf_lectures.R
import co.epitre.aelf_lectures.bible.data.BibleBookEntryType
import co.epitre.aelf_lectures.bible.data.BibleBookList
import co.epitre.aelf_lectures.bible.data.BibleController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import kotlin.time.Duration.Companion.milliseconds

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
    private var selectedPartsState = mutableStateOf<Set<String>>(emptySet())
    private var selectedSectionsState = mutableStateOf<Set<String>>(emptySet())
    private var selectedBooksState = mutableStateOf<Set<String>>(emptySet())
    private var bookFilterState = mutableStateOf<List<String>?>(null)
    private var isFilterSheetVisibleState = mutableStateOf(false)
    private var hasUriFilterState = mutableStateOf(false)

    private val backStackChangeListener = FragmentManager.OnBackStackChangedListener {
        if (isAdded) {
            try {
                val count = parentFragmentManager.backStackEntryCount
                var filterSheetInBackStack = false
                for (i in 0 until count) {
                    if (parentFragmentManager.getBackStackEntryAt(i).name == BACK_STACK_FILTER_SHEET) {
                        filterSheetInBackStack = true
                        break
                    }
                }
                if (!filterSheetInBackStack && isFilterSheetVisibleState.value) {
                    isFilterSheetVisibleState.value = false
                }
            } catch (ignored: IllegalStateException) {
            }
        }
    }

    private fun openFilterSheet() {
        if (!isFilterSheetVisibleState.value) {
            isFilterSheetVisibleState.value = true
            val view = view
            if (view != null) {
                val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
            }
            if (isAdded) {
                try {
                    parentFragmentManager.beginTransaction()
                        .addToBackStack(BACK_STACK_FILTER_SHEET)
                        .commit()
                } catch (ignored: IllegalStateException) {
                }
            }
        }
    }

    private fun dismissFilterSheet() {
        if (isFilterSheetVisibleState.value) {
            isFilterSheetVisibleState.value = false
            if (isAdded) {
                try {
                    val count = parentFragmentManager.backStackEntryCount
                    if (count > 0 && parentFragmentManager.getBackStackEntryAt(count - 1).name == BACK_STACK_FILTER_SHEET) {
                        parentFragmentManager.popBackStack(BACK_STACK_FILTER_SHEET, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    }
                } catch (ignored: IllegalStateException) {
                }
            }
        }
    }

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
                val partsList = args.getStringArrayList(BIBLE_SEARCH_PARTS)
                val sectionsList = args.getStringArrayList(BIBLE_SEARCH_SECTIONS)
                val booksList = args.getStringArrayList(BIBLE_SEARCH_BOOKS)

                if (partsList != null) selectedPartsState.value = partsList.toSet()
                if (sectionsList != null) selectedSectionsState.value = sectionsList.toSet()
                if (booksList != null) selectedBooksState.value = booksList.toSet()

                if (savedInstanceState == null) {
                    hasUriFilterState.value = args.getBoolean(BIBLE_SEARCH_HAS_URI_FILTER, false)
                } else {
                    isFilterSheetVisibleState.value = savedInstanceState.getBoolean(BIBLE_SEARCH_FILTER_SHEET_VISIBLE, false)
                }
            }
            initialized = true
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val query by queryState
                val sort by sortState
                val selectedParts by selectedPartsState
                val selectedSections by selectedSectionsState
                val selectedBooks by selectedBooksState
                val bookFilter by bookFilterState
                val isFilterSheetVisible by isFilterSheetVisibleState
                val hasUriFilter by hasUriFilterState
                var searchResults by remember { mutableStateOf<List<BibleSearchResult>>(emptyList()) }

                LaunchedEffect(query, sort, bookFilter) {
                    if (query.isNotBlank()) {
                        delay(300)
                    }
                    val results = withContext(Dispatchers.IO) {
                        performSearch(query, sort, bookFilter)
                    }
                    searchResults = results
                }

                BibleSearchScreen(
                    query = query,
                    sort = sort,
                    searchResults = searchResults,
                    initialSelectedParts = selectedParts,
                    initialSelectedSections = selectedSections,
                    initialSelectedBooks = selectedBooks,
                    shouldPulseFilter = hasUriFilter,
                    onSortChanged = { newSort ->
                        sortState.value = newSort
                    },
                    onSelectionChanged = { parts, sections, books, flatFilter ->
                        selectedPartsState.value = parts
                        selectedSectionsState.value = sections
                        selectedBooksState.value = books
                        bookFilterState.value = flatFilter
                    },
                    onItemClick = { link ->
                        onItemClick(link)
                    },
                    isFilterSheetVisible = isFilterSheetVisible,
                    onOpenFilter = { openFilterSheet() },
                    onDismissFilter = { dismissFilterSheet() }
                )
            }
        }
    }

    private fun performSearch(
        rawQuery: String,
        sort: BibleController.Sort,
        bookFilter: List<String>?
    ): List<BibleSearchResult> {
        if (rawQuery.length < 3) {
            return emptyList()
        }
        val cleanQuery = rawQuery.replace("[^\\p{L}\\p{N}\\s]".toRegex(), " ").trim()
        if (cleanQuery.length < 3) {
            return emptyList()
        }
        var query = cleanQuery
        if (!query.endsWith("*")) {
            query += "*"
        }

        val cursor = BibleController.getInstance().search(query, sort, bookFilter) ?: return emptyList()
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
        try {
            parentFragmentManager.addOnBackStackChangedListener(backStackChangeListener)
        } catch (ignored: IllegalStateException) {
        }
    }

    override fun onStop() {
        super.onStop()
        mSearchView?.setOnQueryTextListener(null)
        try {
            parentFragmentManager.removeOnBackStackChangedListener(backStackChangeListener)
        } catch (ignored: IllegalStateException) {
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BIBLE_SEARCH_QUERY, queryState.value)
        outState.putString(BIBLE_SEARCH_SORT, sortState.value.name)
        outState.putStringArrayList(BIBLE_SEARCH_PARTS, ArrayList(selectedPartsState.value))
        outState.putStringArrayList(BIBLE_SEARCH_SECTIONS, ArrayList(selectedSectionsState.value))
        outState.putStringArrayList(BIBLE_SEARCH_BOOKS, ArrayList(selectedBooksState.value))
        outState.putBoolean(BIBLE_SEARCH_FILTER_SHEET_VISIBLE, isFilterSheetVisibleState.value)
    }

    override fun getRoute(): String {
        return try {
            val selectedTokens = mutableListOf<String>()
            selectedTokens.addAll(selectedPartsState.value)
            selectedTokens.addAll(selectedSectionsState.value)
            selectedTokens.addAll(selectedBooksState.value)

            val queryParam = URLEncoder.encode(queryState.value, "UTF-8")
            val sortParam = URLEncoder.encode(sortState.value.name, "UTF-8")

            if (selectedTokens.isNotEmpty()) {
                val filterParam = URLEncoder.encode(selectedTokens.joinToString(","), "UTF-8")
                "/search?query=$queryParam&sort=$sortParam&filter=$filterParam"
            } else {
                "/search?query=$queryParam&sort=$sortParam"
            }
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
        private const val BIBLE_SEARCH_PARTS = "bibleSearchParts"
        private const val BIBLE_SEARCH_SECTIONS = "bibleSearchSections"
        private const val BIBLE_SEARCH_BOOKS = "bibleSearchBooks"
        private const val BIBLE_SEARCH_FILTER_SHEET_VISIBLE = "bibleSearchFilterSheetVisible"
        private const val BIBLE_SEARCH_HAS_URI_FILTER = "bibleSearchHasUriFilter"
        private const val BACK_STACK_FILTER_SHEET = "bible_book_filter_sheet"

        @JvmStatic
        fun newInstance(uri: Uri): BibleSearchFragment {
            val fragment = BibleSearchFragment()
            val args = Bundle().apply {
                putString(BIBLE_SEARCH_QUERY, uri.getQueryParameter("query"))
                putString(BIBLE_SEARCH_SORT, uri.getQueryParameter("sort"))
                val filter = uri.getQueryParameter("filter")
                if (!filter.isNullOrEmpty()) {
                    val tree = BibleTreeRepository.getTree()
                    val (parts, sections, books) = parseFilterParam(filter, tree)
                    putStringArrayList(BIBLE_SEARCH_PARTS, ArrayList(parts))
                    putStringArrayList(BIBLE_SEARCH_SECTIONS, ArrayList(sections))
                    putStringArrayList(BIBLE_SEARCH_BOOKS, ArrayList(books))
                    putBoolean(BIBLE_SEARCH_HAS_URI_FILTER, true)
                }
            }
            fragment.arguments = args
            return fragment
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleSearchScreen(
    query: String = "",
    sort: BibleController.Sort,
    searchResults: List<BibleSearchResult>,
    onSortChanged: (BibleController.Sort) -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialSelectedParts: Set<String> = emptySet(),
    initialSelectedSections: Set<String> = emptySet(),
    initialSelectedBooks: Set<String> = emptySet(),
    onSelectionChanged: ((selectedParts: Set<String>, selectedSections: Set<String>, selectedBooks: Set<String>, flatBookRefs: List<String>?) -> Unit)? = null,
    isFilterSheetVisible: Boolean? = null,
    onOpenFilter: (() -> Unit)? = null,
    onDismissFilter: (() -> Unit)? = null,
    shouldPulseFilter: Boolean = false
) {
    val tree = remember { BibleTreeRepository.getTree() }
    var selectedParts by remember(initialSelectedParts) { mutableStateOf(initialSelectedParts) }
    var selectedSections by remember(initialSelectedSections) { mutableStateOf(initialSelectedSections) }
    var selectedBooks by remember(initialSelectedBooks) { mutableStateOf(initialSelectedBooks) }
    var internalIsFilterSheetVisible by remember { mutableStateOf(false) }

    val referenceTarget = remember(query) { parseBibleReference(query) }

    val showFilterSheet = isFilterSheetVisible ?: internalIsFilterSheetVisible
    val handleOpenFilter = onOpenFilter ?: { internalIsFilterSheetVisible = true }
    val handleDismissFilter = onDismissFilter ?: { internalIsFilterSheetVisible = false }

    val (filterLabel, calculatedFilter) = remember(tree, selectedParts, selectedSections, selectedBooks) {
        computeFilterResult(tree, selectedParts, selectedSections, selectedBooks)
    }

    LaunchedEffect(selectedParts, selectedSections, selectedBooks, calculatedFilter) {
        onSelectionChanged?.invoke(selectedParts, selectedSections, selectedBooks, calculatedFilter)
    }

    val bgColor = getLectureBackgroundColor()
    val dividerColor = getDividerColor()
    val rippleConfiguration = RippleConfiguration(
        color = getAccentColor(),
        rippleAlpha = RippleAlpha(.20f, .20f, .20f, .20f)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 0.dp)
            .clickable(enabled = false) {}
    ) {
        CompositionLocalProvider(LocalRippleConfiguration provides rippleConfiguration) {
            Surface(color = bgColor, modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        FilterAndSortHeader(
                            filterLabel = filterLabel,
                            sort = sort,
                            onOpenFilter = handleOpenFilter,
                            onSortChanged = onSortChanged,
                            shouldPulseFilter = shouldPulseFilter
                        )
                        HorizontalDivider(color = dividerColor, thickness = 1.dp)

                        referenceTarget?.let { target ->
                            ReferenceJumpBadge(
                                target = target,
                                onClick = { onItemClick(target.link) }
                            )
                        }

                        SearchResultsList(
                            results = searchResults,
                            onItemClick = onItemClick
                        )
                    }

                    if (showFilterSheet) {
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        val scope = rememberCoroutineScope()

                        val animateAndDismiss = {
                            scope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    handleDismissFilter()
                                }
                            }
                        }

                        ModalBottomSheet(
                            onDismissRequest = { handleDismissFilter() },
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
                                onDismiss = { animateAndDismiss() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterAndSortHeader(
    filterLabel: String,
    sort: BibleController.Sort,
    onOpenFilter: () -> Unit,
    onSortChanged: (BibleController.Sort) -> Unit,
    modifier: Modifier = Modifier,
    shouldPulseFilter: Boolean = false
) {
    val accentColor = getAccentColor()
    val lectureBgColor = getLectureBackgroundColor()

    val pulseAlpha = remember { Animatable(0f) }

    LaunchedEffect(shouldPulseFilter) {
        if (shouldPulseFilter) {
            delay(150.milliseconds)
            pulseAlpha.animateTo(0.20f, animationSpec = tween(300))
            pulseAlpha.animateTo(0f, animationSpec = tween(1000, easing = EaseOutBounce))
        }
    }

    val buttonBgColor = if (pulseAlpha.value > 0f) {
        accentColor.copy(alpha = pulseAlpha.value)
    } else {
        lectureBgColor
    }

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
            color = buttonBgColor,
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
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        SortBar(
            sort = sort,
            onSortChanged = onSortChanged
        )
    }
}

@Composable
private fun BibleBookFilterSheet(
    tree: List<BibleTreePart>,
    selectedParts: Set<String>,
    selectedSections: Set<String>,
    selectedBooks: Set<String>,
    onSelectedPartsChange: (Set<String>) -> Unit,
    onSelectedSectionsChange: (Set<String>) -> Unit,
    onSelectedBooksChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = getLectureTextColor()
    val accentColor = getAccentColor()
    val dividerColor = getDividerColor()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = textColor
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Livres",
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (selectedParts.isNotEmpty() || selectedSections.isNotEmpty() || selectedBooks.isNotEmpty()) {
                Text(
                    text = "Effacer",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable {
                            onSelectedPartsChange(emptySet())
                            onSelectedSectionsChange(emptySet())
                            onSelectedBooksChange(emptySet())
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 24.dp
            )
        ) {
                for (part in tree) {
                    val isPartChecked = selectedParts.contains(part.id)

                    item(key = "part_${part.id}") {
                        TreePartRow(
                            part = part,
                            isChecked = isPartChecked,
                            onToggle = {
                                val next = selectedParts.toMutableSet()
                                if (isPartChecked) {
                                    next.remove(part.id)
                                } else {
                                    next.add(part.id)
                                }
                                onSelectedPartsChange(next)
                            }
                        )
                    }

                    for (section in part.sections) {
                        val isSectionChecked = isPartChecked || selectedSections.contains(section.id)
                        val isSectionEnabled = !isPartChecked

                        item(key = "section_${section.id}") {
                            TreeSectionRow(
                                section = section,
                                isChecked = isSectionChecked,
                                isEnabled = isSectionEnabled,
                                onToggle = {
                                    if (isSectionEnabled) {
                                        val next = selectedSections.toMutableSet()
                                        if (selectedSections.contains(section.id)) {
                                            next.remove(section.id)
                                        } else {
                                            next.add(section.id)
                                        }
                                        onSelectedSectionsChange(next)
                                    }
                                }
                            )
                        }

                        for (book in section.books) {
                            val isBookChecked = isSectionChecked || selectedBooks.contains(book.ref)
                            val isBookEnabled = !isSectionChecked

                            item(key = "book_${section.id}_${book.ref}") {
                                TreeBookRow(
                                    book = book,
                                    isChecked = isBookChecked,
                                    isEnabled = isBookEnabled,
                                    indentationDp = 52.dp,
                                    onToggle = {
                                        if (isBookEnabled) {
                                            val next = selectedBooks.toMutableSet()
                                            if (selectedBooks.contains(book.ref)) {
                                                next.remove(book.ref)
                                            } else {
                                                next.add(book.ref)
                                            }
                                            onSelectedBooksChange(next)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    for (book in part.directBooks) {
                        val isBookChecked = isPartChecked || selectedBooks.contains(book.ref)
                        val isBookEnabled = !isPartChecked

                        item(key = "direct_book_${part.id}_${book.ref}") {
                            TreeBookRow(
                                book = book,
                                isChecked = isBookChecked,
                                isEnabled = isBookEnabled,
                                indentationDp = 32.dp,
                                onToggle = {
                                    if (isBookEnabled) {
                                        val next = selectedBooks.toMutableSet()
                                        if (selectedBooks.contains(book.ref)) {
                                            next.remove(book.ref)
                                        } else {
                                            next.add(book.ref)
                                        }
                                        onSelectedBooksChange(next)
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
private fun TreePartRow(
    part: BibleTreePart,
    isChecked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = getLectureTextColor()
    val accentColor = getAccentColor()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = accentColor,
                checkmarkColor = getLectureBackgroundColor()
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = part.name,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TreeSectionRow(
    section: BibleTreeSection,
    isChecked: Boolean,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = getLectureTextColor()
    val accentColor = getAccentColor()
    val effectiveTextColor = if (isEnabled) textColor else textColor.copy(alpha = 0.5f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled) { onToggle() }
            .padding(start = 28.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            enabled = isEnabled,
            onCheckedChange = { if (isEnabled) onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = accentColor,
                checkmarkColor = getLectureBackgroundColor(),
                disabledCheckedColor = accentColor.copy(alpha = 0.5f)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = section.name,
            color = effectiveTextColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TreeBookRow(
    book: BibleTreeBook,
    isChecked: Boolean,
    isEnabled: Boolean,
    indentationDp: Dp,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = getLectureTextColor()
    val accentColor = getAccentColor()
    val effectiveTextColor = if (isEnabled) textColor else textColor.copy(alpha = 0.5f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled) { onToggle() }
            .padding(start = indentationDp, end = 16.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            enabled = isEnabled,
            onCheckedChange = { if (isEnabled) onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = accentColor,
                checkmarkColor = getLectureBackgroundColor(),
                disabledCheckedColor = accentColor.copy(alpha = 0.5f)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = book.name,
            color = effectiveTextColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

data class BibleTreeBook(
    val name: String,
    val ref: String
)

data class BibleTreeSection(
    val id: String,
    val name: String,
    val books: List<BibleTreeBook>
)

data class BibleTreePart(
    val id: String,
    val name: String,
    val sections: List<BibleTreeSection>,
    val directBooks: List<BibleTreeBook> = emptyList()
)

sealed class SelectedItem {
    data class PartItem(val part: BibleTreePart) : SelectedItem()
    data class SectionItem(val section: BibleTreeSection) : SelectedItem()
    data class BookItem(val book: BibleTreeBook) : SelectedItem()
}

object BibleTreeRepository {
    private var cachedParts: List<BibleTreePart>? = null

    fun getTree(): List<BibleTreePart> {
        cachedParts?.let { return it }

        val bibleBookList = BibleBookList.getInstance()
        val treeParts = mutableListOf<BibleTreePart>()

        for (part in bibleBookList.parts) {
            val sections = mutableListOf<BibleTreeSection>()
            val directBooks = mutableListOf<BibleTreeBook>()
            val seenBookRefs = mutableSetOf<String>()

            var currentSectionName: String? = null
            var currentSectionBooks = mutableListOf<BibleTreeBook>()

            for (entry in part.bibleBookEntries) {
                if (entry.type == BibleBookEntryType.SECTION) {
                    if (currentSectionName != null && currentSectionBooks.isNotEmpty()) {
                        sections.add(
                            BibleTreeSection(
                                id = "${part.partRef}_${currentSectionName}",
                                name = currentSectionName,
                                books = currentSectionBooks.toList()
                            )
                        )
                    }
                    currentSectionName = entry.bookName
                    currentSectionBooks = mutableListOf()
                } else if (entry.type == BibleBookEntryType.BOOK) {
                    val bookRef = entry.bookRef
                    val bookName = entry.bookName
                    if (bookRef != null && seenBookRefs.add(bookRef)) {
                        val book = BibleTreeBook(name = bookName, ref = bookRef)
                        if (currentSectionName != null) {
                            currentSectionBooks.add(book)
                        } else {
                            directBooks.add(book)
                        }
                    }
                }
            }

            if (currentSectionName != null && currentSectionBooks.isNotEmpty()) {
                sections.add(
                    BibleTreeSection(
                        id = "${part.partRef}_${currentSectionName}",
                        name = currentSectionName,
                        books = currentSectionBooks.toList()
                    )
                )
            }

            treeParts.add(
                BibleTreePart(
                    id = part.partRef,
                    name = part.name,
                    sections = sections,
                    directBooks = directBooks
                )
            )
        }

        cachedParts = treeParts
        return treeParts
    }
}

fun parseFilterParam(
    filterParam: String,
    tree: List<BibleTreePart>
): Triple<Set<String>, Set<String>, Set<String>> {
    val parts = mutableSetOf<String>()
    val sections = mutableSetOf<String>()
    val books = mutableSetOf<String>()

    val validPartIds = tree.map { it.id }.toSet()
    val validSectionIds = tree.flatMap { part -> part.sections.map { it.id } }.toSet()

    val tokens = filterParam.split(",")
    for (rawToken in tokens) {
        val token = rawToken.trim()
        if (token.isEmpty()) continue

        when {
            validPartIds.contains(token) -> parts.add(token)
            validSectionIds.contains(token) -> sections.add(token)
            else -> books.add(token)
        }
    }

    return Triple(parts, sections, books)
}

fun computeFilterResult(
    tree: List<BibleTreePart>,
    selectedParts: Set<String>,
    selectedSections: Set<String>,
    selectedBooks: Set<String>
): Pair<String, List<String>?> {
    val totalPartsCount = tree.size
    val allPartsSelected = selectedParts.size == totalPartsCount

    if ((selectedParts.isEmpty() && selectedSections.isEmpty() && selectedBooks.isEmpty()) || allPartsSelected) {
        return Pair("Dans toute la Bible", null)
    }

    val selectedItems = mutableListOf<SelectedItem>()
    val selectedBookRefs = mutableSetOf<String>()

    for (part in tree) {
        if (selectedParts.contains(part.id)) {
            selectedItems.add(SelectedItem.PartItem(part))
            for (section in part.sections) {
                for (book in section.books) {
                    selectedBookRefs.add(book.ref)
                }
            }
            for (book in part.directBooks) {
                selectedBookRefs.add(book.ref)
            }
        } else {
            for (section in part.sections) {
                if (selectedSections.contains(section.id)) {
                    selectedItems.add(SelectedItem.SectionItem(section))
                    for (book in section.books) {
                        selectedBookRefs.add(book.ref)
                    }
                } else {
                    for (book in section.books) {
                        if (selectedBooks.contains(book.ref)) {
                            selectedItems.add(SelectedItem.BookItem(book))
                            selectedBookRefs.add(book.ref)
                        }
                    }
                }
            }
            for (book in part.directBooks) {
                if (selectedBooks.contains(book.ref)) {
                    selectedItems.add(SelectedItem.BookItem(book))
                    selectedBookRefs.add(book.ref)
                }
            }
        }
    }

    if (selectedItems.isEmpty()) {
        return Pair("Dans toute la Bible", null)
    }

    val firstLabel = getLabelForSelectedItem(selectedItems.first())
    val label = if (selectedItems.size > 1) {
        "$firstLabel et plus."
    } else {
        firstLabel
    }

    return Pair(label, selectedBookRefs.toList())
}

private fun getLabelForSelectedItem(item: SelectedItem): String {
    return when (item) {
        is SelectedItem.PartItem -> getPartLabel(item.part.name)
        is SelectedItem.SectionItem -> getSectionLabel(item.section.name)
        is SelectedItem.BookItem -> getBookLabel(item.book.name)
    }
}

private fun getPartLabel(partName: String): String {
    return when (partName) {
        "Ancien Testament" -> "Dans tout l'Ancien Testament"
        "Nouveau Testament" -> "Dans tout le Nouveau Testament"
        "Psaumes" -> "Dans tous les Psaumes"
        else -> "Dans tout $partName"
    }
}

private fun getSectionLabel(sectionName: String): String {
    return when (sectionName) {
        "Pentateuque" -> "Dans le Pentateuque"
        "Livres Historiques" -> "Dans les Livres Historiques"
        "Livres Poètiques et Sapientiaux" -> "Dans les Livres Poètiques et Sapientiaux"
        "Livres Prophètiques" -> "Dans les Livres Prophètiques"
        "Évangiles" -> "Dans les Évangiles"
        "Actes" -> "Dans les Actes"
        "Lettres de saint Paul" -> "Dans les Lettres de saint Paul"
        "Lettres Catholiques" -> "Dans les Lettres Catholiques"
        "Apocalypse" -> "Dans l'Apocalypse"
        else -> {
            if (sectionName.startsWith("Livres") || sectionName.startsWith("Lettres")) {
                "Dans les $sectionName"
            } else {
                "Dans $sectionName"
            }
        }
    }
}

private fun getBookLabel(bookName: String): String {
    return when {
        bookName.startsWith("Livre") || bookName.startsWith("Premier") || bookName.startsWith("Deuxième livre") -> "Dans le $bookName"
        bookName.startsWith("Première") || bookName.startsWith("Deuxième lettre") || bookName.startsWith("Troisième lettre") || bookName.startsWith("Lettre") -> "Dans la $bookName"
        bookName.startsWith("Évangile") -> "Dans l'$bookName"
        bookName.startsWith("Actes") -> "Dans les $bookName"
        bookName.startsWith("Cantique") -> "Dans le $bookName"
        bookName.startsWith("Apocalypse") -> "Dans l'$bookName"
        else -> "Dans $bookName"
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

@Preview(showBackground = true)
@Composable
private fun BibleBookFilterSheetPreview() {
    val tree = remember { BibleTreeRepository.getTree() }
    Surface(color = getLectureBackgroundColor()) {
        BibleBookFilterSheet(
            tree = tree,
            selectedParts = emptySet(),
            selectedSections = emptySet(),
            selectedBooks = emptySet(),
            onSelectedPartsChange = {},
            onSelectedSectionsChange = {},
            onSelectedBooksChange = {},
            onDismiss = {}
        )
    }
}

data class BibleReferenceTarget(
    val bookRef: String,
    val chapterRef: String,
    val referenceParam: String?,
    val displayRef: String,
    val link: String
)

private val refRegex = Regex(
    """^(?:([1-3])\s*)?([a-zA-ZàâéèêëîïôùûüçÀÂÉÈÊËÎÏÔÙÛÜÇ]+)\s*(\d+[a-bA-B]*)?(?:([\s,:-]+)(.*))?${'$'}"""
)

fun parseBibleReference(query: String): BibleReferenceTarget? {
    if (query.length < 2) {
        return null
    }
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
        return null
    }

    val match = refRegex.matchEntire(trimmed) ?: return null

    val numberPrefix = match.groupValues[1]
    val letters = match.groupValues[2]
    val chapter = match.groupValues[3]
    val sep = match.groupValues[4]
    val rest = match.groupValues[5].trim()

    val rawBook = if (numberPrefix.isNotEmpty()) "$numberPrefix $letters" else letters
    val bookEntry = BibleBookList.getInstance().findBookEntry(rawBook) ?: return null
    val canonicalBookRef = bookEntry.bookRef ?: return null
    val shortBookName = bookEntry.shortBookName ?: rawBook

    val chapterRef = if (chapter.isNotEmpty()) chapter else "1"

    val referenceParam = if (rest.isNotEmpty()) {
        val cleanRest = rest.trimStart(',', ':', ' ')
        if (cleanRest.isNotEmpty()) {
            if (cleanRest.startsWith("$chapterRef,") || cleanRest.startsWith("$chapterRef:")) {
                cleanRest.replace(":", ",")
            } else {
                "$chapterRef,${cleanRest.replace(":", ",")}"
            }
        } else {
            null
        }
    } else {
        null
    }

    val link = if (referenceParam != null) {
        "https://www.aelf.org/bible/$canonicalBookRef/$chapterRef?reference=${URLEncoder.encode(referenceParam, "UTF-8")}"
    } else {
        "https://www.aelf.org/bible/$canonicalBookRef/$chapterRef"
    }

    val displayRef = formatBibleReferenceDisplay(shortBookName, chapter, sep, rest)

    return BibleReferenceTarget(
        bookRef = canonicalBookRef,
        chapterRef = chapterRef,
        referenceParam = referenceParam,
        displayRef = displayRef,
        link = link
    )
}

fun formatBibleReferenceDisplay(shortBookName: String, chapter: String, sep: String, rest: String): String {
    if (chapter.isEmpty()) {
        return shortBookName
    }

    val trimmedRest = rest.trim()
    if (trimmedRest.isEmpty()) {
        return "$shortBookName chapitre $chapter"
    }

    val startChapterNum = chapter.toIntOrNull()

    // Check multi-chapter range without verses: e.g. "Mt 26-27" or "Mt 26 - 27" or "Mt 26 à 27"
    if (sep.contains("-") || sep.contains("à")) {
        val endChapterNum = trimmedRest.toIntOrNull()
        if (endChapterNum != null && startChapterNum != null && endChapterNum > startChapterNum) {
            return "$shortBookName chapitres $chapter à $endChapterNum"
        }
    }

    // Check multi-chapter range without verses when rest starts with "-" or "à": e.g. "-27", "- 27"
    val multiChapterNoVersesRegex = Regex("""^\s*(?:-|à)\s*(\d+)\s*$""")
    val multiChapterMatch = multiChapterNoVersesRegex.matchEntire(trimmedRest)
    if (multiChapterMatch != null) {
        val endChapter = multiChapterMatch.groupValues[1]
        val endChapterNum = endChapter.toIntOrNull()
        if (startChapterNum == null || endChapterNum == null || endChapterNum > startChapterNum) {
            return "$shortBookName chapitres $chapter à $endChapter"
        }
    }

    // Check multi-chapter with verses: e.g. "36 - 27, 5" or "36-27:5" or "36 à 27, 5"
    val multiChapterWithVersesRegex = Regex("""^\s*(\d+[a-bA-B]*(?:\s*-\s*\d+[a-bA-B]*)?)\s*(?:-|\bà\b)\s*(\d+)\s*[:,]\s*(\d+.*)$""")
    val multiChapterVerseMatch = multiChapterWithVersesRegex.matchEntire(trimmedRest)
    if (multiChapterVerseMatch != null) {
        val startVersePart = multiChapterVerseMatch.groupValues[1].trim()
        val endChapter = multiChapterVerseMatch.groupValues[2].trim()
        val endVersePart = multiChapterVerseMatch.groupValues[3].trim()
        val endChapterNum = endChapter.toIntOrNull()

        if (startChapterNum != null && endChapterNum != null && endChapterNum > startChapterNum) {
            val formattedStartVerses = formatVerseExpression(startVersePart)
            val formattedEndVerses = formatVerseExpression(endVersePart)

            return "$shortBookName chapitre $chapter $formattedStartVerses à chapitre $endChapter $formattedEndVerses"
        }
    }

    // Single chapter with verses: e.g. "36", "36-46", "36, 38", "1-3, 6, 8-10"
    val formattedVerses = formatVerseExpression(trimmedRest)
    return "$shortBookName chapitre $chapter $formattedVerses"
}

private fun formatVerseExpression(versesExpr: String): String {
    val clean = versesExpr.trimStart(',', ':', ' ', '-').trimEnd(',', ':', ' ')
    if (clean.isEmpty()) return ""

    // Split into clauses by comma, dot, or semicolon or "et"
    val clauses = clean.split(Regex("""[,.;]+|\bet\b"""))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    if (clauses.isEmpty()) return ""

    val formattedClauses = clauses.map { clause ->
        if (clause.contains("-") || clause.contains("à")) {
            val parts = clause.split(Regex("""\s*-\s*|\s+à\s+"""))
            if (parts.size >= 2) {
                "${parts[0].trim()} à ${parts[1].trim()}"
            } else {
                clause
            }
        } else {
            clause
        }
    }

    val isSingleVerse = formattedClauses.size == 1 && !formattedClauses[0].contains("à")
    val prefix = if (isSingleVerse) "verset" else "versets"

    val joined = when (formattedClauses.size) {
        1 -> formattedClauses[0]
        2 -> "${formattedClauses[0]} et ${formattedClauses[1]}"
        else -> "${formattedClauses.dropLast(1).joinToString(", ")} et ${formattedClauses.last()}"
    }

    return "$prefix $joined"
}

@Composable
private fun ReferenceJumpBadge(
    target: BibleReferenceTarget,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = getAccentColor()
    val buttonBgColor = getLectureBackgroundColor()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = buttonBgColor,
            border = BorderStroke(1.dp, accentColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Accéder à ${target.displayRef}",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

