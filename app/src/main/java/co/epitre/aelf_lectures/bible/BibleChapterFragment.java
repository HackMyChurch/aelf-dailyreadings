package co.epitre.aelf_lectures.bible;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

import co.epitre.aelf_lectures.LecturesActivity;
import co.epitre.aelf_lectures.PinchToZoomListener;
import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.components.ReadingWebViewClient;


public class BibleChapterFragment extends Fragment {
    /**
     * The fragment arguments
     */
    private static final String TAG = "BibleChapterFragment";
    public static final String ARG_TEXT_HTML = "chapter_text";
    public static final String ARG_HIGHLIGHT = "highlight";

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
        String highlight = getArguments().getString(ARG_HIGHLIGHT, "");

        htmlString.append("<!DOCTYPE html><html><head>");
        htmlString.append("<link href=\"css/common.css\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<link href=\"css/theme.css\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<script src=\"js/mark.8.11.1.min.js\" charset=\"utf-8\"></script>\n");
        htmlString.append("</head><body>");
        htmlString.append(body);
        htmlString.append("<script>var highlight='"+highlight.replace("'", "")+"';</script>\n");
        htmlString.append("<script src=\"js/chapter.js\" charset=\"utf-8\"></script>\n");
        htmlString.append("</body></html>");

        // Build UI
        View rootView = inflater.inflate(R.layout.fragment_lecture, container, false);
        mWebView = rootView.findViewById(R.id.LectureView);
        mWebView.clearCache(true);
        mWebSettings = mWebView.getSettings();
        mWebSettings.setBuiltInZoomControls(false);
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setDomStorageEnabled(true);

        // Install theme and styling hooks
        mWebView.setWebViewClient(new ReadingWebViewClient((LecturesActivity) getActivity(), mWebView));

        // Install Zoom support
        mWebView.setOnTouchListener(new ReadingPinchToZoomListener());

        // Load content
        mWebView.loadDataWithBaseURL("file:///android_asset/", htmlString.toString(), "text/html", "utf-8", null);
        mWebView.setBackgroundColor(0x00000000);

        return rootView;
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
