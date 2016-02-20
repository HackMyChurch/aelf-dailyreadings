package co.epitre.aelf_lectures;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebSettings.TextSize;

/**
 * "Lecture" renderer
 */
public class LectureFragment extends Fragment implements OnSharedPreferenceChangeListener {
    /**
     * The fragment arguments
     */
    private static final String TAG = "LectureFragment";
    public static final String ARG_TEXT_HTML = "text html";
    protected WebView lectureView;
    protected WebSettings websettings;
    SharedPreferences preferences;

    public LectureFragment() {
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(SyncPrefActivity.KEY_PREF_DISP_FONT_SIZE)) {
            this.refresh();
        }
    }
    
    /* refresh zoom */
    public void refresh() {
        Context context = getActivity();
        if(context == null) {
            return; // we're a dead object
        }

        // load current zoom level
        Resources res = context.getResources();
        String pDefFontSize = res.getString(R.string.pref_font_size_def);
        String fontSize = preferences.getString(SyncPrefActivity.KEY_PREF_DISP_FONT_SIZE, pDefFontSize);

        // set font size
        switch (fontSize) {
            case "small":
                websettings.setTextSize(TextSize.SMALLER);
                break;
            case "big":
                websettings.setTextSize(TextSize.LARGER);
                break;
            case "huge":
                websettings.setTextSize(TextSize.LARGEST);
                break;
            default:
                websettings.setTextSize(TextSize.NORMAL);
        }
    }

    @SuppressLint("NewApi") // surrounded by a runtime test
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // compute view --> HTML
        StringBuilder htmlString = new StringBuilder();
        String body = getArguments().getString(ARG_TEXT_HTML);

        String col_red_hex = Integer.toHexString(getResources().getColor(R.color.aelf_red)).substring(2);
        String col_sepia_light = Integer.toHexString(getResources().getColor(R.color.sepia_bg)).substring(2);
        String col_sepia_dark = Integer.toHexString(getResources().getColor(R.color.sepia_fg)).substring(2);

        htmlString.append("" +
                "<html>" +
                    "<head>" +
                        "<style type=\"text/css\">" +
                        "body{" +
                        "	margin:24px;" +
                        "	background-color:#"+col_sepia_light+";" +
                        "	color:#"+col_sepia_dark+";" +
                        "   font-family: sans-serif;" +
                        "	font-size: 15px;" + // regular body
                        "	font-weight: regular;"+
                        "}" +
                        "h3 {" + // title
                        "	font-size: 20px;" +
                        "	font-weight: bold;" +
                        "}"+
                        "b i{" + // sub-title
                        "	font-size: 15px;" +
                        "	display: block;" +
                        "	margin-top: -12px;" +
                        "	margin-bottom: 20px;" +
                        "}" +
                        "blockquote {" +
                        "	margin-right: 20px" +
                        "}" +
                        "blockquote p {" +
                        "	margin-top: 30px;" +
                        "}" +
                        "h3 small i{" + // global reference
                        "	display: block;" +
                        "	float: right;" +
                        "   font-weight: normal;" +
                        "	margin-top: 5px;" +
                        "}" +
                        "blockquote small i{" + // citation reference
                        "	display: block;" +
                        "	text-align: right;" +
                        "   margin-top: -15px;" +
                        "	margin-right: 0;" +
                        "   padding-top: 0;" +
                        "}" +
                        "font[color='#cc0000'], font[color='#ff0000'], font[color='#CC0000'], font[color='#FF0000'] {" + // psaume refrain
                        "	color: #"+col_red_hex+";" +
                        "} " +
                        ".verse {" + // psaume verse number
                        "	display: block;" +
                        "   float: left;" +
                        "   width: 25px;" +
                        "   text-align: right;" +
                        "   margin-top: 4px;" +
                        "   margin-left: -30px;" +
                        "	font-size: 10px;" +
                        "	color: #"+col_red_hex+";" +
                        "}" +
                        "sup {" + // inflections: do not affect line-height
                        "   vertical-align: baseline;" +
                        "   position: relative;" +
                        "   top: -0.4em;" +
                        "}" +
                        ".underline {" +
                        "    text-decoration: underline;" +
                        "}" +
                        "</style>" +
                    "</head>" +
                    "<body>");
        htmlString.append(body);
        htmlString.append("</body></html>");

        String reading = htmlString.toString();

        // actual UI refresh
        Context context = getActivity();
        View rootView = inflater.inflate(R.layout.fragment_lecture, container, false);
        lectureView = (WebView) rootView.findViewById(R.id.LectureView);
        websettings = lectureView.getSettings();
        websettings.setBuiltInZoomControls(false);

        // accessibility: enable (best effort)
        websettings.setJavaScriptEnabled(true);
        try {
            lectureView.setAccessibilityDelegate(new View.AccessibilityDelegate());
        } catch (java.lang.NoClassDefFoundError e) {
            Log.w(TAG, "Accessibility support is not available on this device");
        }

        //accessibility: drop the underline attributes, they break the screen readers
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(context.ACCESSIBILITY_SERVICE);
        if(am.isEnabled()) {
            reading = reading.replaceAll("</?u>", "");
        }

        // load content
        lectureView.loadDataWithBaseURL("file:///android_asset/", reading, "text/html", "utf-8", null);
        lectureView.setBackgroundColor(0x00000000);

        // register listener
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);

        // font size
        this.refresh();

        if(android.os.Build.VERSION.SDK_INT > 11)
        {
            lectureView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        }

        return rootView;
    }
}
