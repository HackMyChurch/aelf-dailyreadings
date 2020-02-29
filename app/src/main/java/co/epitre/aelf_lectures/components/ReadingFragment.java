package co.epitre.aelf_lectures.components;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

import co.epitre.aelf_lectures.LecturesActivity;
import co.epitre.aelf_lectures.PinchToZoomListener;
import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.settings.SettingsActivity;


public abstract class ReadingFragment extends Fragment {
    private static final String TAG = "ReadingFragment";

    /**
     * The fragment arguments
     */
    public static final String ARG_TEXT_HTML = "chapter_text";
    public static final String ARG_HIGHLIGHT = "highlight";

    /**
     * Views
     */
    protected WebView mWebView;
    private WebSettings mWebSettings;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Build UI
        View rootView = inflater.inflate(R.layout.fragment_lecture, container, false);
        mWebView = rootView.findViewById(R.id.LectureView);
        mWebView.clearCache(true);
        mWebSettings = mWebView.getSettings();
        mWebSettings.setBuiltInZoomControls(false);
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setDomStorageEnabled(true);

        // Install theme and styling hooks
        mWebView.setWebViewClient(new ReadingWebViewClient((LecturesActivity) getActivity(), mWebView));

        // Install Zoom support
        mWebView.setOnTouchListener(new ReadingFragment.ReadingPinchToZoomListener());

        // Accessibility: enable (best effort)
        try {
            mWebView.setAccessibilityDelegate(new View.AccessibilityDelegate());
        } catch (NoClassDefFoundError e) {
            Log.w(TAG, "Accessibility support is not available on this device");
        }

        // Load content
        loadText();

        return rootView;
    }

    protected class ReadingPinchToZoomListener extends PinchToZoomListener {
        private ViewGroup mParent;

        ReadingPinchToZoomListener() {
            super(ReadingFragment.this.getContext());
            mParent = (ViewGroup)mWebView.getParent();

            mParent.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    int width = right - left;
                    int oldWidth = oldRight - oldLeft;

                    // On width change
                    if (width != oldWidth) {
                        onZoomEnd(getCurrentZoomLevel());
                    }
                }
            });
        }

        private void setCurrentZoom(int zoom) {
            if (mWebSettings == null || mParent == null || !isAdded()) {
                return;
            }

            // Get the base width
            int base_width = (int)(450 * getResources().getDisplayMetrics().density);

            // Get current parent width
            int parent_width = mParent.getMeasuredWidth();
            if (parent_width == 0) {
                return; // Fast path: not yet visible
            }

            // Compute new width
            int current_width = mWebView.getMeasuredWidth();
            int new_width = base_width * zoom / 100;
            if (new_width > parent_width) {
                new_width = parent_width;
            }

            // Apply new width and zoom
            mWebSettings.setTextZoom(zoom);
            if (current_width != new_width) {
                ViewGroup.LayoutParams params = mWebView.getLayoutParams();
                params.width = new_width;
                mWebView.setLayoutParams(params);
            }
        }

        public int onZoomStart() {
            return super.onZoomStart();
        }

        public void onZoomEnd(int zoomLevel) {
            super.onZoomEnd(zoomLevel);
            setCurrentZoom(zoomLevel);
        }

        public void onZoom(int zoomLevel) {
            super.onZoom(zoomLevel);
            setCurrentZoom(zoomLevel);
        }
    }

    /**
     * Internal tools
     */

    protected String getThemeCss() {
        Activity activity = getActivity();
        if (activity == null) {
            return null;
        }

        boolean nightMode = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(SettingsActivity.KEY_PREF_DISP_NIGHT_MODE, false);
        String themeName = nightMode ? "dark":"light";
        return "css/theme-"+themeName+".css";
    }

    /**
     * Interface
     */

    abstract protected void loadText();
}
