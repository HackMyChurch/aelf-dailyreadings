package co.epitre.aelf_lectures;

import co.epitre.aelf_lectures.lectures.data.Lecture;
import co.epitre.aelf_lectures.lectures.data.Office;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

/**
 * Adapter, return a fragment for each lecture / slide.
 */
class LecturePagerAdapter extends FragmentStatePagerAdapter {
    public static final String TAG = "LecturePagerAdapter";

    private Office mOffice;

    LecturePagerAdapter(FragmentManager fm, Office office) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mOffice = office;
    }

    @Override
    public Fragment getItem(int position) {
        Lecture lecture = this.getLecture(position);
        Fragment fragment = new LectureFragment();

        Bundle args = new Bundle();
        args.putString(LectureFragment.ARG_TEXT_HTML, lecture.toHtml());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getCount() {
        return mOffice.getLectures().size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if(position < this.getCount()) {
            return this.getLecture(position).getShortTitle();
        }
        return null;
    }

    public Lecture getLecture(int position) {
        if(position < this.getCount()) {
            return mOffice.getLectures().get(position).get(0);
        }
        return null;
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        // https://stackoverflow.com/questions/41650721/attempt-to-invoke-virtual-method-android-os-handler-android-support-v4-app-frag
        try{
            super.finishUpdate(container);
        } catch (NullPointerException nullPointerException){
            Log.w(TAG, "Catch the NullPointerException in FragmentPagerAdapter.finishUpdate");
        }
    }
}