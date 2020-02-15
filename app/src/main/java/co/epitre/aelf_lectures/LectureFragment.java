package co.epitre.aelf_lectures;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import co.epitre.aelf_lectures.components.ReadingWebViewClient;
import co.epitre.aelf_lectures.settings.SettingsActivity;

/**
 * "Lecture" renderer
 */
public class LectureFragment extends Fragment implements
        NetworkStatusMonitor.NetworkStatusChangedListener,
        OnSharedPreferenceChangeListener {
    /**
     * The fragment arguments
     */
    private static final String TAG = "LectureFragment";
    public static final String ARG_TEXT_HTML = "text html";

    /**
     * Views
     */
    protected ViewGroup parent;
    protected WebView lectureView;
    protected WebSettings websettings;

    SharedPreferences preferences;
    NetworkStatusMonitor networkStatusMonitor = NetworkStatusMonitor.getInstance();

    /**
     * Swipe refresh / zoom status
     */
    private boolean isZooming = false;
    private boolean hasNetwork = false;
    private int base_width = 0;

    @Override
    public void onNetworkStatusChanged(NetworkStatusMonitor.NetworkStatusEvent networkStatusEvent) {
        switch (networkStatusEvent) {
            case NETWORK_OFF:
                hasNetwork = false;
                break;
            case NETWORK_ON:
                hasNetwork = true;
                break;
        }
    }

    public LectureFragment() {
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(SettingsActivity.KEY_PREF_DISP_FONT_SIZE)) {
            this.refresh();
        } else if (key.equals(SettingsActivity.KEY_PREF_DISP_PSALM_UNDERLINE)) {
            loadText();
        }
    }
    
    /* refresh zoom */
    public void refresh() {
        Context context = getActivity();
        if(context == null) {
            return; // we're a dead object
        }

        // load current zoom level
        int zoom = preferences.getInt(SettingsActivity.KEY_PREF_DISP_FONT_SIZE, 100);
        setCurrentZoom(zoom);
    }

    // Helper: set zoom as percent, even on older phones
    protected void setCurrentZoom(int zoom) {
        if (websettings == null || parent == null) {
            return;
        }

        // Compute new width
        int parent_width = parent.getMeasuredWidth();
        int current_width = lectureView.getMeasuredWidth();
        int new_width = base_width * zoom / 100;
        if (new_width > parent_width) {
            new_width = parent_width;
        }

        // Apply new width and zoom
        websettings.setTextZoom(zoom);
        if (current_width != new_width) {
            ViewGroup.LayoutParams params = lectureView.getLayoutParams();
            params.width = new_width;
            lectureView.setLayoutParams(params);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = getActivity();

        // actual UI refresh
        View rootView = inflater.inflate(R.layout.fragment_lecture, container, false);
        lectureView = rootView.findViewById(R.id.LectureView);
        websettings = lectureView.getSettings();
        websettings.setBuiltInZoomControls(false);

        // get base width
        base_width = (int)(450 * getResources().getDisplayMetrics().density);

        // Capture links
        lectureView.setWebViewClient(new ReadingWebViewClient((LecturesActivity) getActivity(), lectureView));

        // register preference listener
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);

        // accessibility: enable (best effort)
        websettings.setJavaScriptEnabled(true);
        try {
            lectureView.setAccessibilityDelegate(new View.AccessibilityDelegate());
        } catch (NoClassDefFoundError e) {
            Log.w(TAG, "Accessibility support is not available on this device");
        }

        // load content
        this.loadText();

        // Refresh the zoom on layout change (screen split, rotation, ...)
        parent = (ViewGroup)lectureView.getParent();
        parent.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int width = right - left;
                int oldWidth = oldRight - oldLeft;

                // On width change
                if (width != oldWidth) {
                    refresh();
                }
            }
        });

        lectureView.setOnTouchListener(new PinchToZoomListener(context) {
            public int onZoomStart() {
                isZooming = true;
                return super.onZoomStart();
            }

            public void onZoomEnd(int zoomLevel) {
                super.onZoomEnd(zoomLevel);
                isZooming = false;
                setCurrentZoom(zoomLevel);
            }

            public void onZoom(int zoomLevel) {
                super.onZoom(zoomLevel);
                setCurrentZoom(zoomLevel);
            }
        });

        return rootView;
    }

    private void loadText() {
        Context context = getActivity();
        if (context == null) {
            return;
        }

        // compute view --> HTML
        StringBuilder htmlString = new StringBuilder();
        String body = getArguments().getString(ARG_TEXT_HTML);

        htmlString.append("<!DOCTYPE html><html><head>");
        htmlString.append("<link href=\"css/common.css\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<link href=\"css/theme.css\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("</head>");
        htmlString.append("<body>");
        htmlString.append(body);
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
        lectureView.loadDataWithBaseURL("file:///android_asset/", reading, "text/html", "utf-8", null);
        lectureView.setBackgroundColor(0x00000000);
    }

    @Override
    public void onResume() {
        super.onResume();
        networkStatusMonitor.registerNetworkStatusChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        networkStatusMonitor.unregisterNetworkStatusChangeListener(this);
    }
}
