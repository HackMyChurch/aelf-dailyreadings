package co.epitre.aelf_lectures.sync;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import co.epitre.aelf_lectures.components.NetworkStatusMonitor;
import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.lectures.data.AelfDate;
import co.epitre.aelf_lectures.lectures.data.CacheEntryIndex;
import co.epitre.aelf_lectures.lectures.data.CacheEntryIndexes;
import co.epitre.aelf_lectures.lectures.data.LecturesController;
import co.epitre.aelf_lectures.settings.SettingsActivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import static android.content.Context.ACCOUNT_SERVICE;
import static android.content.Context.ACTIVITY_SERVICE;

// FIXME: this class is a *mess*. We need to rewrite it !

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "AELFSyncAdapter";
    static final int SYNC_WORKER_COUNT = 4;

    private Context mContext;
    private LecturesController mController;

    NetworkStatusMonitor networkStatusMonitor;

    private static final long MAX_RUN_TIME = 30;

    /**
     * Sync account related vars
     */
    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = "co.epitre.aelf"; // DANGER: must be the same as the provider's in the manifest
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "epitre.co";
    // The account name
    public static final String ACCOUNT = "www.aelf.org";
    // Sync interval in s. ~ 1 Day
    public static final long SYNC_INTERVAL = 60L * 60L * 22L;

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
    private void syncReading(LecturesController.WHAT what, AelfDate when, SyncResult syncResult, boolean isManualSync, boolean wifiOnly) {
        // Check pre-requisites
        if (syncResult.stats.numIoExceptions > 10) {
            Log.w(TAG, "Too many errors, cancelling sync");
            return;
        } else if (!revalidateConnection(isManualSync, wifiOnly)) {
            Log.w(TAG, "Network went down, cancelling sync");
            return;
        }

        // Load from the network
        try {
            Log.i(TAG, "Starting sync for " + what.urlName()+" for "+when.toIsoString());
            mController.loadLecturesFromNetwork(what, when);
        } catch (IOException e) {
            Log.e(TAG, "I/O error while loading "+what.urlName()+"/"+when.toIsoString()+": "+e, e);
            syncResult.stats.numIoExceptions++;
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
        return networkStatusMonitor.isNetworkAvailable();
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
        boolean wifiOnly = syncPref.getBoolean(SettingsActivity.KEY_PREF_SYNC_WIFI_ONLY, false);

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
        pLectures = syncPref.getString(SettingsActivity.KEY_PREF_SYNC_LECTURES, pLectures);
        pDuree    = syncPref.getString(SettingsActivity.KEY_PREF_SYNC_DUREE,    pDuree);
        pConserv  = syncPref.getString(SettingsActivity.KEY_PREF_SYNC_CONSERV,  pConserv);

        Log.i(TAG, "Pref lectures="+pLectures);
        Log.i(TAG, "Pref durée="+pDuree);
        Log.i(TAG, "Pref conservation="+pConserv);

        LecturesController controller = LecturesController.getInstance(this.getContext());

        // Build the list of office to sync
        LecturesController.WHAT[] whatList;
        if(pLectures.equals("messe-offices")) {
            whatList = LecturesController.WHAT.values();
        } else {
            whatList = new LecturesController.WHAT[]{LecturesController.WHAT.MESSE, LecturesController.WHAT.INFORMATIONS};
        }

        // Compute the sync period length
        int daysToSync = 0;
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
            // Get current cache status
            CacheEntryIndexes cachedOffices = mController.listCachedEntries(new AelfDate());

            // Initialize the worker pool
            ExecutorService executorService = Executors.newFixedThreadPool(SYNC_WORKER_COUNT);

            // Pre-Load 'daysToSync'. It is important to create a new date instance. Otherwise, all
            // future would be sharing the same date instance and save more or less on the same day
            // which is not quite good...
            for (int i = 0; i < daysToSync; i++) {
                // Enqueue sync tasks for the day
                AelfDate when = new AelfDate();
                when.add(Calendar.DATE, i);

                // Sync office and information for that day, if needed
                for (LecturesController.WHAT what: whatList) {
                    // Do we need to refresh ?
                    if (cachedOffices.contains(new CacheEntryIndex(what, when))) {
                        if (when.isWithin7NextDays()) {
                            // We always load for this week to allow corrections made by volunteers to
                            // eventually reach the phones.
                            Log.i(TAG, what.urlName()+" for "+when.toIsoString()+" SCHEDULING REFRESH (<7 days)");
                        } else {
                            // This is more than a week ahead and we already have a version in the cache
                            Log.i(TAG, what.urlName()+" for "+when.toIsoString()+" SKIPPED");
                            continue;
                        }
                    }
                    executorService.execute(() -> this.syncReading(what, when, syncResult, isManualSync, wifiOnly));
                }
            }

            // Execute all tasks and wait for completion
            executorService.shutdown();
            boolean success_within_time = executorService.awaitTermination(MAX_RUN_TIME, TimeUnit.MINUTES);

            if(!success_within_time) {
                Log.w(TAG, "Time budget exceeded, cancelling sync");
                throw new InterruptedException("Time budget exceeded, cancelling sync");
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
            long currentTimeMillis = System.currentTimeMillis();
            editor.putLong(SettingsActivity.KEY_APP_SYNC_LAST_ATTEMPT, currentTimeMillis);
            if (errorName.equals("success")) {
                editor.putLong(SettingsActivity.KEY_APP_SYNC_LAST_SUCCESS, currentTimeMillis);
            }
            editor.commit();
        }

        // ** CLEANUP **
        AelfDate minConserv = new AelfDate();
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

        // Finish sync, ensure that next sync will get a clean preference copy
        System.exit(0);
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
        return getHoursSincePreference(ctx, SettingsActivity.KEY_APP_SYNC_LAST_SUCCESS);
    }

    public static Account getSyncAccount(Context ctx) {
        Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) ctx.getSystemService(ACCOUNT_SERVICE);

        // Create the account explicitly. If account creation fails, it means that it already exists.
        // In this case, keep and return the dummy instance. We'll need to trigger manual sync
        try {
            accountManager.addAccountExplicitly(newAccount, null, null);
        } catch (SecurityException e) {
            // This should not fail BUT, there are reports on Android 6.0
            Log.e(TAG, "getSyncAccount: SecurityException while creating account", e);
        }
        return newAccount;
    }

    public static void configureSync(Context ctx) {
        Account account = getSyncAccount(ctx);

        ContentResolver.setIsSyncable(account, AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
        ContentResolver.addPeriodicSync(account, AUTHORITY, new Bundle(1), SYNC_INTERVAL);
    }

    public static void triggerSync(Context ctx) {
        Account account = getSyncAccount(ctx);

        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        // start sync
        ContentResolver.requestSync(account, AUTHORITY, settingsBundle);
    }

    // If there is any sync in progress, terminate it. This allows the sync engine to pick up any
    // important preference changes
    // TODO: use some sort of signaling instead...
    public static void killPendingSyncs(Context ctx) {
        // Is there a sync in progress ?
        if (ContentResolver.getCurrentSyncs().isEmpty()) {
            // There is no sync in progress
            return;
        }

        // Kill any background processes
        ActivityManager am = (ActivityManager)ctx.getSystemService(ACTIVITY_SERVICE);
        String packageName = ctx.getPackageName();
        if (packageName != null && am != null) {
            am.killBackgroundProcesses(packageName);
        }

        // Cancel sync
        Account account = getSyncAccount(ctx);
        ContentResolver.cancelSync(account, AUTHORITY);
    }
}
