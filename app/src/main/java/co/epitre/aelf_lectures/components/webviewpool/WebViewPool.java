package co.epitre.aelf_lectures.components.webviewpool;

import android.content.Context;
import android.content.MutableContextWrapper;
import co.epitre.aelf_lectures.components.AelfWebView;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Instanciating a WebView is expensive. This is also something we do on every page slide. This
 * class aims at pooling webviews resources so that we do not instanciate them over and over again.
 *
 * Inspired by:
 * - https://gist.github.com/vishalratna-microsoft/6fb32ef46996144248f3b86a4e07bd40
 * - https://medium.com/microsoft-mobile-engineering/clean-android-webview-caching-9b871b3579f3
 */
public class WebViewPool extends WebViewPoolBase {
    private static final String TAG = "WebViewPool";

    private final ArrayBlockingQueue<AelfWebView> webViews;
    private final Context appContext;

    //
    // Initialization code
    //

    WebViewPool(Context appContext, int initialCapacity, int maxCapacity) {
        this.appContext = appContext;
        this.webViews = new ArrayBlockingQueue<>(maxCapacity);

        // Initialize pool
        for (int i = 0; i < initialCapacity; i++) {
            webViews.add(createWebView(appContext));
        }

        // Clear the (application wide) webview cache
        AelfWebView webView = webViews.peek();
        if (webView != null) {
            webView.clearCache(true);
        }
    }

    //
    // Interface
    //

    public AelfWebView borrowWebView(Context ctx) {
        // Get available webview or allocate if needed
        AelfWebView webView = webViews.poll();
        if (webView == null) {
            webView = createWebView(ctx);
        }

        // Swap the context
        MutableContextWrapper mutableContextWrapper = (MutableContextWrapper) webView.getContext();
        mutableContextWrapper.setBaseContext(ctx);

        // Return
        return webView;
    }

    public void releaseWebView(AelfWebView webView) {
        // Swap the context
        MutableContextWrapper mutableContextWrapper = (MutableContextWrapper) webView.getContext();
        mutableContextWrapper.setBaseContext(appContext);

        // Put it back in the pool, unless full
        webViews.offer(webView);
    }
}
