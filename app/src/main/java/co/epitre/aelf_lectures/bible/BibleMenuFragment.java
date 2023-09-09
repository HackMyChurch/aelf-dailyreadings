package co.epitre.aelf_lectures.bible;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

import co.epitre.aelf_lectures.LecturesActivity;
import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.bible.data.BibleBookList;
import co.epitre.aelf_lectures.bible.data.BiblePart;

public class BibleMenuFragment extends BibleFragment {
    /**
     * Internal
     */

    private final static String BIBLE_PART_ID = "biblePartId";

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
    protected BibleMenuPagerAdapter mBibleMenuPagerAdapter;
    protected ViewPager mViewPager;
    protected TabLayout mTabLayout;

    public static BibleMenuFragment newInstance(Uri uri) {
        String fragment = uri.getFragment();

        int biblePartId = 0;

        // Locate section
        boolean found = false;
        BibleBookList bibleBookList = BibleBookList.getInstance();
        for (BiblePart candidateBiblePart: bibleBookList.getParts()) {
            if (candidateBiblePart.getPartRef().equals(fragment)) {
                found = true;
                break;
            }
            biblePartId++;
        }

        if (!found) {
            biblePartId = 0;
        }

        return newInstance(biblePartId);
    }

    public static BibleMenuFragment newInstance(int biblePartId) {
        BibleMenuFragment fragment = new BibleMenuFragment();

        Bundle args = new Bundle();
        args.putInt(BIBLE_PART_ID, biblePartId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Load global views
        activity = (LecturesActivity) requireActivity();
        actionBar = activity.getSupportActionBar();
        drawerView = activity.findViewById(R.id.drawer_navigation_view);

        // Option menu
        setHasOptionsMenu(true);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_section_bible_menu, container, false);

        // Load the Bible part book list
        Bundle args = getArguments();
        int biblePartId = 0;
        if (args != null) {
            biblePartId = args.getInt(BIBLE_PART_ID, 0);
        }

        // Set section title
        actionBar.setTitle("Bible de la liturgie");

        // Setup the pager
        mBibleMenuPagerAdapter = new BibleMenuPagerAdapter(getChildFragmentManager(), BibleBookList.getInstance());
        mViewPager = view.findViewById(R.id.bible_menu_pager);
        mViewPager.setAdapter(mBibleMenuPagerAdapter);
        mTabLayout = view.findViewById(R.id.bible_menu_layout);
        mTabLayout.setupWithViewPager(mViewPager);

        // Select requested chapter
        mViewPager.setCurrentItem(biblePartId);

        return view;
    }

    @Override
    public String getRoute() {
        // Make sure we have a view pager
        if (mViewPager ==  null || mBibleMenuPagerAdapter == null) {
            return null;
        }

        // Return the route
        int position = mViewPager.getCurrentItem();
        return "/bible"+mBibleMenuPagerAdapter.getRoute(position);
    }

    @Override
    public String getTitle() {
        // Make sure we have a view pager
        if (mViewPager ==  null || mBibleMenuPagerAdapter == null) {
            return null;
        }

        int position = mViewPager.getCurrentItem();
        String partTitle = Objects.requireNonNull(mBibleMenuPagerAdapter.getPageTitle(position)).toString();
        return "Bible de la liturgie — " + partTitle;
    }
}
