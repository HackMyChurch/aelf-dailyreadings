package co.epitre.aelf_lectures;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.os.Process;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.TrackerConfig;
import org.piwik.sdk.extra.DownloadTracker;
import org.piwik.sdk.extra.PiwikApplication;
import org.piwik.sdk.extra.TrackHelper;

import static co.epitre.aelf_lectures.SyncPrefActivity.KEY_PREF_PARTICIPATE_STATISTICS;

// Attempt to fix crash on Android 4.4 when upgrading app
// http://stackoverflow.com/questions/40069273/unable-to-get-provider-rarely-crash-on-kitkat
public class LecturesApplication extends PiwikApplication implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "LecturesApplication";

    public static final int STATS_DIM_SOURCE = 1;
    public static final int STATS_DIM_STATUS = 2;
    public static final int STATS_DIM_DAY_DELTA = 3;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "app start...");
        checkAppReplacingState();
        initPiwik();
    }

    private void checkAppReplacingState() {
        if (getResources() == null) {
            Log.w(TAG, "app is replacing...kill");
            Process.killProcess(Process.myPid());
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
        if(key.equals(KEY_PREF_PARTICIPATE_STATISTICS)) {
            Tracker tracker = getTracker();
            tracker.setOptOut(!settings.getBoolean(KEY_PREF_PARTICIPATE_STATISTICS, true));
            Log.i(TAG, "Piwik OptOut status changed to: "+tracker.isOptOut());
        }
    }

    @Override
    public TrackerConfig onCreateTrackerConfig() {
        return TrackerConfig.createDefault("https://piwik.app.epitre.co/", 2);
    }

    // see https://github.com/piwik/piwik-sdk-android/blob/master/exampleapp/src/main/java/com/piwik/demo/DemoApp.java
    private void initPiwik() {
        // When working on an app we don't want to skew tracking results.
        // getPiwik().setDryRun(BuildConfig.DEBUG);

        // Load application preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(this);

        // Track this app install, this will only trigger once per app version.
        Tracker tracker = getTracker();
        TrackHelper.track().download().identifier(new DownloadTracker.Extra.ApkChecksum(this)).with(tracker);
        tracker.setOptOut(!settings.getBoolean(KEY_PREF_PARTICIPATE_STATISTICS, true));
        // tracker.setDispatchInterval(1);

        // TODO: enable gzip. Will require server side LUA
        // http://www.pataliebre.net/howto-make-nginx-decompress-a-gzipped-request.html
        // tracker.setDispatchGzipped(true);

        Log.i(TAG, "Piwik setup complete. OptOut status: "+tracker.isOptOut());
    }
}