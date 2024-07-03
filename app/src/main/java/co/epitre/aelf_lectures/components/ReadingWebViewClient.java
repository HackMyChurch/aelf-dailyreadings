package co.epitre.aelf_lectures.components;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import co.epitre.aelf_lectures.LecturesActivity;

public class ReadingWebViewClient extends WebViewClient {
    private static final String TAG = "ReadingWebViewClient";

    private WebView mWebView;
    private LecturesActivity mActivity;

    public ReadingWebViewClient(@NonNull LecturesActivity activity, @NonNull WebView webView) {
        mWebView = webView;
        mActivity = activity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Log.w(TAG, "Got a URL: "+url);

        // Prepare URL
        url = url.replace("file:///android_asset/", "");
        if (url.startsWith("http%C2%A0:%20")) {
            url = "http:"+url.substring("http%C2%A0:%20".length());
        } else if (url.startsWith("https%C2%A0:%20")) {
            url = "https:"+url.substring("https%C2%A0:%20".length());
        } else if (url.startsWith("mailto%C2%A0:%20")) {
            url = "mailto:"+url.substring("mailto%C2%A0:%20".length());
        } else if (url.startsWith("aelf%C2%A0:%20")) {
            url = "aelf:"+url.substring("aelf%C2%A0:%20".length());
        }

        // Parse URL
        Uri uri = Uri.parse(url);
        if (uri == null) {
            return true;
        }
        String host = uri.getHost();
        String scheme = uri.getScheme();

        if (host != null && host.equals("www.aelf.org") || url.startsWith("aelf:")) {
            // If this is a request to AELF website, forward it to the main activity
            mActivity.onIntent(new Intent(Intent.ACTION_VIEW, uri));
        } else if (url.startsWith("mailto:")) {
            // Send mail to dev
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setType("text/plain");
            intent.setData(uri);
            mActivity.startActivity(Intent.createChooser(intent, "Envoyer un mail"));
        } else if (scheme != null) {
            // Open external resources
            mActivity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }

        // Always cancel default action
        return true;
    }

    // Inject margin at the bottom to account for the navigation bar
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(mWebView, url);

        Resources resources = mActivity.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        int navigationBarHeight = 0;
        if (resourceId > 0) {
            navigationBarHeight = (int)(resources.getDimension(resourceId) / resources.getDisplayMetrics().density);
        }

        mWebView.loadUrl("javascript:(function(){document.body.style.marginBottom = '"+navigationBarHeight+"px';})()");
    }
}
