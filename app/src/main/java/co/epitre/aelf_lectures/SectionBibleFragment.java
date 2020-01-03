package co.epitre.aelf_lectures;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import co.epitre.aelf_lectures.bible.BibleBookFragment;
import co.epitre.aelf_lectures.bible.BibleBookListAdapter;
import co.epitre.aelf_lectures.bible.BibleFragment;
import co.epitre.aelf_lectures.bible.BibleMenuFragment;
import co.epitre.aelf_lectures.bible.BibleSearchEngine;
import co.epitre.aelf_lectures.bible.BibleSearchFragment;


/**
 * Created by jean-tiare on 12/03/18.
 */

public class SectionBibleFragment extends SectionFragmentBase {
    public static final String TAG = "SectionBibleFragment";

    public SectionBibleFragment(){
        // Required empty public constructor
    }

    /**
     * Global managers / resources
     */
    private boolean initialized = false;
    FragmentManager mFragmentManager;
    SharedPreferences settings = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Init the search engine asynchronously
        BibleSearchEngine.getInstance();

        // Load settings
        settings = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_section_bible, container, false);

        // Make sure to select the menu entry
        drawerView.setCheckedItem(R.id.nav_bible);

        // Get fragment manager
        mFragmentManager = getChildFragmentManager();

        // Load intent
        Intent intent = activity.getIntent();
        Uri uri = intent.getData();

        // Load selected state
        if (initialized) {
            // Coming from back button. Re-Attach fragment
        } else if (uri != null) {
            // Load requested URL
            onLink(uri);
        } else if(Intent.ACTION_SEARCH.equals(intent.getAction())) {
            onSearch(intent.getStringExtra(SearchManager.QUERY));
        } else if (savedInstanceState != null) {
            // Nothing to do
        } else {
            // load default page
            onLink(null);
        }

        initialized = true;
        return view;
    }

    @Override
    public void onLink(Uri uri) {
        if (mFragmentManager == null) {
            return;
        }

        // Route
        Fragment newBibleFragment;
        if (uri == null) {
            // To the menu fragment, first page
            newBibleFragment = BibleMenuFragment.newInstance(0);
        } else if (uri.getPath().equals("/bible")) {
            // To the menu fragment
            newBibleFragment = BibleMenuFragment.newInstance(uri);
        } else if (uri.getPath().equals("/search")) {
            // To the search fragment
            newBibleFragment = BibleSearchFragment.newInstance(uri);
        } else {
            // To the Bible fragment
            newBibleFragment = BibleBookFragment.newInstance(uri);
        }

        // Start selected fragment
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.bible_container, newBibleFragment);
        if (getCurrentBibleFragment() != null) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    @Override
    public void onSearch(String query) {
        Uri uri = Uri.parse("https://www.aelf.org/search?query="+query);
        onLink(uri);
    }

    //
    // Option menu
    //

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflate the menu; this adds items to the action bar
        inflater.inflate(R.menu.toolbar_bible, menu);

        // Associate searchable configuration with the SearchView
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // Directly open the search fragment when the search button is pressed unless on the search page
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(getCurrentBibleFragment() instanceof BibleSearchFragment)) {
                    onSearch("");
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                return onShare();
        }
        return super.onOptionsItemSelected(item);
    }

    private BibleFragment getCurrentBibleFragment() {
        return (BibleFragment) mFragmentManager.findFragmentById(R.id.bible_container);
    }

    //
    // API
    //

    public Uri getUri() {
        String route = "";
        BibleFragment currentBibleFragment = getCurrentBibleFragment();
        if (currentBibleFragment != null) {
            route = currentBibleFragment.getRoute();
        }
        return Uri.parse("https://www.aelf.org"+route);
    }

    public String getTitle() {
        BibleFragment currentBibleFragment = getCurrentBibleFragment();
        if (currentBibleFragment != null) {
            return currentBibleFragment.getTitle();
        }
        return "Bible de la liturgie";
    }

    public void openBook(int biblePartId, int bibleBookId) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        BibleFragment newBibleFragment = BibleBookFragment.newInstance(biblePartId, bibleBookId);
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.bible_container, newBibleFragment);
        if (getCurrentBibleFragment() != null) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
    }

    //
    // Lifecycle
    //

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // STUB
    }

    @Override
    public boolean onBackPressed() {
        return getChildFragmentManager().popBackStackImmediate();
    }

    //
    // Events
    //

    @Subscribe
    public void onBibleEntryClick(BibleBookListAdapter.OnBibleEntryClickEvent event) {
        openBook(event.mBiblePartId, event.mBibleBookId);
    }

    public boolean onShare() {
        // Get current URL
        String uri = getUri().toString();

        // Get current webview title
        String title = getTitle();

        // Build share message
        String message = title + ": " + uri;

        // Create the intent
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)));

        // All done !
        return true;
    }
}
