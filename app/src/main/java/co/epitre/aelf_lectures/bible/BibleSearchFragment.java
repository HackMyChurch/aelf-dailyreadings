package co.epitre.aelf_lectures.bible;

import android.app.Activity;
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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import co.epitre.aelf_lectures.LecturesActivity;
import co.epitre.aelf_lectures.R;


public class BibleSearchFragment extends BibleFragment implements BibleSearchResultAdapter.ItemClickListener {
    /**
     * Internal
     */

    private final static String BIBLE_SEARCH_QUERY = "bibleSearchQuery";

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
    private Semaphore mEditSearchSemaphore = new Semaphore(1);
    private SearchRunnable mSearchRunnable;
    private ExecutorService mSearchExecutorService = Executors.newSingleThreadExecutor();
    private String mQuery = "";
    RecyclerView mRecyclerView;
    BibleSearchResultAdapter mResultAdapter;

    public static BibleSearchFragment newInstance(Uri uri) {
        BibleSearchFragment fragment = new BibleSearchFragment();

        Bundle args = new Bundle();
        args.putString(BIBLE_SEARCH_QUERY, uri.getQueryParameter("query"));
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

        // Set up the RecyclerView
        mRecyclerView = view.findViewById(R.id.search_results);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Perform the search
        mQuery = getArguments().getString(BIBLE_SEARCH_QUERY);
        search(mQuery);

        updateListBottomMargin();

        return view;
    }

    private void search(String query) {
        // Enqueue search job, overriding any pending search
        mEditSearchSemaphore.acquireUninterruptibly();
        if (mSearchRunnable == null) {
            mSearchRunnable = new SearchRunnable(query);
            mSearchExecutorService.submit(mSearchRunnable);
        } else {
            mSearchRunnable.setQuery(query);
        }
        mEditSearchSemaphore.release();
    }

    /**
     * Search task. The search task can be edited until it is started.
     */
    class SearchRunnable implements Runnable {
        private String mQuery;
        private Semaphore mEditSemaphore;

        SearchRunnable(String query) {
            mQuery = query;
            mEditSemaphore = new Semaphore(1);
        }

        boolean setQuery(String query) {
            if (!mEditSemaphore.tryAcquire()) {
                return false;
            }
            mQuery = query;
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
                Cursor cursor = BibleSearchEngine.getInstance().search(mQuery);
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
                    BibleSearchFragment.this.mQuery = mQuery;
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Expand the search box
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        searchMenuItem.expandActionView();

        // Initialize the search box with the query
        mSearchView = (SearchView) searchMenuItem.getActionView();
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setQuery(mQuery, false);

        // Register search listener
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
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
                // Send empty query to clear the results
                if (query == null || query.isEmpty()) {
                    search(query);
                } else if (query.length() >= 3) {
                    search(query+"*");
                }
                return true;
            }
        });

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
        return "/search";
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
    // Lifecycle
    //

    @Override
    public void onStop() {
        // Save the current search query so that it is restored when moving back
        super.onStop();
        if (mSearchView != null) {
            String query = mSearchView.getQuery().toString();
            getArguments().putString(BIBLE_SEARCH_QUERY, query);
        }
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

        mainActivity.onLectureLink(Uri.parse(link));
    }
}
