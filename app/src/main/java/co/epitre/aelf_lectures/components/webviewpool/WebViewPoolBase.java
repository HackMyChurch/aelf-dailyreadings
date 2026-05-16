package co.epitre.aelf_lectures.components.webviewpool;

import android.annotation.SuppressLint;
import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;

import co.epitre.aelf_lectures.base.VirtualDisplayMutableContextWrapper;

public abstract class WebViewPoolBase {
    private static final String TAG = "WebViewPoolBase";

    //
    // Interface
    //

    public abstract WebView borrowWebView(Context ctx);
    public abstract void releaseWebView(WebView webView);

    //
    // Internals
    //

    @SuppressLint("SetJavaScriptEnabled")
    protected static WebView createWebView(Context baseContext) {
        Context WebViewContext = new VirtualDisplayMutableContextWrapper(baseContext);
        WebView webView = new WebView(WebViewContext);

        // Common setup
        webView.setBackgroundColor(0x00000000);
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        return webView;
    }
}
