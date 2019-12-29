package co.epitre.aelf_lectures.bible;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import co.epitre.aelf_lectures.LecturesActivity;
import co.epitre.aelf_lectures.R;

public class BibleSearchFragment extends BibleFragment implements BibleSearchResultAdapter.ItemClickListener {
    /**
     * Global Views
     */
    protected ActionBar actionBar;
    protected NavigationView drawerView;
    protected LecturesActivity activity;

    /**
     * Results
     */
    private String query = "";
    RecyclerView mRecyclerView;
    BibleSearchResultAdapter mResultAdapter;

    public static BibleSearchFragment newInstance() {
        return new BibleSearchFragment();
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

        // Get the search query
        Intent intent = activity.getIntent();
        if(Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
        }

        // Set section title
        actionBar.setTitle(getTitle());

        // Perform the search
        Cursor cursor = BibleSearchEngine.getInstance().search(query);

        // Set up the RecyclerView
        mRecyclerView = view.findViewById(R.id.search_results);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mResultAdapter = new BibleSearchResultAdapter(getContext(), cursor);
        mResultAdapter.setClickListener(this);
        mRecyclerView.setAdapter(mResultAdapter);
        updateListBottomMargin();

        return view;
    }

    @Override
    public String getRoute() {
        return "/search";
    }

    @Override
    public String getTitle() {
        return "Recherche « "+query+" »";
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
