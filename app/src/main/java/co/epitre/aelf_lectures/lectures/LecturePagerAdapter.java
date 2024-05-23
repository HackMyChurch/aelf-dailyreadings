package co.epitre.aelf_lectures.lectures;

import android.os.Bundle;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.List;

import co.epitre.aelf_lectures.lectures.data.office.Lecture;
import co.epitre.aelf_lectures.lectures.data.office.LectureVariants;
import co.epitre.aelf_lectures.lectures.data.office.Office;

/**
 * Adapter, return a fragment for each lecture / slide.
 */
public class LecturePagerAdapter extends FragmentStatePagerAdapter {
    public static final String TAG = "LecturePagerAdapter";

    private Office mOffice;
    private SparseIntArray mVariantIds = new SparseIntArray(10);

    public LecturePagerAdapter(FragmentManager fm, Office office) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mOffice = office;
    }

    @Override
    @NonNull
    public Fragment getItem(int position) {
        Bundle args = new Bundle();
        args.putInt(LectureFragment.ARG_POSITION, position);
        args.putInt(LectureFragment.ARG_VARIANT, mVariantIds.get(position, 0));

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

    public boolean hasVariants(int position) {
        LectureVariants lectureVariants = getLectureVariants(position);
        return  (lectureVariants != null && lectureVariants.hasVariants());
    }

    public List<String> getVariantTitles(int position) {
        LectureVariants lectureVariants = getLectureVariants(position);
        if (lectureVariants == null) {
            return null;
        }
        return lectureVariants.getVariantTitles();
    }

    public Lecture getLecture(int position) {
        LectureVariants lectureVariants = getLectureVariants(position);
        if (lectureVariants == null) {
            return null;
        }
        return lectureVariants.get(mVariantIds.get(position, 0));
    }

    public void setLectureVariantId(int position, int variant) {
        if(position < this.getCount()) {
            mVariantIds.put(position, variant);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        LectureFragment lectureFragment = (LectureFragment)object;
        Bundle args = lectureFragment.getArguments();
        if (args == null) {
            return POSITION_UNCHANGED;
        }

        // Get current status
        int position = args.getInt(LectureFragment.ARG_POSITION);
        int currentVariantId = args.getInt(LectureFragment.ARG_VARIANT);
        int newVariantId = mVariantIds.get(position, 0);

        // If the position changed, reload
        if (currentVariantId != newVariantId) {
            // Update variant
            args.putInt(LectureFragment.ARG_VARIANT, newVariantId);
            lectureFragment.loadText();
        }
        return POSITION_UNCHANGED;
    }
}