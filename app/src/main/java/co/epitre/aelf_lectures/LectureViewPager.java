package co.epitre.aelf_lectures;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

/**
 * Created by jean-tiare on 26/08/17.
 */

public class LectureViewPager extends ViewPager {
    public LectureViewPager(Context context) {
        super(context);
    }

    public LectureViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Do not restore any state/cache beyond what we explicitely control as an attempt to fix
    // spurious display of psalms in "hymnes" for instance on restore days after.
    // https://stackoverflow.com/questions/15519214/prevent-fragment-recovery-in-android
    // super.onCreate(createBundleNoFragmentRestore(savedInstanceState));
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(null);
    }
}
