package co.epitre.aelf_lectures.lectures;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import androidx.preference.PreferenceManager;

import co.epitre.aelf_lectures.components.ReadingFragment;
import co.epitre.aelf_lectures.lectures.data.office.Lecture;
import co.epitre.aelf_lectures.lectures.data.office.LectureVariants;
import co.epitre.aelf_lectures.settings.SettingsActivity;

/**
 * "Lecture" renderer
 */
public class LectureFragment extends ReadingFragment implements
        OnSharedPreferenceChangeListener {
    private static final String TAG = "LectureFragment";

    /**
     * The fragment arguments
     */
    public static final String ARG_POSITION = "position";
    public static final String ARG_VARIANT = "variant";

    private SharedPreferences preferences;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) {
            return;
        }

        if (key.equals(SettingsActivity.KEY_PREF_DISP_PSALM_UNDERLINE)) {
            loadText();
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Register preference listener
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        preferences.registerOnSharedPreferenceChangeListener(this);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected void loadText() {
        Context context = getActivity();
        Bundle args = getArguments();
        if (context == null || mWebView == null || args == null) {
            return;
        }

        // Get the lecture
        int position = args.getInt(ARG_POSITION);
        int variant = args.getInt(ARG_VARIANT);

        SectionLecturesFragment parent = (SectionLecturesFragment)getParentFragment();
        if (parent == null) {
            return;
        }

        LectureVariants lectureVariants = parent.getLectureVariants(position);
        if (lectureVariants == null) {
            return;
        }

        Lecture lecture = lectureVariants.get(variant);

        // Build HTML
        StringBuilder htmlString = new StringBuilder();
        htmlString.append("<!DOCTYPE html><html><head>");
        htmlString.append("<link href=\"css/common.css\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<link href=\"");
        htmlString.append(getThemeCss());
        htmlString.append("\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("</head>");
        htmlString.append("<body>");
        htmlString.append(lecture.toHtml());
        htmlString.append("<script src=\"js/lecture.js\" charset=\"utf-8\"></script>\n");
        htmlString.append("</body></html>");

        String reading = htmlString.toString();

        // accessibility: drop the underline attributes && line wrapper fixes, they break the screen readers
        String underlineMode = preferences.getString(SettingsActivity.KEY_PREF_DISP_PSALM_UNDERLINE, "auto");

        boolean underline = underlineMode.equals("always");
        if (underlineMode.equals("auto")) {
            AccessibilityManager am = (AccessibilityManager) getActivity().getSystemService(Context.ACCESSIBILITY_SERVICE);
            underline = !am.isEnabled();
        }

        if(!underline) {
            reading = reading.replaceAll("</?u>", "");
        }

        // load content
        mWebView.loadDataWithBaseURL("file:///android_asset/", reading, "text/html", "utf-8", null);
        mWebView.setBackgroundColor(0x00000000);
    }
}
