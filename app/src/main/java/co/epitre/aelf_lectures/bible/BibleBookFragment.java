package co.epitre.aelf_lectures.bible;

import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.PopupMenu;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import co.epitre.aelf_lectures.LecturesActivity;
import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.bible.data.BibleBookChapter;
import co.epitre.aelf_lectures.bible.data.BibleBookEntry;
import co.epitre.aelf_lectures.bible.data.BibleBookList;
import co.epitre.aelf_lectures.bible.data.BiblePart;

public class BibleBookFragment extends BibleFragment {
    /**
     * Internal
     */

    private final static String BIBLE_PART_ID = "biblePartId";
    private final static String BIBLE_BOOK_ID = "bibleBookId";
    private final static String BIBLE_CHAPTER_ID = "bibleChapterId";
    private final static String BIBLE_SEARCH_QUERY = "bibleSearchQuery";
    private final static String BIBLE_REFERENCE = "bibleReference";

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

    // FIXME: this is *very* ineficient
    public static BibleBookFragment newInstance(Uri uri) {
        String path = uri.getPath();
        String[] chunks = path.split("/");
        String query = uri.getQueryParameter("query");
        String reference = uri.getQueryParameter("reference");

        int biblePartId = 0;
        int bibleBookId = 0;
        int bibleChapterId = 0;
        BibleBookEntry bibleBookEntry = null;

        // Locate book
        if (chunks.length > 2) {
            String bookRef = chunks[2];

            BibleBookList bibleBookList = BibleBookList.getInstance();
            search:
            for (BiblePart candidateBiblePart: bibleBookList.getParts()) {
                bibleBookId = 0;
                for (BibleBookEntry candidateBibleBookEntry: candidateBiblePart.getBibleBookEntries()) {
                    String candidateBookRef = candidateBibleBookEntry.getBookRef();

                    if (candidateBookRef != null && candidateBookRef.equals(bookRef)) {
                        bibleBookEntry = candidateBibleBookEntry;
                        break search;
                    }
                    bibleBookId++;
                }
                biblePartId++;
            }

            // Not found
            if (bibleBookEntry == null) {
                biblePartId = 0;
                bibleBookId = 0;
            }
        }

        // Locate chapter
        if (chunks.length > 3 && bibleBookEntry != null) {
            String chapterRef = chunks[3];
            boolean found = false;

            for (BibleBookChapter bibleBookChapter: bibleBookEntry.getChapters()) {
                if (bibleBookChapter.getChapterRef().equals(chapterRef)) {
                    found = true;
                    break;
                }
                bibleChapterId++;
            }

            // Not found
            if (!found) {
                bibleChapterId = 0;
            }
        }

        return newInstance(biblePartId, bibleBookId, bibleChapterId, query, reference);
    }

    public static BibleBookFragment newInstance(int biblePartId, int bibleBookId) {
        return newInstance(biblePartId, bibleBookId, -1);
    }

    public static BibleBookFragment newInstance(int biblePartId, int bibleBookId, int bibleChapterId) {
        return newInstance(biblePartId, bibleBookId, bibleChapterId, null, null);
    }

    public static BibleBookFragment newInstance(int biblePartId, int bibleBookId, int bibleChapterId, String query, String reference) {
        BibleBookFragment fragment = new BibleBookFragment();

        Bundle args = new Bundle();
        args.putInt(BIBLE_PART_ID, biblePartId);
        args.putInt(BIBLE_BOOK_ID, bibleBookId);
        args.putInt(BIBLE_CHAPTER_ID, bibleChapterId);
        args.putString(BIBLE_SEARCH_QUERY, query);
        args.putString(BIBLE_REFERENCE, reference);
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
        int bibleChapterId = getArguments().getInt(BIBLE_CHAPTER_ID, -1);
        String query = getArguments().getString(BIBLE_SEARCH_QUERY);
        String reference = getArguments().getString(BIBLE_REFERENCE);
        BiblePart biblePart = BibleBookList.getInstance().getParts().get(biblePartId);
        mBibleBookEntry = biblePart.getBibleBookEntries().get(bibleBookId);

        // Get the first chapter
        if (bibleChapterId < 0) {
            bibleChapterId = mBibleBookEntry.getChapterRefPosition();
        }

        // Set section title
        actionBar.setTitle(mBibleBookEntry.getBookName());

        // Setup the pager
        mBibleChapterPagerAdapter = new BibleChapterPagerAdapter(getChildFragmentManager(), mBibleBookEntry, bibleChapterId, query, reference);
        mViewPager = view.findViewById(R.id.bible_chapter_pager);
        mViewPager.setAdapter(mBibleChapterPagerAdapter);
        mTabLayout = view.findViewById(R.id.bible_chapter_layout);
        mTabLayout.setTabIndicatorFullWidth(true);

        // Setup the chapter selection menu
        mTabLayout.addOnTabSelectedListener(new ChapterSelectionListener());

        // Select requested chapter
        mViewPager.setCurrentItem(bibleChapterId);

        // Populate the tabs
        mTabLayout.setupWithViewPager(mViewPager);

        return view;
    }

    @Override
    public String getRoute() {
        if (mViewPager == null) {
            return null;
        }

        String separator = "?";

        int position = mViewPager.getCurrentItem();
        String route = "/bible"+mBibleChapterPagerAdapter.getRoute(position);

        if (position == getArguments().getInt(BIBLE_CHAPTER_ID, -1)) {
            route += separator+"query=" + getArguments().getString(BIBLE_SEARCH_QUERY);
            separator = "&";
        }

        String reference = getArguments().getString(BIBLE_SEARCH_QUERY);
        if (reference != null) {
            route += separator+"reference=" + reference;
            separator = "&";
        }

        return route;
    }

    @Override
    public String getTitle() {
        if (mViewPager == null || mBibleBookEntry == null || mBibleChapterPagerAdapter == null) {
            return null;
        }

        int position = mViewPager.getCurrentItem();
        String BookTitle = mBibleBookEntry.getBookName();
        String ChapterTitle = mBibleChapterPagerAdapter.getPageTitle(position).toString();

        return BookTitle + " — " + ChapterTitle;
    }

    private void showChapterSelectionMenu(View menuAnchor) {
        if (menuAnchor == null || mTabLayout == null) {
            return;
        }

        // Build menu
        final ListPopupWindow listPopupWindow = new ListPopupWindow(getContext());
        listPopupWindow.setAnchorView(menuAnchor);
        listPopupWindow.setDropDownGravity(Gravity.CENTER);
        listPopupWindow.setHeight(ListPopupWindow.WRAP_CONTENT);
        listPopupWindow.setWidth(menuAnchor.getWidth());
        listPopupWindow.setAdapter(new ArrayAdapter(getContext(),
                android.R.layout.simple_list_item_1, mBibleBookEntry.getBook().getChapters()));

        // Handle events
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listPopupWindow.dismiss();
                mViewPager.setCurrentItem(position);
            }
        });

        // Display menu
        listPopupWindow.show();
    }

    private class ChapterSelectionListener implements TabLayout.OnTabSelectedListener {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            tab.setIcon(R.drawable.ic_drop_down);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            tab.setIcon(null);
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            showChapterSelectionMenu(tab.view);
        }
    }

    //
    // Lifecycle
    //

    @Override
    public void onStart() {
        super.onStart();

        // Enable "home" button to get back to the menu
        if (activity != null) {
            activity.setHomeButtonEnabled(true, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (activity != null) {
                        activity.onBackPressed();
                    }
                }
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // Reset the home button state
        if (activity != null) {
            activity.setHomeButtonEnabled(false, null);
        }
    }
}
