package co.epitre.aelf_lectures.components;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        mWebviewPlaceHolder = replaceView(R.id.LectureView, rootView, mWebView);

        // Install theme and styling hooks
        mWebView.setWebViewClient(new ReadingWebViewClient(lecturesActivity, mWebView));

        // Install Zoom support
        mWebView.setOnTouchListener(new ReadingPinchToZoomListener(getContext(), mWebView));

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
        mWebView.setOnTouchListener(null);
        mWebView.setWebViewClient(null);
        WebViewPool.getInstance().releaseWebView(mWebView);
        mWebviewPlaceHolder = null;
        mWebView = null;
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
