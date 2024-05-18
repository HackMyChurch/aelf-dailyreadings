package co.epitre.aelf_lectures.sync;

import static android.content.Context.ACCOUNT_SERVICE;
import static android.content.Context.ACTIVITY_SERVICE;

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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.components.NetworkStatusMonitor;
import co.epitre.aelf_lectures.lectures.data.AelfDate;
import co.epitre.aelf_lectures.lectures.data.LecturesController;
import co.epitre.aelf_lectures.lectures.data.OfficeTypes;
import co.epitre.aelf_lectures.lectures.data.cache.CacheEntries;
import co.epitre.aelf_lectures.lectures.data.cache.CacheEntry;
import co.epitre.aelf_lectures.lectures.data.cache.CacheEntryIndex;
import co.epitre.aelf_lectures.lectures.data.office.OfficesChecksums;
import co.epitre.aelf_lectures.settings.SettingsActivity;

// FIXME: this class is a *mess*. We need to rewrite it !

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "AELFSyncAdapter";
    static final int SYNC_WORKER_COUNT = 4;

    private Context mContext;
    private LecturesController mController;

    NetworkStatusMonitor networkStatusMonitor;

    private static final long MAX_RUN_TIME = 30*60;
    private static final long MAX_REQUEST_RUN_TIME = 60;

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
    private void syncReading(OfficeTypes what, AelfDate when, SyncResult syncResult, boolean isManualSync, boolean wifiOnly) {
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
        long start = SystemClock.elapsedRealtime();
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
        OfficeTypes[] whatList;
        if(pLectures.equals("messe-offices")) {
            whatList = OfficeTypes.values();
        } else {
            whatList = new OfficeTypes[]{OfficeTypes.MESSE, OfficeTypes.INFORMATIONS};
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
            // Initialize the worker pool
            ExecutorService executorService = Executors.newFixedThreadPool(SYNC_WORKER_COUNT);

            // Fetch 7 next days offices checksums in the background (can take up to 3-5s when the cache is cold on the server)
            FutureTask<OfficesChecksums> officesChecksumFetcher = new FutureTask<>(() -> {
                try {
                    return mController.loadOfficesChecksums(new AelfDate(), 7);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to fetch future offices checksums", e);
                    return null;
                }
            });
            executorService.execute(officesChecksumFetcher);

            // Get current cache status
            CacheEntries cachedOffices = mController.listCachedEntries(new AelfDate());

            // Schedule sync of all offices that are not yet in the cache
            for (int i = 0; i < daysToSync; i++) {
                // Enqueue sync tasks for the day
                AelfDate when = new AelfDate();
                when.add(Calendar.DATE, i);

                // Sync office and information for that day, if needed
                for (OfficeTypes what: whatList) {
                    // Do we need to sync ?
                    if (cachedOffices.containsKey(new CacheEntryIndex(what, when))) {
                        continue;
                    }

                    Log.i(TAG, what.urlName()+" for "+when.toIsoString()+" SCHEDULING INITIAL FETCH");
                    executorService.execute(() -> this.syncReading(what, when, syncResult, isManualSync, wifiOnly));
                }
            }

            // Schedule the REFRESH of the offices in the next 7 days if:
            // - we have a local copy
            // - this local copy is outdated
            // - we were able to query the remote server status
            OfficesChecksums serverChecksums;
            try {
                serverChecksums = officesChecksumFetcher.get(MAX_REQUEST_RUN_TIME, TimeUnit.SECONDS);
            } catch (Exception e) {
                Log.e(TAG, "Failed to query server-side offices checksums. Ignoring.", e);
                serverChecksums = null;
            }

            for (int i = 0; i < 7 && serverChecksums != null; i++) {
                // Enqueue sync tasks for the day
                AelfDate when = new AelfDate();
                when.add(Calendar.DATE, i);

                // Sync office and information for that day, if needed
                for (OfficeTypes what: whatList) {
                    // Only if we have a cache entry (otherwise the loop above already fetched it)
                    CacheEntry cacheEntry = cachedOffices.get(new CacheEntryIndex(what, when));
                    if (cacheEntry == null) {
                        continue;
                    }

                    // Only if we have a checksum for this day
                    String serverChecksum = serverChecksums.getOfficeChecksum(what, when);
                    if (serverChecksum == null) {
                        continue;
                    }

                    // And if the checksum is different than the one in cache
                    if (cacheEntry.checksum.equals(serverChecksum)) {
                        continue;
                    }

                    // Schedule refresh
                    Log.i(TAG, what.urlName()+" for "+when.toIsoString()+" SCHEDULING REFRESH (outdated)");
                    executorService.execute(() -> this.syncReading(what, when, syncResult, isManualSync, wifiOnly));
                }
            }

            // Execute all tasks and wait for completion
            executorService.shutdown();
            boolean success_within_time = executorService.awaitTermination(MAX_RUN_TIME, TimeUnit.SECONDS);

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
        long stop = SystemClock.elapsedRealtime();
        Log.i(TAG, "Sync duration: "+(stop-start)/1000.0+"s");
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
