package co.epitre.aelf_lectures.components.webviewpool;

import android.content.Context;
import android.webkit.WebView;

/**
 * This class is a Stub implementation of the WebViewPool that will always allocate a new instance
 * on borrow and "forget" it on release.
 *
 * This implementation is meant to be used on phones with buggy surface implementations. For example,
 * at least Samsung phones running Android 8.0 detect the use of surface as an attempt to take a
 * screenshot. This mis-detection is a cause of concern by users.
 */
public class WebViewPoolStub extends WebViewPoolBase {
    @Override
    public WebView borrowWebView(Context ctx) {
        return createWebView(ctx);
    }

    @Override
    public void releaseWebView(WebView webView) {}
}
