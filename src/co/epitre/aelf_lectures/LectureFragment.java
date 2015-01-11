package co.epitre.aelf_lectures;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    	htmlString.append("" +
    			"<html>" +
    				"<head>" +
    					"<style type=\"text/css\">" +
    					"body{" +
    					"	margin:24px;" +
    					"	background-color:#"+Integer.toHexString(getResources().getColor(R.color.sepia_bg)).substring(2)+";" +
    					"	color:#"+Integer.toHexString(getResources().getColor(R.color.sepia_fg)).substring(2)+";" +
    					"   font-family: 'Droid Sans', Courier, sans-serif;" +
    					"	font-size: 15px;" + // regular body
    					"	font-weight: regular;"+
    					"}" +
    					"h3 {" + // title
    					"	font-size: 20px;" +
    					"	font-weight: thin;" +
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
    					"small i{" + // reference
    					"	display: block;" +
    					"	text-align: right;" +
    					"   margin-top: -15px;" +
    					"	margin-right: 40px;" +
    					"   padding-top: 0;" +
    					"}" +
    					"font[color='#cc0000'] {" + // psaume refrain
    					"	color: #"+Integer.toHexString(getResources().getColor(R.color.aelf_red)).substring(2)+";" +
    					"} " +
    					"</style>" +
    				"</head>" +
    				"<body>");
    	htmlString.append(body);
    	htmlString.append("</body></html>");
    	;

    	// actual UI refresh
    	View rootView = inflater.inflate(R.layout.fragment_lecture, container, false);
    	lectureView = (WebView) rootView.findViewById(R.id.LectureView);
    	websettings = lectureView.getSettings();
    	websettings.setBuiltInZoomControls(false);
    	lectureView.loadDataWithBaseURL("file:///android_asset/", htmlString.toString(), "text/html", "utf-8", null);
    	lectureView.setBackgroundColor(0x00000000);
    	
    	// register listener
    	Context context = getActivity();
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
