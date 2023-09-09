package co.epitre.aelf_lectures.bible;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import co.epitre.aelf_lectures.R;

public class BibleMenuBookListFragment extends Fragment {
    /**
     * Internal
     */

    private final static String BIBLE_PART_ID = "biblePartId";

    /**
     * Device status
     */
    private boolean is_tablet;
    private boolean is_landscape;

    /**
     * Book list
     */
    private int mBiblePartId;
    private RecyclerView mRecyclerView;
    private BibleBookListAdapter mAdapter;
    private GridLayoutManager mLayoutManager;

    public static BibleMenuBookListFragment newInstance(int biblePartId) {
        BibleMenuBookListFragment fragment = new BibleMenuBookListFragment();

        Bundle args = new Bundle();
        args.putInt(BIBLE_PART_ID, biblePartId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_section_bible_book_list, container, false);

        // Grab the book list view
        mRecyclerView = view.findViewById(R.id.book_list);
        mRecyclerView.setHasFixedSize(true);

        // Use a grid layout manager
        mLayoutManager = new GridLayoutManager(getContext(), 1);
        mRecyclerView.setLayoutManager(mLayoutManager);
        updateLayoutManagerSpan(getResources().getConfiguration());
        updateListBottomMargin();

        // Load the Bible part book list
        mBiblePartId = getArguments().getInt(BIBLE_PART_ID, 0);

        // Specify an adapter
        mAdapter = new BibleBookListAdapter(mBiblePartId);
        mRecyclerView.setAdapter(mAdapter);

        // Automatically expand span for the titles
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (mAdapter.isTitle(position)) {
                    return mLayoutManager.getSpanCount();
                } else {
                    return 1;
                }
            }
        });

        return view;
    }

    //
    // Handlers
    //

    private void updateLayoutManagerSpan(@NonNull Configuration config) {
        if (mLayoutManager == null) {
            return;
        }

        // Guess the device type and orientation
        is_tablet = (config.smallestScreenWidthDp >= 600);
        is_landscape = (config.orientation == Configuration.ORIENTATION_LANDSCAPE);

        // Apply
        int span = 1;
        if (is_tablet)    span *= 2;
        if (is_landscape) span *= 2;
        mLayoutManager.setSpanCount(span);
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
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayoutManagerSpan(newConfig);
        updateListBottomMargin();
    }
}
