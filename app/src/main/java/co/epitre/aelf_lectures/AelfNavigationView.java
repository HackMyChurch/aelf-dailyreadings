package co.epitre.aelf_lectures;

import android.content.Context;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.WindowInsetsCompat;
import android.util.AttributeSet;

/**
 * Created by jean-tiare on 03/12/17.
 */

public class AelfNavigationView extends NavigationView {
    public AelfNavigationView(Context context) {
        super(context);
    }

    public AelfNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AelfNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInsetsChanged(WindowInsetsCompat insets) {
        // HACK: do nothing to prevent original navigation view from display a grey area in place
        // of the navigation and status bars when using FLAG_TRANSLUCENT_NAVIGATION
    }
}
