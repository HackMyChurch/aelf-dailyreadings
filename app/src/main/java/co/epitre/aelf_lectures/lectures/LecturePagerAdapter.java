package co.epitre.aelf_lectures.lectures;

import co.epitre.aelf_lectures.lectures.data.Lecture;
import co.epitre.aelf_lectures.lectures.data.LectureVariants;
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
public class LecturePagerAdapter extends FragmentStatePagerAdapter {
    public static final String TAG = "LecturePagerAdapter";

    private Office mOffice;

    public LecturePagerAdapter(FragmentManager fm, Office office) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mOffice = office;
    }

    @Override
    public Fragment getItem(int position) {
        Bundle args = new Bundle();
        args.putInt(LectureFragment.ARG_POSITION, position);

        Fragment fragment = new LectureFragment();
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

    public LectureVariants getLectureVariants(int position) {
        if(position < this.getCount()) {
            return mOffice.getLectures().get(position);
        }
        return null;
    }

    public Lecture getLecture(int position) {
        return getLectureVariants(position).get(0);
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