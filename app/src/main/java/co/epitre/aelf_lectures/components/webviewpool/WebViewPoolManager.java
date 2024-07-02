package co.epitre.aelf_lectures.components.webviewpool;

import android.content.Context;
import android.os.Build;
import android.util.Log;

public class WebViewPoolManager {
    private static final String TAG = "WebViewPoolManager";

    private static volatile WebViewPoolBase instance;

    public static void Initialize(Context appContext, int initialCapacity, int maxCapacity) {
        synchronized(WebViewPool.class) {
            if (instance != null) {
                throw new RuntimeException("WebViewPoolManager is already initialized");
            }

            if(isBrokenSurfaceImplementation()) {
                Log.w(TAG, "Initialize: Broken surface implementation detected. Falling back to stub webview pool");
                instance = new WebViewPoolStub();
            } else {
                Log.d(TAG, "Initialize: Creating regular webview pool");
                instance = new WebViewPool(appContext, initialCapacity, maxCapacity);
            }
        }
    }

    public static WebViewPoolBase getInstance() {
        return instance;
    }

    //
    // Internals
    //

    private static boolean isBrokenSurfaceImplementation() {
        if (Build.MANUFACTURER.equals("samsung") && Build.VERSION.RELEASE.equals("8.0.0")) {
            // Samsung phones running Android 8.0.0 detect the use of surface as an attempt to take
            // a screenshot.
            return true;
        }
        return false;
    }
}
