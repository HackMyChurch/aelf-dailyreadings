package co.epitre.aelf_lectures.bible

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import co.epitre.aelf_lectures.R
import co.epitre.aelf_lectures.base.SectionFragment
import co.epitre.aelf_lectures.bible.data.bible.BibleController
import co.epitre.aelf_lectures.settings.SettingsActivity

/**
 * Created by jean-tiare on 12/03/18.
 */
class SectionBibleFragment : SectionFragment() {

    private var initialized = false
    private var fragmentLoaded = false
    private var mFragmentManager: FragmentManager? = null
    private var settings: SharedPreferences? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        // Init the search engine asynchronously
        BibleController.getInstance()

        val act = activity ?: return null

        // Load settings
        settings = PreferenceManager.getDefaultSharedPreferences(act.applicationContext)

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_section_bible, container, false)

        // Make sure to select the menu entry
        drawerView.setCheckedItem(R.id.nav_bible)
        setDrawerHeaderView(R.layout.navigation_drawer_header_bible)

        // Get fragment manager
        mFragmentManager = childFragmentManager

        // Load intent
        val intent = act.intent
        val uri = intent.data

        // Load selected state
        if (initialized) {
            // Coming from back button. Re-Attach fragment
        } else if (savedInstanceState != null) {
            // Nothing to do
        } else if (uri != null) {
            val path = uri.path
            if (path != null && path == "/bible/home") {
                // Handle "Home": Load default or last visited page
                onHome()
            } else {
                // Load requested URL
                onLink(uri)
            }
        } else if (Intent.ACTION_SEARCH == intent.action) {
            onSearch(intent.getStringExtra(SearchManager.QUERY))
        } else {
            // load default page
            onLink(null)
        }

        initialized = true
        return view
    }

    private fun onHome() {
        // Load the last visited uri
        var lastPageStr = settings?.getString(SettingsActivity.KEY_BIBLE_LAST_PAGE, null)
        if (lastPageStr == null) {
            lastPageStr = "https://www.aelf.org/bible"
        }

        // Parse and extract the path
        val lastPage = Uri.parse(lastPageStr)
        val lastPagePath = lastPage.path

        // Inject a "home" page in the back stack, unless requesting the home page
        if (lastPagePath != null && lastPagePath != "/bible") {
            onLink(null)
        }

        // Restore last viewed page
        onLink(lastPage)
    }

    fun onLink(uri: Uri?) {
        if (mFragmentManager == null) {
            return
        }

        // Route
        var newBibleFragment: Fragment? = if (uri == null) {
            // To the menu fragment, first page
            BibleMenuFragment.newInstance(0)
        } else {
            val path = uri.path
            when (path) {
                null -> BibleBookFragment.newInstance(uri)
                "/bible" -> BibleMenuFragment.newInstance(uri)
                "/search" -> BibleSearchFragment.newInstance(uri)
                else -> BibleBookFragment.newInstance(uri)
            }
        }

        // Handle errors when parsing the URL
        if (newBibleFragment == null) {
            // Fallback on the menu fragment (instead of crashing miserably)
            newBibleFragment = if (uri != null) {
                BibleMenuFragment.newInstance(uri)
            } else {
                BibleMenuFragment.newInstance(0)
            }
        }

        // Start selected fragment
        setFragment(newBibleFragment)
    }

    override fun onSearch(query: String?) {
        var url = "https://www.aelf.org/search?query=" + (if (query != null) Uri.encode(query) else "")
        val current = currentBibleFragment
        if (current is BibleBookFragment) {
            val bookRef = current.bookRef
            if (!bookRef.isNullOrEmpty() && bookRef.trim().isNotEmpty()) {
                url += "&filter=" + Uri.encode(bookRef.trim())
            }
        }
        val uri = Uri.parse(url)
        onLink(uri)
    }

    //
    // Option menu
    //

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        // Inflate the menu; this adds items to the action bar
        inflater.inflate(R.menu.toolbar_bible, menu)

        // Associate searchable configuration with the SearchView
        val act = activity ?: return

        val searchManager = act.getSystemService(Context.SEARCH_SERVICE) as? SearchManager ?: return
        val searchView = menu.findItem(R.id.action_search)?.actionView as? SearchView ?: return

        searchView.setSearchableInfo(searchManager.getSearchableInfo(act.componentName))
        searchView.maxWidth = Int.MAX_VALUE

        // Directly open the search fragment when the search button is pressed unless on the search page
        searchView.setOnSearchClickListener {
            if (currentBibleFragment !is BibleSearchFragment) {
                onSearch("")
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId

        return if (itemId == R.id.action_share) {
            onShare()
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private val currentBibleFragment: BibleFragment?
        get() = mFragmentManager?.findFragmentById(R.id.bible_container) as? BibleFragment

    private fun setFragment(newBibleFragment: Fragment) {
        val fm = mFragmentManager ?: return
        val fragmentTransaction = fm.beginTransaction()
        fragmentTransaction.replace(R.id.bible_container, newBibleFragment)
        if (fragmentLoaded) {
            fragmentTransaction.addToBackStack(null)
        }
        fragmentTransaction.commit()
        fragmentLoaded = true
    }

    //
    // API
    //

    override fun getUri(): Uri {
        var route = ""
        val currentBibleFragment = currentBibleFragment
        if (currentBibleFragment != null) {
            route = currentBibleFragment.route
        }
        return Uri.parse("https://www.aelf.org$route")
    }

    fun getTitle(): String {
        val currentBibleFragment = currentBibleFragment
        if (currentBibleFragment != null) {
            return currentBibleFragment.title
        }
        return "Bible de la liturgie"
    }

    //
    // Lifecycle
    //

    override fun onPause() {
        val lastPage = getUri().toString()

        val editor = settings?.edit()
        editor?.putString(SettingsActivity.KEY_BIBLE_LAST_PAGE, lastPage)
        editor?.apply()

        super.onPause()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val act = activity ?: return

        act.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fm = mFragmentManager
                if (fm != null && fm.backStackEntryCount > 0) {
                    // Move back in the current stack
                    fm.popBackStack()
                } else {
                    // Delete parent fragment
                    parentFragmentManager.popBackStack()
                }
            }
        })
    }

    //
    // Events
    //

    fun onShare(): Boolean {
        // Get current URL
        val uri = getUri().toString()

        // Get current webview title
        val title = getTitle()

        // Build share message
        val message = "$title: $uri"

        // Create the intent
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))

        // All done !
        return true
    }

    companion object {
        const val TAG = "SectionBibleFragment"
    }
}
