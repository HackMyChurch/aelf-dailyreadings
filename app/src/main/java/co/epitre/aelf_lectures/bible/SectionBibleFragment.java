package co.epitre.aelf_lectures.bible;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.base.SectionFragment;
import co.epitre.aelf_lectures.bible.data.BibleController;
import co.epitre.aelf_lectures.settings.SettingsActivity;


/**
 * Created by jean-tiare on 12/03/18.
 */

public class SectionBibleFragment extends SectionFragment {
    public static final String TAG = "SectionBibleFragment";

    public SectionBibleFragment(){
        // Required empty public constructor
    }

    /**
     * Global managers / resources
     */
    private boolean initialized = false;
    private boolean fragmentLoaded = false;
    FragmentManager mFragmentManager;
    SharedPreferences settings = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Init the search engine asynchronously
        BibleController.getInstance();

        // Load settings
        settings = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());

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
        } else if (savedInstanceState != null) {
            // Nothing to do
        } else if (uri != null) {
            String path = uri.getPath();
            if (path != null && path.equals("/bible/home")) {
                // Handle "Home": Load default or last visited page
                onHome();
            } else {
                // Load requested URL
                onLink(uri);
            }
        } else if(Intent.ACTION_SEARCH.equals(intent.getAction())) {
            onSearch(intent.getStringExtra(SearchManager.QUERY));
        } else {
            // load default page
            onLink(null);
        }

        initialized = true;
        return view;
    }

    private void onHome() {
        // Load the last visited uri
        String lastPageStr = settings.getString(SettingsActivity.KEY_BIBLE_LAST_PAGE, null);
        if (lastPageStr == null) {
            lastPageStr = "https://www.aelf.org/bible";
        }

        // Parse and extract the path
        Uri lastPage = Uri.parse(lastPageStr);
        String lastPagePath = lastPage.getPath();

        // Inject a "home" page in the back stack, unless requesting the home page
        if (lastPagePath != null && !lastPagePath.equals("/bible")) {
            onLink(null);
        }

        // Restore last viewed page
        onLink(lastPage);
    }

    public void onLink(Uri uri) {
        if (mFragmentManager == null) {
            return;
        }

        // Route
        Fragment newBibleFragment;
        if (uri == null) {
            // To the menu fragment, first page
            newBibleFragment = BibleMenuFragment.newInstance(0);
        } else {
            String path = uri.getPath();
            if (path == null) {
                // To the Bible fragment
                newBibleFragment = BibleBookFragment.newInstance(uri);
            } else if (path.equals("/bible")) {
                // To the menu fragment
                newBibleFragment = BibleMenuFragment.newInstance(uri);
            } else if (path.equals("/search")) {
                // To the search fragment
                newBibleFragment = BibleSearchFragment.newInstance(uri);
            } else {
                // To the Bible fragment
                newBibleFragment = BibleBookFragment.newInstance(uri);
            }
        }

        // Handle errors when parsing the URL
        if (newBibleFragment == null) {
            // Fallback on the menu fragment (instead of crashing miserably)
            newBibleFragment = BibleMenuFragment.newInstance(uri);
        }

        // Start selected fragment
        setFragment(newBibleFragment);
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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
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
        if (searchView == null ) {
            return;
        }

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
        int itemId = item.getItemId();

        if (itemId == R.id.action_share) {
            return onShare();
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private BibleFragment getCurrentBibleFragment() {
        return (BibleFragment) mFragmentManager.findFragmentById(R.id.bible_container);
    }

    private void setFragment(Fragment newBibleFragment) {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.bible_container, newBibleFragment);
        if (fragmentLoaded) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commit();
        fragmentLoaded = true;
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
        setFragment(newBibleFragment);
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
    public void onPause() {
        String lastPage = getUri().toString();

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SettingsActivity.KEY_BIBLE_LAST_PAGE, lastPage);
        editor.apply();

        super.onPause();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // STUB
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mFragmentManager.getBackStackEntryCount() > 0) {
                    // Move back in the current stack
                    mFragmentManager.popBackStack();
                } else {
                    // Delete parent fragment
                    getParentFragmentManager().popBackStack();
                }
            }
        });
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
