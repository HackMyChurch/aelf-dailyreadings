package co.epitre.aelf_lectures.bible;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import co.epitre.aelf_lectures.LecturesActivity;
import co.epitre.aelf_lectures.R;

public class BibleBookFragment extends Fragment {
    /**
     * Internal
     */

    private final static String BIBLE_PART_ID = "biblePartId";
    private final static String BIBLE_BOOK_ID = "bibleBookId";

    /**
     * Global Views
     */
    protected ActionBar actionBar;
    protected NavigationView drawerView;
    protected LecturesActivity activity;
    protected Menu mMenu;

    /**
     * Pager
     */
    private BibleBookEntry mBibleBookEntry;
    protected BibleChapterPagerAdapter mBibleChapterPagerAdapter;
    protected ViewPager mViewPager;
    protected TabLayout mTabLayout;

    public static BibleBookFragment newInstance(int biblePartId, int bibleBookId) {
        BibleBookFragment fragment = new BibleBookFragment();

        Bundle args = new Bundle();
        args.putInt(BIBLE_PART_ID, biblePartId);
        args.putInt(BIBLE_BOOK_ID, bibleBookId);
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
        View view = inflater.inflate(R.layout.fragment_section_bible_book, container, false);

        // Load the Bible part book list
        int biblePartId = getArguments().getInt(BIBLE_PART_ID, 0);
        int bibleBookId = getArguments().getInt(BIBLE_BOOK_ID, 0);
        BiblePart biblePart = BibleBookList.getInstance().getParts().get(biblePartId);
        mBibleBookEntry = biblePart.getBibleBookEntries().get(bibleBookId);

        // Set section title
        actionBar.setTitle(mBibleBookEntry.getBookName());

        // Setup the pager
        mBibleChapterPagerAdapter = new BibleChapterPagerAdapter(getChildFragmentManager(), mBibleBookEntry);
        mViewPager = view.findViewById(R.id.bible_chapter_pager);
        mViewPager.setAdapter(mBibleChapterPagerAdapter);
        mTabLayout = view.findViewById(R.id.bible_chapter_layout);
        mTabLayout.setupWithViewPager(mViewPager);

        // Set tab mode based on chapter count
        if (mBibleChapterPagerAdapter.getCount() <= 3) {
            mTabLayout.setTabMode(TabLayout.MODE_FIXED);
            mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        }

        // Select requested chapter
        mViewPager.setCurrentItem(mBibleBookEntry.getChapterRefPosition());

        return view;
    }
}
