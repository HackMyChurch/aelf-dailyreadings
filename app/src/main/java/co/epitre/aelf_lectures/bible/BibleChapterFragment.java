package co.epitre.aelf_lectures.bible;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.IOException;
import java.io.InputStream;

import co.epitre.aelf_lectures.PinchToZoomListener;
import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.SyncPrefActivity;


public class BibleChapterFragment extends Fragment {
    /**
     * The fragment arguments
     */
    private static final String TAG = "BibleChapterFragment";
    public static final String ARG_TEXT_HTML = "chapter_text";

    /**
     * Views
     */
    protected WebView mWebView;
    protected WebSettings mWebSettings;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Build content
        StringBuilder htmlString = new StringBuilder();
        String body = getArguments().getString(ARG_TEXT_HTML);

        htmlString.append("<!DOCTYPE html><html><head>");
        htmlString.append("<link href=\"css/common.css\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<link href=\"css/theme.css\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("</head><body>");
        htmlString.append(body);
        htmlString.append("</body></html>");

        // Build UI
        View rootView = inflater.inflate(R.layout.fragment_lecture, container, false);
        mWebView = rootView.findViewById(R.id.LectureView);
        mWebView.clearCache(true);
        mWebSettings = mWebView.getSettings();
        mWebSettings.setBuiltInZoomControls(false);
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setDomStorageEnabled(true);

        // Disable swipe layout (only used by the lecture view)
        SwipeRefreshLayout swipeLayout = rootView.findViewById(R.id.LectureSwipeRefresh);
        swipeLayout.setEnabled(false);

        // Install theme and styling hooks
        mWebView.setWebViewClient(new ReadingWebViewClient());

        // Install Zoom support
        mWebView.setOnTouchListener(new ReadingPinchToZoomListener());

        // Load content
        mWebView.loadDataWithBaseURL("file:///android_asset/", htmlString.toString(), "text/html", "utf-8", null);
        mWebView.setBackgroundColor(0x00000000);

        return rootView;
    }

    private class ReadingWebViewClient extends WebViewClient {
        // Route the virtual theme.css to the active theme css
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (url.equals("file:///android_asset/css/theme.css")) {
                // Detect the current theme
                boolean nightMode = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(SyncPrefActivity.KEY_PREF_DISP_NIGHT_MODE, false);
                String themeName = nightMode ? "dark":"light";
                String cssPath = "css/theme-"+themeName+".css";

                // Load the selected CSS
                try {
                    InputStream styleStream = getActivity().getAssets().open(cssPath);
                    return new WebResourceResponse("text/css", "UTF-8", styleStream);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to load "+themeName+" theme", e);
                    return null;
                }
            }

            return super.shouldInterceptRequest(view, url);
        }

        // Inject margin at the bottom to account for the navigation bar
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(mWebView, url);

            Context context = getContext();
            if (context == null) {
                return;
            }

            Resources resources = context.getResources();
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            int navigationBarHeight = 0;
            if (resourceId > 0) {
                navigationBarHeight = (int)(resources.getDimension(resourceId) / getResources().getDisplayMetrics().density);
            }

            mWebView.loadUrl("javascript:(function(){document.body.style.marginBottom = '"+navigationBarHeight+"px';})()");
        }
    }

    private class ReadingPinchToZoomListener extends PinchToZoomListener {
        private ViewGroup mParent;

        ReadingPinchToZoomListener() {
            super(BibleChapterFragment.this.getContext());
            mParent = (ViewGroup)mWebView.getParent();

            mParent.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    int width = right - left;
                    int oldWidth = oldRight - oldLeft;

                    // On width change
                    if (width != oldWidth) {
                        onZoomEnd(getCurrentZoomLevel());
                    }
                }
            });
        }

        private void setCurrentZoom(int zoom) {
            if (mWebSettings == null || mParent == null || !isAdded()) {
                return;
            }

            // Get the base width
            int base_width = (int)(450 * getResources().getDisplayMetrics().density);

            // Get current parent width
            int parent_width = mParent.getMeasuredWidth();
            if (parent_width == 0) {
                return; // Fast path: not yet visible
            }

            // Compute new width
            int current_width = mWebView.getMeasuredWidth();
            int new_width = base_width * zoom / 100;
            if (new_width > parent_width) {
                new_width = parent_width;
            }

            // Apply new width and zoom
            mWebSettings.setTextZoom(zoom);
            if (current_width != new_width) {
                ViewGroup.LayoutParams params = mWebView.getLayoutParams();
                params.width = new_width;
                mWebView.setLayoutParams(params);
            }
        }

        public int onZoomStart() {
            return super.onZoomStart();
        }

        public void onZoomEnd(int zoomLevel) {
            super.onZoomEnd(zoomLevel);
            setCurrentZoom(zoomLevel);
        }

        public void onZoom(int zoomLevel) {
            super.onZoom(zoomLevel);
            setCurrentZoom(zoomLevel);
        }
    }
}
