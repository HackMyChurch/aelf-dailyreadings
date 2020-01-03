package co.epitre.aelf_lectures.sync;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import co.epitre.aelf_lectures.NetworkStatusMonitor;
import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.SyncPrefActivity;
import co.epitre.aelf_lectures.data.AelfDate;
import co.epitre.aelf_lectures.data.LecturesController;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

// FIXME: this class is a *mess*. We need to rewrite it !

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "AELFSyncAdapter";

    private Context mContext;
    private LecturesController mController;

    NetworkStatusMonitor networkStatusMonitor;

    private static final long MAX_RUN_TIME = TimeUnit.MINUTES.toMillis(30);

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        this.mContext = context;
        this.mController = LecturesController.getInstance(this.getContext());

        // Network state change listener
        networkStatusMonitor = NetworkStatusMonitor.getInstance();
    }

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    // Sync one reading for the day, if it is not yet in the cache or is in the current week.
    private void syncReading(LecturesController.WHAT what, AelfDate when, SyncResult syncResult) throws InterruptedException {
        // Do we need to refresh
        if (mController.isLecturesInCache(what, when, false)) {
            if (when.isWithin7NextDays()) {
                // We always load for this week to allow corrections made by volunteers to
                // eventually reach the phones.
                Log.i(TAG, what.urlName()+" for "+when.toIsoString()+" REFRESHING (<7 days)");
            } else {
                // This is more than a week ahead and we already have a version in the cache
                Log.i(TAG, what.urlName()+" for "+when.toIsoString()+" SKIPPED");
                return;
            }
        }

        // Load from the network
        try {
            Log.i(TAG, "Starting sync for " + what.urlName()+" for "+when.toIsoString());
            mController.loadLecturesFromNetwork(what, when);
        } catch (IOException e) {
            if (e.getCause() instanceof InterruptedException) {
                throw (InterruptedException) e.getCause();
            }
            Log.e(TAG, "I/O error while syncing");
            syncResult.stats.numIoExceptions++;
        }
    }

    // Sync all readings for the day
    private void syncDay(AelfDate when, int max, SyncResult syncResult) throws InterruptedException {
        syncReading(LecturesController.WHAT.INFORMATIONS, when, syncResult);
        while(max-- > 0) {
            LecturesController.WHAT what = LecturesController.WHAT.values()[max];
            syncReading(what, when, syncResult);
        }
    }

    private boolean revalidateConnection(boolean isManualSync, boolean wifiOnly) {
        // Has WiFi ? Always OK
        if (networkStatusMonitor.isWifiAvailable()) {
            return true;
        }

        // Requested only wifi and not manual ? Too bad...
        if (wifiOnly && !isManualSync) {
            return false;
        }

        // No hard constraint ? Good to go
        if (networkStatusMonitor.isNetworkAvailable()) {
            return true;
        }

        // Sorry. At least, that's better to know.
        return false;
    }

    /**
     * Pre-load readings for
     *  - yesterday
     *  - today
     *  - tomorrow
     *  - next Sunday
     */
    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {
        Log.i(TAG, "Beginning network synchronization");

        // Load preferences, but not yet there value
        SharedPreferences syncPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences syncStat = mContext.getSharedPreferences("sync-stats", Context.MODE_PRIVATE);

        // If this is not a manual sync and we are supposed to wait for a wifi network, wait for it
        boolean isManualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL);
        boolean wifiOnly = syncPref.getBoolean(SyncPrefActivity.KEY_PREF_SYNC_WIFI_ONLY, true);

        if (wifiOnly && !isManualSync) {
            // Wait for wifi
            Log.i(TAG, "This is a scheduled sync and users requested WiFi, checking network status");

            // check if we are already connected to a wifi OR wait for wifi
            try {
                networkStatusMonitor.waitForWifi();
            } catch (InterruptedException e) {
                // Reschedule and exit
                Log.w(TAG, "Interrupted while waiting for WiFi, schedule retry");
                syncResult.stats.numIoExceptions++;
                return;
            }

            Log.w(TAG, "WiFi is OK, let's sync");
        }

        // ** PREFS **

        // defaults
        Resources res = mContext.getResources();
        String pLectures = res.getString(R.string.pref_lectures_def);
        String pDuree    = res.getString(R.string.pref_duree_def);
        String pConserv  = res.getString(R.string.pref_conserv_def);

        // read preferences
        pLectures = syncPref.getString(SyncPrefActivity.KEY_PREF_SYNC_LECTURES, pLectures);
        pDuree    = syncPref.getString(SyncPrefActivity.KEY_PREF_SYNC_DUREE,    pDuree);
        pConserv  = syncPref.getString(SyncPrefActivity.KEY_PREF_SYNC_CONSERV,  pConserv);

        Log.i(TAG, "Pref lectures="+pLectures);
        Log.i(TAG, "Pref durée="+pDuree);
        Log.i(TAG, "Pref conservation="+pConserv);

        LecturesController controller = LecturesController.getInstance(this.getContext());

        // turn params into something usable
        int daysToSync = 0;
        // FIXME: -1 because 8 is Meta, which is always synced
        int whatMax = (pLectures.equals("messe-offices"))?LecturesController.WHAT.values().length-1:1;
        long currentTimeMillis = System.currentTimeMillis();

        switch (pDuree) {
            // "auj" and "auj-dim" are legacy, consider them as "semaine"
            case "auj":
            case "auj-dim":
            case "semaine":
                daysToSync += 7;
                break;
            case "mois":
                daysToSync += 31;
                break;
        }

        // ** SYNC **
        String errorName = "success";
        try {
            // Pre-Load 'daysToSync'. It is important to create a new date instance. Otherwise, all
            // future would be sharing the same date instance and save more or less on the same day
            // which is not quite good...
            for (int i = 0; i < daysToSync; i++) {
                // Compute remaining time budget
                long timeBudget = currentTimeMillis + MAX_RUN_TIME - System.currentTimeMillis();

                // Check error conditions
                if (syncResult.stats.numIoExceptions > 10) {
                    Log.w(TAG, "Too many errors, cancelling sync");
                    break;
                } else if (!revalidateConnection(isManualSync, wifiOnly)) {
                    Log.w(TAG, "Network went down, cancelling sync");
                    break;
                } else if (timeBudget < 0) {
                    Log.w(TAG, "Time budget exceeded, cancelling sync");
                    break;
                }

                // Actual sync
                AelfDate when = new AelfDate();
                when.add(Calendar.DATE, i);
                syncDay(when, whatMax, syncResult);
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Sync was interrupted, scheduling retry");
            syncResult.stats.numIoExceptions++;
        } catch (Exception e) {
            errorName = "error."+e.getClass().getName();
            throw e;
        }
        finally {

            // Track sync status
            Log.d(TAG, "Sync result: "+syncResult.toDebugString());
            if (syncResult.stats.numIoExceptions > 0) {
                errorName = "io";
            }

            // Internally track last sync data in DEDICATED store to avoid races with user set preferences (last write wins, hence a sync would overwrite any changes...)
            SharedPreferences.Editor editor = syncStat.edit();
            editor.putLong(SyncPrefActivity.KEY_APP_SYNC_LAST_ATTEMPT, currentTimeMillis);
            if (errorName.equals("success")) {
                editor.putLong(SyncPrefActivity.KEY_APP_SYNC_LAST_SUCCESS, currentTimeMillis);
            }
            editor.commit();
        }

        // ** CLEANUP **
        GregorianCalendar minConserv = new GregorianCalendar();
        switch (pConserv) {
            case "semaine":
                minConserv.add(Calendar.DATE, -7);
                break;
            case "mois":
                minConserv.add(Calendar.MONTH, -1);
                break;
            case "toujours":
                // let's be honest: If I keep all, users will kill me !
                minConserv.add(Calendar.YEAR, -1);
                break;
        }
        controller.truncateBefore(minConserv);
    }

    /**
     * Helpers: return the time in hours elapsed since the last sync attempt / success
     */
    private static long getHoursSincePreference(Context ctx, String preferenceName) {
        SharedPreferences syncStat = ctx.getSharedPreferences("sync-stats", Context.MODE_PRIVATE);
        long currentTimeMillis = System.currentTimeMillis();
        long lastSync = syncStat.getLong(preferenceName, -1);

        if (lastSync < 0) {
            return -1;
        }
        long lastSyncAge = (currentTimeMillis - lastSync) / 1000 / 3600;
        Log.d(TAG, "Last sync timestamp: "+lastSync+" age: "+lastSyncAge);
        return lastSyncAge;
    }

    public static long getLastSyncSuccessAgeHours(Context ctx) {
        return getHoursSincePreference(ctx, SyncPrefActivity.KEY_APP_SYNC_LAST_SUCCESS);
    }
}
