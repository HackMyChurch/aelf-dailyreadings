package co.epitre.aelf_lectures.bible;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import co.epitre.aelf_lectures.LecturesActivity;
import co.epitre.aelf_lectures.R;

public class BibleSearchFragment extends BibleFragment implements
        BibleSearchResultAdapter.ItemClickListener,
        SearchView.OnQueryTextListener,
        View.OnClickListener {
    /**
     * Internal
     */

    private final static String BIBLE_SEARCH_QUERY = "bibleSearchQuery";
    private final static String BIBLE_SEARCH_SORT = "bibleSearchSort";

    /**
     * Global Views
     */
    protected ActionBar actionBar;
    protected NavigationView drawerView;
    protected LecturesActivity activity;
    protected SearchView mSearchView;

    /**
     * Results
     */
    private boolean initialized = false;
    private Semaphore mEditSearchSemaphore = new Semaphore(1);
    private SearchRunnable mSearchRunnable;
    private ExecutorService mSearchExecutorService = Executors.newSingleThreadExecutor();
    private String mQuery = "";
    private BibleSearchEngine.Sort mSort = BibleSearchEngine.Sort.Relevance;
    RecyclerView mRecyclerView;
    BibleSearchResultAdapter mResultAdapter;

    public static BibleSearchFragment newInstance(Uri uri) {
        BibleSearchFragment fragment = new BibleSearchFragment();

        Bundle args = new Bundle();
        args.putString(BIBLE_SEARCH_QUERY, uri.getQueryParameter("query"));
        args.putString(BIBLE_SEARCH_SORT, uri.getQueryParameter("sort"));
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Load global views
        activity = (LecturesActivity) getActivity();
        actionBar = activity.getSupportActionBar();
        drawerView = activity.findViewById(R.id.drawer_navigation_view);

        // Option menu
        setHasOptionsMenu(true);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_section_bible_search, container, false);

        // Set section title
        actionBar.setTitle(getTitle());

        // Load selected state
        if (initialized) {
            // Coming from back button.
        } else {
            // Get state source
            Bundle args = getArguments();
            if (savedInstanceState != null) {
                args = savedInstanceState;
            }

            // Load the argument
            mQuery = args.getString(BIBLE_SEARCH_QUERY);
            try {
                mSort = BibleSearchEngine.Sort.valueOf(args.getString(BIBLE_SEARCH_SORT));
            } catch (IllegalArgumentException | NullPointerException e) {
            }

            // Setup the sort buttons
            switch (mSort) {
                case Relevance:
                    ((RadioButton) view.findViewById(R.id.radio_sort_relevance)).setChecked(true);
                    break;
                case Bible:
                    ((RadioButton) view.findViewById(R.id.radio_sort_bible)).setChecked(true);
                    break;
            }
        }
        view.findViewById(R.id.radio_sort_bible).setOnClickListener(this);
        view.findViewById(R.id.radio_sort_relevance).setOnClickListener(this);

        // Set up the RecyclerView
        mRecyclerView = view.findViewById(R.id.search_results);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Perform the search
        search(mQuery);

        updateListBottomMargin();

        initialized = true;
        return view;
    }

    private void search(String query) {
        mQuery = query;

        // Add a wildcard to the last word if the query is long enough and does not already end with a wildcard
        if (query != null && query.length() >= 3 && !query.endsWith("*")) {
            query = query + "*";
        }

        // Enqueue search job, overriding any pending search
        mEditSearchSemaphore.acquireUninterruptibly();
        if (mSearchRunnable == null) {
            mSearchRunnable = new SearchRunnable(query, mSort);
            mSearchExecutorService.submit(mSearchRunnable);
        } else {
            mSearchRunnable.setQuery(query, mSort);
        }
        mEditSearchSemaphore.release();
    }

    private void setSort(BibleSearchEngine.Sort sort) {
        mSort = sort;
        search(mQuery);
    }

    /**
     * Search task. The search task can be edited until it is started.
     */
    class SearchRunnable implements Runnable {
        private String mQuery;
        private BibleSearchEngine.Sort mSort;
        private Semaphore mEditSemaphore;

        SearchRunnable(String query, BibleSearchEngine.Sort sort) {
            mQuery = query;
            mSort = sort;
            mEditSemaphore = new Semaphore(1);
        }

        boolean setQuery(String query, BibleSearchEngine.Sort sort) {
            if (!mEditSemaphore.tryAcquire()) {
                return false;
            }
            mQuery = query;
            mSort = sort;
            mEditSemaphore.release();
            return true;
        }

        @Override
        public void run() {
            // This task can no longer be edited
            mEditSearchSemaphore.acquireUninterruptibly();
            mSearchRunnable = null;
            mEditSearchSemaphore.release();
            mEditSemaphore.acquireUninterruptibly();

            // Perform the search
            if (mQuery == null || mQuery.isEmpty()) {
                // Skip search
                mResultAdapter = null;
            } else {
                // Run real search
                Cursor cursor = BibleSearchEngine.getInstance().search(mQuery, mSort);
                cursor.moveToPosition(0);

                // Create the new adapter
                mResultAdapter = new BibleSearchResultAdapter(getContext(), cursor, mQuery);
                mResultAdapter.setClickListener(BibleSearchFragment.this);
            }

            // Update the UI from the main thread
            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.swapAdapter(mResultAdapter, true);
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Expand the search box
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);

        if (searchMenuItem == null) {
            return;
        }

        searchMenuItem.expandActionView();

        // Initialize the search box with the query
        mSearchView = (SearchView) searchMenuItem.getActionView();
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setQuery(mQuery, false);

        // Register search listener
        mSearchView.setOnQueryTextListener(this);

        // Go back to the previous fragment when the search box is closed
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                activity.onBackPressed();
                return false;
            }
        });
    }

    @Override
    public String getRoute() {
        try {
            return "/search?query="+URLEncoder.encode(mQuery, "utf8")+"&sort="+URLEncoder.encode(mSort.name(), "utf8");
        } catch (UnsupportedEncodingException e) {
            return "/search";
        }
    }

    @Override
    public String getTitle() {
        return "Recherche";
    }

    private void updateListBottomMargin() {
        // Inject margin at the bottom to account for the navigation bar
        Resources resources = getContext().getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        int navigationBarHeight = 0;
        if (resourceId > 0) {
            navigationBarHeight = (int)(resources.getDimension(resourceId) / getResources().getDisplayMetrics().density);
        }

        // Update the layout params
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mRecyclerView.getLayoutParams();
        params.bottomMargin = navigationBarHeight;
        mRecyclerView.setLayoutParams(params);
    }

    //
    // SearchView listener
    //

    @Override
    public boolean onQueryTextSubmit(String query) {
        // Start the search asynchronously
        search(query);

        // Close keyboard
        mRecyclerView.requestFocus();
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);

        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        // Start the search asynchronously
        search(query);
        return true;
    }

    //
    // Lifecycle
    //

    @Override
    public void onStart() {
        super.onStart();
        if (mSearchView != null) {
            // Re-register our listener
            mSearchView.setOnQueryTextListener(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSearchView != null) {
            // Un-register our listener to avoid messing with the state when in background
            mSearchView.setOnQueryTextListener(null);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(BIBLE_SEARCH_QUERY, mQuery);
        outState.putString(BIBLE_SEARCH_SORT, mSort.name());
    }

    //
    // Events
    //

    @Override
    public void onItemClick(String link) {
        LecturesActivity mainActivity;
        try {
            mainActivity = (LecturesActivity) getActivity();
        } catch (ClassCastException e) {
            return;
        }

        Uri uri = Uri.parse(link);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        mainActivity.onIntent(intent);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.radio_sort_bible:
                if (((RadioButton)view).isChecked()){
                    setSort(BibleSearchEngine.Sort.Bible);
                }
                break;
            case R.id.radio_sort_relevance:
                if (((RadioButton)view).isChecked()){
                    setSort(BibleSearchEngine.Sort.Relevance);
                }
                break;
        }
    }
}
