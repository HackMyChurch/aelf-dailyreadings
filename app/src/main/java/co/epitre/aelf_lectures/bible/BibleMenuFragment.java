package co.epitre.aelf_lectures.bible;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.ActionBar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import co.epitre.aelf_lectures.LecturesActivity;
import co.epitre.aelf_lectures.R;

public class BibleMenuFragment extends BibleFragment {

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
        View view = inflater.inflate(R.layout.fragment_section_bible_menu, container, false);

        // Set section title
        actionBar.setTitle("Bible de la liturgie");

        // Setup the pager
        mBibleMenuPagerAdapter = new BibleMenuPagerAdapter(getChildFragmentManager(), BibleBookList.getInstance());
        mViewPager = view.findViewById(R.id.bible_menu_pager);
        mViewPager.setAdapter(mBibleMenuPagerAdapter);
        mTabLayout = view.findViewById(R.id.bible_menu_layout);
        mTabLayout.setupWithViewPager(mViewPager);

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
        return mBibleMenuPagerAdapter.getRoute(position);
    }

    @Override
    public String getTitle() {
        // Make sure we have a view pager
        if (mViewPager ==  null || mBibleMenuPagerAdapter == null) {
            return null;
        }

        int position = mViewPager.getCurrentItem();
        String partTitle = mBibleMenuPagerAdapter.getPageTitle(position).toString();
        return "Bible de la liturgie — " + partTitle;
    }
}
