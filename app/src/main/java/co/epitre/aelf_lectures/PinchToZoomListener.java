package co.epitre.aelf_lectures;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class PinchToZoomListener implements View.OnTouchListener {
    private ScaleGestureDetector mScaleDetector;
    private SharedPreferences preferences;


    class PinchListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private static final String TAG = "PinchListeners";

        private int initialZoom;
        private int newZoom;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Compute new zoom
            float scale = detector.getScaleFactor();
            newZoom = (int)(initialZoom * scale);

            // Minimum zoom is 100%. This helps keep something at least a little readable
            // and intuitively reset to default zoom level.
            if (newZoom < 100) {
                newZoom = 100;
            }

            // Apply zoom
            Log.d(TAG, "pinch scaling factor: "+scale+"; new zoom: "+newZoom);
            onZoom(newZoom);

            // Do not restart scale factor to 1, until the user removed his fingers
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            initialZoom = onZoomStart();
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
            onZoomEnd(newZoom);
        }
    }

    public PinchToZoomListener(Context context) {
        // Get preference store
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Start the scale detector
        mScaleDetector = new ScaleGestureDetector(context, new PinchListener());
        if (Build.VERSION.SDK_INT >= 23) {
            mScaleDetector.setStylusScaleEnabled(false); // disable stylus scale
        }
        if (Build.VERSION.SDK_INT >= 19) {
            mScaleDetector.setQuickScaleEnabled(false);  // disable double tap + swipe
        }

        // Set initial zoom
        int initialZoom = this.onZoomStart();
        this.onZoomEnd(initialZoom);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        return mScaleDetector.isInProgress();
    }

    public int onZoomStart() {
        // Get current scale preference
        return preferences.getInt(SyncPrefActivity.KEY_PREF_DISP_FONT_SIZE, 100);
    }

    public void onZoomEnd(int zoomLevel) {
        // Save new scale preference
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SyncPrefActivity.KEY_PREF_DISP_FONT_SIZE, zoomLevel);
        editor.apply();
    }

    public void onZoom(int zoomLevel) {
        return;
    }
}
