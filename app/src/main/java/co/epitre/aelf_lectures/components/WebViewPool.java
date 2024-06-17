package co.epitre.aelf_lectures.components;

import android.content.Context;
import android.content.MutableContextWrapper;
import android.webkit.WebView;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Instanciating a WebView is expensive. This is also something we do on every page slide. This
 * class aims at pooling webviews resources so that we do not instanciate them over and over again.
 *
 * Inspired by:
 * - https://gist.github.com/vishalratna-microsoft/6fb32ef46996144248f3b86a4e07bd40
 * - https://medium.com/microsoft-mobile-engineering/clean-android-webview-caching-9b871b3579f3
 */
public class WebViewPool {
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

    private WebView createWebView() {
        Context WebViewContext = new MutableContextWrapper(appContext);
        return new WebView(WebViewContext);
    }
}
