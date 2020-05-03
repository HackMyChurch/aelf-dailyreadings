package co.epitre.aelf_lectures.components;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

// https://stackoverflow.com/questions/39002684/tabs-dont-fit-to-screen-with-tabmode-scrollable-even-with-a-custom-tab-layout

/**
 * Custom tab Layout subclass to display at most 3 tabs on a screen
 */
public class AelfTabLayout extends TabLayout {
    public AelfTabLayout(Context context) {
        super(context);
    }

    public AelfTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AelfTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        ViewGroup tabLayout = (ViewGroup)getChildAt(0);
        int childCount = tabLayout.getChildCount();

        if( childCount != 0 ) {
            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            int tabMinWidth = displayMetrics.widthPixels/3;

            for(int i = 0; i < childCount; ++i){
                tabLayout.getChildAt(i).setMinimumWidth(tabMinWidth);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
