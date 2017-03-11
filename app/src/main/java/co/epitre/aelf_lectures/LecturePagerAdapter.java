package co.epitre.aelf_lectures;

import java.util.List;

import co.epitre.aelf_lectures.data.LectureItem;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Adapter, return a fragment for each lecture / slide.
 */
class LecturePagerAdapter extends FragmentStatePagerAdapter {

    private List<LectureItem> mlectures;

    LecturePagerAdapter(FragmentManager fm, List<LectureItem> lectures) {
        super(fm);
        mlectures = lectures;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        LectureItem lecture = mlectures.get(position);
        Fragment fragment = new LectureFragment();

        Bundle args = new Bundle();
        args.putString(LectureFragment.ARG_TEXT_HTML, lecture.description);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getCount() {
        return mlectures.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if(position < this.getCount()) {
            return mlectures.get(position).shortTitle;
        }
        return null;
    }
}