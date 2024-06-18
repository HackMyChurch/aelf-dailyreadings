package co.epitre.aelf_lectures.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import co.epitre.aelf_lectures.settings.SettingsActivity;

public class ReadingPinchToZoomListener implements View.OnTouchListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int MIN_ZOOM_LEVEL = 100;
    private static final int MAX_ZOOM_LEVEL = 700;

    private final ScaleGestureDetector mScaleDetector;
    private final PinchListener mPinchListener;
    private final SharedPreferences preferences;
    private int currentZoom;

    private final Context context;
    private final WebView mWebview;
    private final WebSettings mWebsettings;
    private final ViewGroup mParent;

    class PinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        // Start with min zoom level to prevent accidental zoom = 0
        private int initialZoom = MIN_ZOOM_LEVEL;
        private int newZoom = MIN_ZOOM_LEVEL;
        private boolean enabled = true;

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Compute new zoom
            float scale = detector.getScaleFactor();
            newZoom = (int)(initialZoom * scale);

            // Apply zoom
            onZoom(newZoom);

            // Do not restart scale factor to 1, until the user removed his fingers
            return false;
        }

        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            // Is the "Pinch to zoom" feature enabled ?
            if (!enabled) {
                return false;
            }
            initialZoom = onZoomStart();
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
            onZoomEnd(newZoom);
        }
    }

    public ReadingPinchToZoomListener(Context context, WebView webview) {
        this.context = context;
        this.mWebview = webview;
        this.mWebsettings = webview.getSettings();
        this.mParent = (ViewGroup) webview.getParent();

        // Get preference store
        preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        // Start the scale detector
        mPinchListener = new PinchListener();
        mScaleDetector = new ScaleGestureDetector(context, mPinchListener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mScaleDetector.setStylusScaleEnabled(false); // disable stylus scale
        }
        mScaleDetector.setQuickScaleEnabled(false);  // disable double tap + swipe
        mPinchListener.setEnabled(preferences.getBoolean(SettingsActivity.KEY_PREF_DISP_PINCH_TO_ZOOM, true));

        // Set initial zoom
        int initialZoom = this.onZoomStart();
        this.onZoomEnd(initialZoom);

        // Register application wide zoom change listener
        preferences.registerOnSharedPreferenceChangeListener(this);

        // Register layout change listener
        mParent.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = right - left;
            int oldWidth = oldRight - oldLeft;

            // On width change
            if (width != oldWidth) {
                onZoomEnd(getCurrentZoomLevel());
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) {
            return;
        }

        // Apply externally changed zoom
        if(key.equals(SettingsActivity.KEY_PREF_DISP_FONT_SIZE)) {
            int zoomLevel = preferences.getInt(SettingsActivity.KEY_PREF_DISP_FONT_SIZE, 100);
            this.onZoomEnd(zoomLevel);
        }

        // Dynamically enable/disable pinch to zoom
        if(key.equals(SettingsActivity.KEY_PREF_DISP_PINCH_TO_ZOOM)) {
            mPinchListener.setEnabled(preferences.getBoolean(SettingsActivity.KEY_PREF_DISP_PINCH_TO_ZOOM, true));
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        return mScaleDetector.isInProgress();
    }

    //
    // Zoom logic
    //

    private static int cropZoom(int zoom) {
        return Math.min(Math.max(zoom, MIN_ZOOM_LEVEL), MAX_ZOOM_LEVEL);
    }

    public int getCurrentZoomLevel() {
        return currentZoom;
    }

    private void setCurrentZoom(int zoomLevel) {
        if (mWebsettings == null || mParent == null) {
            return;
        }

        // Validate input: zoom must be between 100% and 700%
        zoomLevel = cropZoom(zoomLevel);

        // Save new scale preference
        if (currentZoom != zoomLevel) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(SettingsActivity.KEY_PREF_DISP_FONT_SIZE, zoomLevel);
            editor.apply();
        }
        currentZoom = zoomLevel;

        // Get the base width
        int base_width = (int) (450 * context.getResources().getDisplayMetrics().density);

        // Get current parent width
        int parent_width = mParent.getMeasuredWidth();
        if (parent_width == 0) {
            return; // Fast path: not yet visible
        }

        // Compute new width
        int current_width = mWebview.getMeasuredWidth();
        int new_width = base_width * zoomLevel / 100;
        if (new_width > parent_width) {
            new_width = parent_width;
        }

        // Apply new width and zoom
        mWebsettings.setTextZoom(zoomLevel);
        if (current_width != new_width) {
            ViewGroup.LayoutParams params = mWebview.getLayoutParams();
            params.width = new_width;
            mWebview.setLayoutParams(params);
        }
    }

    public int onZoomStart() {
        return preferences.getInt(SettingsActivity.KEY_PREF_DISP_FONT_SIZE, 100);
    }

    public void onZoomEnd(int zoomLevel) {
        setCurrentZoom(zoomLevel);
    }

    public void onZoom(int zoomLevel) {
        setCurrentZoom(zoomLevel);
    }
}
