package co.epitre.aelf_lectures.components;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import co.epitre.aelf_lectures.LecturesActivity;
import co.epitre.aelf_lectures.R;


public abstract class ReadingFragment extends Fragment {
    private static final String TAG = "ReadingFragment";

    /**
     * Views
     */
    private View mWebviewPlaceHolder;
    private WebView mWebView;
    private WebSettings mWebSettings;

    /**
     * Internals
     */

    private LecturesActivity lecturesActivity;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get context
        this.lecturesActivity = (LecturesActivity) getActivity();

        // Build UI
        return inflater.inflate(R.layout.fragment_lecture, container, false);
    }

    protected class ReadingPinchToZoomListener extends PinchToZoomListener {
        private ViewGroup mParent;

        ReadingPinchToZoomListener() {
            super(ReadingFragment.this.getContext());
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

    protected void setWebViewContent(@NonNull String content, String historyURL) {
        if (mWebView == null) {
            return;
        }

        mWebView.loadDataWithBaseURL(
                "file:///android_asset/",
                content,
                "text/html",
                "utf-8",
                historyURL
        );
    }

    /*
     * Lifecycle
     */

    @Override
    public void onViewCreated(@NonNull View rootView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);

        // Get and install the WebView
        mWebView = WebViewPool.getInstance().borrowWebView(requireContext());
        mWebSettings = mWebView.getSettings();
        mWebviewPlaceHolder = replaceView(R.id.LectureView, rootView, mWebView);

        // Install theme and styling hooks
        mWebView.setWebViewClient(new ReadingWebViewClient(lecturesActivity, mWebView));

        // Install Zoom support
        mWebView.setOnTouchListener(new ReadingFragment.ReadingPinchToZoomListener());

        // Load content
        loadText();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Get rootView
        View rootView = getView();
        if (rootView == null) {
            return;
        }

        // Release WebView references, allow them to be garbage collected
        replaceView(R.id.LectureView, rootView, mWebviewPlaceHolder);
        WebViewPool.getInstance().releaseWebView(mWebView);
        mWebviewPlaceHolder = null;
        mWebView = null;
        mWebSettings = null;
    }

    /*
     * Internal tools
     */

    protected String getThemeCss() {
        String themeName = this.lecturesActivity.getNightMode() ? "dark":"light";
        return "css/theme-"+themeName+".css";
    }

    private View replaceView(@IdRes int id, View rootView, View newView) {
        // Locate view location
        View oldView = rootView.findViewById(id);
        ViewGroup parentView = (ViewGroup)oldView.getParent();
        int viewIndex = parentView.indexOfChild(oldView);

        // Preserve IDs
        newView.setId(oldView.getId());

        // Swap views, preserving layout
        parentView.removeView(oldView);
        parentView.addView(
                newView,
                viewIndex,
                oldView.getLayoutParams()
        );

        return oldView;
    }

    /**
     * Interface
     */

    abstract protected void loadText();
}
