package co.epitre.aelf_lectures;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
// WebView dependencies
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


/**
 * Created by jean-tiare on 12/03/18.
 */

public class SectionBibleFragment extends SectionFragmentBase {
    public static final String TAG = "SectionBibleFragment";
    public static final String BASE_RES_URL = "file:///android_asset/www/";

    public SectionBibleFragment(){
        // Required empty public constructor
    }

    /**
     * Global managers / resources
     */
    SharedPreferences settings = null;
    WebView mWebView;
    int activityRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Load settings
        settings = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Set Section title (Can be anywhere in the class !)
        actionBar.setTitle("Bible");

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_section_bible, container, false);
        mWebView = view.findViewById(R.id.webView);

        // Get webview settings settings
        final WebSettings webSettings = mWebView.getSettings();

        // Zoom support
        mWebView.setOnTouchListener(new PinchToZoomListener(getContext()) {
            public int onZoomStart() {
                return super.onZoomStart();
            }

            public void onZoomEnd(int zoomLevel) {
                super.onZoomEnd(zoomLevel);
                webSettings.setTextZoom(zoomLevel);
            }

            public void onZoom(int zoomLevel) {
                super.onZoom(zoomLevel);
                webSettings.setTextZoom(zoomLevel);
            }
        });

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

        // Enable Javascript
        webSettings.setJavaScriptEnabled(true);
        // Enable Dom Storage https://stackoverflow.com/questions/33079762/android-webview-uncaught-typeerror-cannot-read-property-getitem-of-null
        webSettings.setDomStorageEnabled(true);

        // Get intent link, if any
        Uri uri = activity.getIntent().getData();

        // Load webview
        if (uri != null) {
            // Parse link and open linked page
            onLink(uri);
        } else if (savedInstanceState != null) {
            // Restore state
            mWebView.restoreState(savedInstanceState);
        } else {
            // Load default page
            mWebView.loadUrl(BASE_RES_URL + "index.html");
        }

        //Save last URL
        // onPageFinished infos found on https://stackoverflow.com/a/6720004
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(mWebView, url);
                String last_url = mWebView.getUrl();
                Log.d(TAG, "Last page visited is " + last_url);
                //TODO: Save the value in a persistent storage, maybe sharedpreferences and use it when the webview is re-created.
            }
        });

        //TODO: Save scroll position and restore it.

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

    @Override
    public void onLink(Uri uri) {
        String path = uri.getPath();
        String host = uri.getHost();

        String parsedUrl = "index.html";

        if (host.equals("www.aelf.org")) {
            // AELF Website
            String[] chunks = path.split("/");

            if (chunks.length >= 2 && chunks[1].equals("bible")) {
                if (chunks.length == 2) {
                    // Bible home page
                    parsedUrl = "index.html";
                } else {
                    parsedUrl = TextUtils.join("/", Arrays.copyOfRange(chunks, 1, chunks.length));
                    parsedUrl += ".html";
                }
            }
        }

        // Load requested page
        mWebView.loadUrl(BASE_RES_URL + parsedUrl);
    }

    //
    // Option menu
    //

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflate the menu; this adds items to the action bar
        inflater.inflate(R.menu.toolbar_bible, menu);

        // Make the share image white
        Drawable normalDrawable = ContextCompat.getDrawable(activity, R.drawable.ic_share_black_24dp);
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(activity, R.color.white));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                return onShare();
        }
        return super.onOptionsItemSelected(item);
    }

    //
    // Events
    //

    public boolean onShare() {
        if (mWebView == null) {
            return false;
        }

        // Get current webview URL
        String webviewUrl = mWebView.getUrl();
        webviewUrl = webviewUrl.substring(BASE_RES_URL.length() - 1, webviewUrl.length()- ".html".length());
        if (webviewUrl.equals("/index")) {
            webviewUrl = "/bible";
        }

        // Get current webview title
        String webviewTitle = mWebView.getTitle();

        // Build share message
        String websiteUrl = "https://www.aelf.org" + webviewUrl;
        String message = webviewTitle + ": " + websiteUrl;
        String subject = webviewTitle;

        // Create the intent
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)));

        // All done !
        return true;
    }

    //
    // Lifecycle
    //

    @Override
    public void onResume() {
        super.onResume();

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        // Get current requested orientation, so that we can restore it
        activityRequestedOrientation = activity.getRequestedOrientation();

        // Disable landscape view, this is currently broken
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onPause() {
        super.onPause();

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        activity.setRequestedOrientation(activityRequestedOrientation);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mWebView != null) {
            mWebView.saveState(outState);
        }
    }

    // TODO : Fix shadow on "Autres Livres" dropdown menu not showing on real phone
    // TODO : Test Bible on tablet !
    // TODO : Link daily readings from mass and offices to Bible
    // TODO : Add search in Bible function...
    // TODO (later): support landscape orientation
}
