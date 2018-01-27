package co.epitre.aelf_lectures;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * "Lecture" renderer
 */
public class LectureFragment extends Fragment implements
        NetworkStatusMonitor.NetworkStatusChangedListener,
        OnSharedPreferenceChangeListener {
    /**
     * The fragment arguments
     */
    private static final String TAG = "LectureFragment";
    public static final String ARG_TEXT_HTML = "text html";
    protected WebView lectureView;
    protected WebSettings websettings;
    private SwipeRefreshLayout swipeLayout;

    SharedPreferences preferences;
    NetworkStatusMonitor networkStatusMonitor = NetworkStatusMonitor.getInstance();

    /**
     * Swipe refresh / zoom status
     */
    private boolean isZooming = false;
    private boolean hasNetwork = false;

    @Override
    public void onNetworkStatusChanged(NetworkStatusMonitor.NetworkStatusEvent networkStatusEvent) {
        switch (networkStatusEvent) {
            case NETWORK_OFF:
                hasNetwork = false;
                break;
            case NETWORK_ON:
                hasNetwork = true;
                break;
        }
        refreshSwipeToRefreshEnabled();
    }

    private synchronized void refreshSwipeToRefreshEnabled() {
        boolean prefPullToRefreshEnabled = preferences.getBoolean(SyncPrefActivity.KEY_PREF_DISP_PULL_TO_REFRESH, false);
        swipeLayout.setEnabled(!isZooming && hasNetwork && prefPullToRefreshEnabled);
    }

    interface LectureLinkListener {
        boolean onLectureLink(Uri link);
    }

    public LectureFragment() {
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(SyncPrefActivity.KEY_PREF_DISP_FONT_SIZE)) {
            this.refresh();
        } else if (key.equals(SyncPrefActivity.KEY_PREF_DISP_PULL_TO_REFRESH)) {
            this.refreshSwipeToRefreshEnabled();
        }
    }
    
    /* refresh zoom */
    public void refresh() {
        Context context = getActivity();
        if(context == null) {
            return; // we're a dead object
        }

        // load current zoom level
        int zoom = preferences.getInt(SyncPrefActivity.KEY_PREF_DISP_FONT_SIZE, 100);
        setCurrentZoom(zoom);
    }


    // Helper: get zoom as percent, even on older phones
    protected int getCurrentZoom() {
        if (websettings == null) {
            return -1;
        }

        return websettings.getTextZoom();
    }
    // Helper: set zoom as percent, even on older phones
    protected void setCurrentZoom(int zoom) {
        if (websettings == null) {
            return;
        }

        websettings.setTextZoom(zoom);
        return;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = getActivity();

        // compute view --> HTML
        StringBuilder htmlString = new StringBuilder();
        String body = getArguments().getString(ARG_TEXT_HTML);

        String col_red_hex = Integer.toHexString(ContextCompat.getColor(context, R.color.aelf_red)).substring(2);
        String col_sepia_light = Integer.toHexString(ContextCompat.getColor(context, R.color.sepia_bg)).substring(2);
        String col_sepia_dark = Integer.toHexString(ContextCompat.getColor(context, R.color.sepia_fg)).substring(2);

        htmlString.append("<!DOCTYPE html>" +
                "<html>" +
                    "<head>" +
                        "<meta charset=\"utf-8\">" +
                        "<style type=\"text/css\">" +
                        "body{" +
                        "	margin:24px;" +
                        "	background-color:#"+col_sepia_light+";" +
                        "	color:#"+col_sepia_dark+";" +
                        "   font-family: sans-serif;" +
                        "	font-size: 15px;" + // regular body
                        "	font-weight: regular;" +
                        "}" +
                        "h3 {" + // title
                        "	font-size: 20px;" +
                        "	font-weight: bold;" +
                        "}" +
                        "p {" +
                        "   line-height: 1.2;"+
                        "}" +
                        "div.app-office-navigation {" +
                        "    margin-top: 20px;" +
                        "}" +
                        ".app-office-navigation a {" +
                        "    display: block;" +
                        "    text-align: center;" +
                        "    padding: 13px;" +
                        "    margin-top: 10px;" +
                        "    color: #"+col_sepia_dark+";" +
                        "	 font-size: 17px;" +
                        "    text-decoration: none;" +
                        "    border: 1px solid #"+col_sepia_dark+";" +
                        "}"+
                        ".app-office-navigation a:active, .app-office-navigation a.active {" +
                        "    color: #"+col_sepia_dark+";" +
                        "    background-color: #"+col_sepia_light+";" +
                        "}"+
                        "b i{" + // sub-title
                        "	font-size: 15px;" +
                        "	display: block;" +
                        "	margin-top: -12px;" +
                        "	margin-bottom: 20px;" +
                        "}" +
                        "blockquote {" +
                        "	margin-right: 20px" +
                        "}" +
                        "blockquote p {" +
                        "	margin-top: 30px;" +
                        "}" +
                        "h3 small i{" + // global reference
                        "	display: block;" +
                        "	float: right;" +
                        "   font-weight: normal;" +
                        "	margin-top: 5px;" +
                        "}" +
                        "blockquote small i{" + // citation reference
                        "	display: block;" +
                        "	text-align: right;" +
                        "   margin-top: -15px;" +
                        "	margin-right: 0;" +
                        "   padding-top: 0;" +
                        "}" +
                        "font[color='#cc0000'], font[color='#ff0000'], font[color='#CC0000'], font[color='#FF0000'] {" + // psaume refrain
                        "	color: #"+col_red_hex+";" +
                        "} " +
                        "font[color='#000000'] {" + // regular text
                        "	color: #"+col_sepia_dark+";" +
                        "} " +
                        ".verse {" + // psaume verse number
                        "	display: block;" +
                        "   float: left;" +
                        "   width: 25px;" +
                        "   text-align: right;" +
                        "   margin-top: 4px;" +
                        "   margin-left: -30px;" +
                        "	font-size: 10px;" +
                        "	color: #"+col_red_hex+";" +
                        "}" +
                        "sup {" + // inflections: do not affect line-height
                        "   vertical-align: baseline;" +
                        "   position: relative;" +
                        "   top: -0.4em;" +
                        "}" +
                        ".underline {" +
                        "    text-decoration: underline;" +
                        "}" +
                        // indent line when verse is too long to fit on the screen
                        ".line .verse {" +
                        "   margin-left: -30px;" +
                        "}" +
                        ".line-wrap .verse {" +
                        "   margin-left: -55px;" +
                        "}" +
                        ".line {" +
                        "   display: block;" +
                        "   margin-bottom: 5px;" +
                        "}" +
                        ".line-wrap {" +
                        "   display: block;" +
                        "   padding-left: 25px;" +
                        "   text-indent: -25px;" +
                        "   margin-bottom: 1px;" +
                        "}" +
                        "img {" +
                        "   display: none;" + // quick and dirty fix for spurious images. May need to be removed / hacked
                        "}" +
                        "</style>" +
                    "</head>" +
                    "<body>");
        htmlString.append(body);
        htmlString.append("</body></html>");

        String reading = htmlString.toString();

        // actual UI refresh
        View rootView = inflater.inflate(R.layout.fragment_lecture, container, false);
        lectureView = (WebView) rootView.findViewById(R.id.LectureView);
        swipeLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.LectureSwipeRefresh);
        websettings = lectureView.getSettings();
        websettings.setBuiltInZoomControls(false);

        // Log.d("HTML IN", "VERSION: "+lectureView.getSettings().getUserAgentString()+" DATA: "+reading);

        // Capture links
        lectureView.setWebViewClient(new WebViewClient() {
            // DEBUG: print interpreted HTML
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (Build.VERSION.SDK_INT >= 19) {
                    view.evaluateJavascript(
                            "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String html) {
                                    // Log.d("HTML INTERPRETED", "VERSION: "+lectureView.getSettings().getUserAgentString()+" DATA: "+html.replace("\\u003C", "<").replace("\\\"", "\""));
                                }
                            }
                    );
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.w(TAG, "Got a URL: "+url);

                // Prepare URL
                url = url.replace("file:///android_asset/", "");
                if (url.startsWith("http%C2%A0:%20")) {
                    url = "http:"+url.substring("http%C2%A0:%20".length());
                } else if (url.startsWith("https%C2%A0:%20")) {
                    url = "https:"+url.substring("https%C2%A0:%20".length());
                } else if (url.startsWith("mailto%C2%A0:%20")) {
                    url = "mailto:"+url.substring("mailto%C2%A0:%20".length());
                } else if (url.startsWith("aelf%C2%A0:%20")) {
                    url = "aelf:"+url.substring("aelf%C2%A0:%20".length());
                }

                // Parse URL
                Uri uri = Uri.parse(url);
                if (uri == null) {
                    return true;
                }
                String host = uri.getHost();

                if (host != null && host.equals("www.aelf.org") || url.startsWith("aelf:")) {
                    // If this is a request to AELF website, forward it to the main activity
                    LectureLinkListener listener = null;
                    try {
                        listener = (LectureLinkListener) getActivity();
                    } catch (ClassCastException e) {
                        // Ignore. Means the activity does not implement the interface
                    }

                    if (listener != null) {
                        listener.onLectureLink(uri);
                    }
                } else if (url.startsWith("mailto:")) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setType("text/plain");
                    intent.setData(uri);
                    startActivity(Intent.createChooser(intent, "Envoyer un mail"));
                }

                // Always cancel default action
                return true;
            }
        });

        // capture refresh events
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                FragmentActivity activity = getActivity();
                LecturesActivity test  = (LecturesActivity) activity;
                test.onRefresh("pull");
                swipeLayout.setRefreshing(false); // we have our own spinner
            }
        });

        // accessibility: enable (best effort)
        websettings.setJavaScriptEnabled(true);
        try {
            lectureView.setAccessibilityDelegate(new View.AccessibilityDelegate());
        } catch (NoClassDefFoundError e) {
            Log.w(TAG, "Accessibility support is not available on this device");
        }

        //accessibility: drop the underline attributes && line wrapper fixes, they break the screen readers
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(am.isEnabled()) {
            reading = reading.replaceAll("</?u>", "")
                             // FIXME: what do people prefer ? Line by line or § by § ?
                             .replaceAll("</line><line>", "<br aria-hidden=true />")
                             .replaceAll("</?line>", "");
        }

        // load content
        lectureView.loadDataWithBaseURL("file:///android_asset/", reading, "text/html", "utf-8", null);
        lectureView.setBackgroundColor(0x00000000);

        // register listener
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);

        // font size
        this.refresh();

        // Attempt to workaround a strange native crash:
        // http://stackoverflow.com/questions/19614526/android-crash-system-lib-libhwui-so
        lectureView.post(new Runnable() {
            @Override
            public void run () {
                lectureView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
            }
        });

        class PinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            private int initialScale;
            private int newZoom;

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                // Compute new zoom
                float scale = detector.getScaleFactor();
                newZoom = (int)(initialScale * scale);

                // Minimum zoom is 100%. This helps keep something at least a little readable
                // and intuitively reset to default zoom level.
                if (newZoom < 100) {
                    newZoom = 100;
                }

                // Apply zoom
                Log.d(TAG, "pinch scaling factor: "+scale+"; new zoom: "+newZoom);
                setCurrentZoom(newZoom);

                // Do not restart scale factor to 1, until the user removed his fingers
                return false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                initialScale = getCurrentZoom();

                // Disable "Swipe to refresh" to prevent accidental refresh while zooming
                isZooming = true;
                refreshSwipeToRefreshEnabled();

                return super.onScaleBegin(detector);
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                super.onScaleEnd(detector);

                // Re-enable "Swipe to refresh"
                isZooming = false;
                refreshSwipeToRefreshEnabled();

                // Save new scale preference
                Context context = getActivity();
                if(context == null) {
                    return; // we're a dead object
                }

                // load current zoom level
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(SyncPrefActivity.KEY_PREF_DISP_FONT_SIZE, newZoom);
                editor.apply();
            }
        }

        final ScaleGestureDetector mScaleDetector = new ScaleGestureDetector(context, new PinchListener());
        if (Build.VERSION.SDK_INT >= 23) {
            mScaleDetector.setStylusScaleEnabled(false); // disable stylus scale
        }
        if (Build.VERSION.SDK_INT >= 19) {
            mScaleDetector.setQuickScaleEnabled(false);  // disable double tap + swipe
        }

        lectureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleDetector.onTouchEvent(event);
                return mScaleDetector.isInProgress();
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        networkStatusMonitor.registerNetworkStatusChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        networkStatusMonitor.unregisterNetworkStatusChangeListener(this);
    }
}
