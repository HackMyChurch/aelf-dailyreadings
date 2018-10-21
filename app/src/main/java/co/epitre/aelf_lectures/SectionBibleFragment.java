package co.epitre.aelf_lectures;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// WebView dependencies
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jean-tiare on 12/03/18.
 */

public class SectionBibleFragment extends SectionFragmentBase {
    public static final String TAG = "SectionBibleFragment";

    public SectionBibleFragment(){
        // Required empty public constructor
    }

    /**
     * Global managers / resources
     */
    SharedPreferences settings = null;

    WebView mWebView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Load settings
        settings = PreferenceManager.getDefaultSharedPreferences(getContext());

        Uri uri = activity.getIntent().getData();
        if (uri != null) {
            // Do something like loading a specific reference ?
        }

        // Set Section title (Can be anywhere in the class !)
        actionBar.setTitle("Bible");

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_section_bible, container, false);
        mWebView = view.findViewById(R.id.webView);

        // Dark theme support
        final boolean nightMode = settings.getBoolean(SyncPrefActivity.KEY_PREF_DISP_NIGHT_MODE, false);

        // Force links and redirects to open in the WebView instead of in a browser
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url.equals("file:///android_asset/www/virtual/application.css")) {
                    // Load the CSS file corresponding to the selected theme
                    String themeName = nightMode ? "dark":"light";
                    try {
                        InputStream styleStream = getActivity().getAssets().open("www/assets/application_"+themeName+".css");
                        return new WebResourceResponse("text/css", "UTF-8", styleStream);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to load "+themeName+" theme", e);
                        return null;
                    }
                }

                return super.shouldInterceptRequest(view, url);
            }
        });
        // Clear the cache on theme change so that we can inject our own CSS
        mWebView.clearCache(true);

        // Tweak settings
        WebSettings webSettings = mWebView.getSettings();

        // Enable Javascript
        webSettings.setJavaScriptEnabled(true);
        // Enable Dom Storage https://stackoverflow.com/questions/33079762/android-webview-uncaught-typeerror-cannot-read-property-getitem-of-null
        webSettings.setDomStorageEnabled(true);

        // Use local resource
        mWebView.loadUrl("file:///android_asset/www/index.html");
        return view;
    }

    /**
     * Back pressed send from activity.
     *
     * @return if event is consumed, it will return true.
     * https://www.skoumal.net/en/android-handle-back-press-in-fragment/
     */
    @Override
    public boolean onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        } else {
            return false;
        }
    }


    // TODO : Fix shadow on "Autres Livres" dropdown menu not showing on real phone
    // TODO : Add support for pinch to zoom in Bible (or at least buttons zooming)
    // TODO : Test Bible on tablet !
    // TODO : Link daily readings from mass and offices to Bible
    // TODO : Intent filter for opening bible link in app...
    // TODO : Add search in Bible function...
}
