package co.epitre.aelf_lectures.components.webviewpool;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import co.epitre.aelf_lectures.base.VirtualDisplayMutableContextWrapper;
import co.epitre.aelf_lectures.components.AelfWebView;

public abstract class WebViewPoolBase {
    private static final String TAG = "WebViewPoolBase";

    //
    // Interface
    //

    public abstract AelfWebView borrowWebView(Context ctx);
    public abstract void releaseWebView(AelfWebView webView);

    //
    // Internals
    //

    @SuppressLint("SetJavaScriptEnabled")
    protected static AelfWebView createWebView(Context baseContext) {
        Context WebViewContext = new VirtualDisplayMutableContextWrapper(baseContext);
        AelfWebView webView = new AelfWebView(WebViewContext);

        // Common setup
        webView.setBackgroundColor(0x00000000);
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDisabledActionModeMenuItems(WebSettings.MENU_ITEM_WEB_SEARCH);

        // Accessibility: enable (best effort)
        try {
            webView.setAccessibilityDelegate(new View.AccessibilityDelegate());
        } catch (NoClassDefFoundError e) {
            Log.w(TAG, "Accessibility support is not available on this device");
        }

        return webView;
    }
}
