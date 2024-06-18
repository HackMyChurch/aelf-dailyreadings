package co.epitre.aelf_lectures.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.MutableContextWrapper;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.util.concurrent.ArrayBlockingQueue;

import co.epitre.aelf_lectures.base.VirtualDisplayMutableContextWrapper;

/**
 * Instanciating a WebView is expensive. This is also something we do on every page slide. This
 * class aims at pooling webviews resources so that we do not instanciate them over and over again.
 *
 * Inspired by:
 * - https://gist.github.com/vishalratna-microsoft/6fb32ef46996144248f3b86a4e07bd40
 * - https://medium.com/microsoft-mobile-engineering/clean-android-webview-caching-9b871b3579f3
 */
public class WebViewPool {
    private static final String TAG = "WebViewPool";

    private static volatile WebViewPool instance;

    private final ArrayBlockingQueue<WebView> webViews;
    private final Context appContext;

    //
    // Initialization code
    //

    private WebViewPool(Context appContext, int initialCapacity, int maxCapacity) {
        this.appContext = appContext;
        this.webViews = new ArrayBlockingQueue<>(maxCapacity);

        // Initialize pool
        for (int i = 0; i < initialCapacity; i++) {
            webViews.add(createWebView());
        }

        // Clear the (application wide) webview cache
        WebView webView = webViews.peek();
        webView.clearCache(true);
    }

    public static void Initialize(Context appContext, int initialCapacity, int maxCapacity) {
        synchronized(WebViewPool.class) {
            if (instance != null) {
                throw new RuntimeException("WebViewPoolManager is already initialized");
            }

            instance = new WebViewPool(appContext, initialCapacity, maxCapacity);
        }
    }

    public static WebViewPool getInstance() {
        return instance;
    }

    //
    // Interface
    //

    public WebView borrowWebView(Context ctx) {
        // Get available webview or allocate if needed
        WebView webView = webViews.poll();
        if (webView == null) {
            webView = createWebView();
        }

        // Swap the context
        MutableContextWrapper mutableContextWrapper = (MutableContextWrapper) webView.getContext();
        mutableContextWrapper.setBaseContext(ctx);

        // Return
        return webView;
    }

    public void releaseWebView(WebView webView) {
        // Swap the context
        MutableContextWrapper mutableContextWrapper = (MutableContextWrapper) webView.getContext();
        mutableContextWrapper.setBaseContext(appContext);

        // Put it back in the pool, unless full
        webViews.offer(createWebView());
    }

    //
    // Internals
    //

    @SuppressLint("SetJavaScriptEnabled")
    private WebView createWebView() {
        Context WebViewContext = new VirtualDisplayMutableContextWrapper(appContext);
        WebView webView = new WebView(WebViewContext);

        // Common setup
        webView.setBackgroundColor(0x00000000);
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // Accessibility: enable (best effort)
        try {
            webView.setAccessibilityDelegate(new View.AccessibilityDelegate());
        } catch (NoClassDefFoundError e) {
            Log.w(TAG, "Accessibility support is not available on this device");
        }

        return webView;
    }
}
