package co.epitre.aelf_lectures.bible;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class BibleMenuPagerAdapter extends FragmentPagerAdapter {
    private BibleBookList mBibleBookList;

    BibleMenuPagerAdapter(@NonNull FragmentManager fm, @NonNull BibleBookList bibleBookList) {
        super(fm);
        this.mBibleBookList = bibleBookList;
    }

    @Override
    public int getCount() {
        return mBibleBookList.getParts().size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mBibleBookList.getParts().get(position).getName();
    }

    public String getRoute(int position) {
        return mBibleBookList.getParts().get(position).getRoute();
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return BibleMenuBookListFragment.newInstance(position);
    }
}
