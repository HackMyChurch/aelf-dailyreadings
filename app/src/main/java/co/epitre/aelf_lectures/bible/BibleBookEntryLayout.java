package co.epitre.aelf_lectures.bible;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import co.epitre.aelf_lectures.R;

/**
 * Wrapper around LinearLayout cleanly abstracting the implementation details of the list entry view
 * for the purpose of the click listener and transitions.
 */
public class BibleBookEntryLayout extends LinearLayout {
    //
    // Constructors matching the parent class
    //

    public BibleBookEntryLayout(Context context) {
        super(context);
    }

    public BibleBookEntryLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BibleBookEntryLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BibleBookEntryLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    //
    // Custom methods
    //

    public LinearLayout getButtonView() {
        return findViewById(R.id.title_button);
    }

    public LinearLayout getTitleView() {
        return findViewById(R.id.title);
    }
}
