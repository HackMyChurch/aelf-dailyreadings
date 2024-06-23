package co.epitre.aelf_lectures.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import co.epitre.aelf_lectures.settings.SettingsActivity;

public class SyncManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    // General configuration
    private static final String TAG = "SyncManager";
    public static final String SYNC_JOB_TAG = "sync";
    public static final String SYNC_JOB_NAME = "aelf-office-sync";
    public static final String SYNC_JOB_EXPEDITED_NAME = "aelf-office-sync-expedited";
    public static final long SYNC_INTERVAL_PERIOD = 24L;
    public static final long SYNC_INTERVAL_JITTER = 6L;
    public static final long SYNC_INITIAL_DELAY_MINUTES = 5L;

    // Resources
    private static volatile SyncManager instance;
    private final Context mContext;
    private final SharedPreferences mSyncPrefs;
    private final WorkManager mWorkManager;

    /**
     * Singleton
     */

    SyncManager(Context context) {
        super();
        this.mContext = context.getApplicationContext();
        this.mSyncPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        if (!WorkManager.isInitialized()) {
            // Attempt to workaround IllegalStateException: WorkManager is not initialized properly.
            // on Itel itel-A665L / Android 13 (SDK 33)
            // cf. https://play.google.com/console/u/0/developers/7638290669081425144/app/4975758405726891388/vitals/crashes/82a8911e66a8a4ec34ac3380b1695a53/details?versionCode=81
            WorkManager.initialize(this.mContext, new Configuration.Builder().build());
        }
        this.mWorkManager = WorkManager.getInstance(this.mContext);

        // Ensure the sync request is enqueued
        this.ensureSyncWorkRequest();
    }

    public synchronized static SyncManager getInstance(Context context) {
        if (SyncManager.instance == null) {
            SyncManager.instance = new SyncManager(context);
        }
        return SyncManager.instance;
    }

    /**
     * Create or update the periodic sync job
     */
    private void ensureSyncWorkRequest() {
        // Get WiFi vs Non-Wifi pref
        boolean wifiOnlyPref = this.mSyncPrefs.getBoolean(SettingsActivity.KEY_PREF_SYNC_WIFI_ONLY, true);

        // Build the job specification
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(wifiOnlyPref?NetworkType.UNMETERED:NetworkType.CONNECTED)
                .setRequiresCharging(true)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                SYNC_INTERVAL_PERIOD, TimeUnit.HOURS,
                SYNC_INTERVAL_JITTER, TimeUnit.HOURS
            )
            .addTag(SYNC_JOB_TAG)
            .setConstraints(constraints)
            .build();

        // Enqueue
        this.mWorkManager.enqueueUniquePeriodicWork(SYNC_JOB_NAME, ExistingPeriodicWorkPolicy.UPDATE, syncRequest);
    }

    /**
     * State management
     */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        // Preferences were cleared, ignore
        if(key == null) {
            return;
        }

        // Sync constraint(s) changed. Update sync job.
        if(key.equals(SettingsActivity.KEY_PREF_SYNC_WIFI_ONLY)) {
            this.ensureSyncWorkRequest();
        }
    }

    /**
     * Helpers: return the time in hours elapsed since the last sync attempt / success
     */

    public long getLastSyncSuccessAgeHours() {
        SharedPreferences syncStat = this.mContext.getSharedPreferences("sync-stats", Context.MODE_PRIVATE);
        long currentTimeMillis = System.currentTimeMillis();
        long lastSync = syncStat.getLong(SettingsActivity.KEY_APP_SYNC_LAST_SUCCESS, -1);

        if (lastSync < 0) {
            return -1;
        }
        long lastSyncAge = (currentTimeMillis - lastSync) / 1000 / 3600;
        Log.d(TAG, "Last sync timestamp: "+lastSync+" age: "+lastSyncAge);
        return lastSyncAge;
    }

    public void triggerSync() {
        triggerSync(SYNC_INITIAL_DELAY_MINUTES, TimeUnit.MINUTES);
    }

    public void triggerSync(long initialDelay, TimeUnit initialDelayUnit) {
        // Build the job specification
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Log.i(TAG, "Scheduling sync in "+initialDelay+" "+initialDelayUnit.toString());
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .addTag(SYNC_JOB_TAG)
                .setInitialDelay(initialDelay, initialDelayUnit)
                .setConstraints(constraints)
                .build();

        // Enqueue
        this.mWorkManager.enqueueUniqueWork(
                SYNC_JOB_EXPEDITED_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                syncRequest
        );
    }
}
