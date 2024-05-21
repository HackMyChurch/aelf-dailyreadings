package co.epitre.aelf_lectures.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.lectures.data.AelfDate;
import co.epitre.aelf_lectures.lectures.data.LecturesController;
import co.epitre.aelf_lectures.lectures.data.OfficeTypes;
import co.epitre.aelf_lectures.lectures.data.cache.CacheEntries;
import co.epitre.aelf_lectures.lectures.data.cache.CacheEntry;
import co.epitre.aelf_lectures.lectures.data.cache.CacheEntryIndex;
import co.epitre.aelf_lectures.lectures.data.office.OfficeChecksum;
import co.epitre.aelf_lectures.lectures.data.office.OfficesChecksums;
import co.epitre.aelf_lectures.settings.SettingsActivity;

/**
 * This class is the core of the sync engine. This is where the actual requests are triggered.
 */
public class SyncWorker extends Worker {
    // General configuration
    private static final String TAG = "SyncWorker";
    private static final int SYNC_WORKER_COUNT = 4;
    private static final long MAX_RUN_TIME = 30*60;
    private static final long MAX_REQUEST_RUN_TIME = 60;

    // Resources
    private final Resources mResources;
    private final LecturesController mController;
    private final SharedPreferences mSyncPrefs;
    private final SharedPreferences mSyncStats;

    // Status
    private final AtomicInteger errors = new AtomicInteger();
    private static final Object syncLock = new Object();

    public SyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);

        // Load resources
        this.mController = LecturesController.getInstance(context);
        this.mResources = context.getResources();

        // Load preferences
        this.mSyncPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.mSyncStats = context.getSharedPreferences("sync-stats", Context.MODE_PRIVATE);
    }

    private String getStringPreference(String key, int defId) {
        String default_value = this.mResources.getString(defId);
        return this.mSyncPrefs.getString(key, default_value);
    }

    private OfficeTypes[] getOfficeTypesToSync() {
        String configValue = this.getStringPreference(SettingsActivity.KEY_PREF_SYNC_LECTURES, R.string.pref_lectures_def);
        if(configValue.equals("messe-offices")) {
            return OfficeTypes.values();
        } else {
            return new OfficeTypes[]{OfficeTypes.MESSE, OfficeTypes.INFORMATIONS};
        }
    }

    private int getDaysAheadToSync() {
        String configValue = this.getStringPreference(SettingsActivity.KEY_PREF_SYNC_DUREE, R.string.pref_duree_def);
        return switch (configValue) {
            case "auj", "auj-dim", "semaine" -> 7;
            default -> 31;
        };
    }

    private AelfDate getRetentionCutoffDate() {
        String configValue = this.getStringPreference(SettingsActivity.KEY_PREF_SYNC_CONSERV, R.string.pref_conserv_def);
        AelfDate retentionCutoffDate = new AelfDate();
        switch (configValue) {
            case "semaine":
                retentionCutoffDate.add(Calendar.DATE, -7);
                break;
            case "mois":
                retentionCutoffDate.add(Calendar.MONTH, -1);
                break;
            case "toujours":
                retentionCutoffDate.add(Calendar.YEAR, -1);
                break;
        }
        return retentionCutoffDate;
    }

    private void syncReading(OfficeTypes what, AelfDate when) {
        try {
            Log.i(TAG, "Starting sync for " + what.urlName()+" for "+when.toIsoString());
            mController.loadLecturesFromNetwork(what, when);
        } catch (IOException e) {
            Log.e(TAG, "I/O error while loading "+what.urlName()+"/"+when.toIsoString(), e);
            errors.incrementAndGet();
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        // We must guarantee that a single sync worker is running (expected vs periodic)
        synchronized(syncLock) {
            return synchronize();
        }
    }

    @NonNull
    private Result synchronize() {
        long start = SystemClock.elapsedRealtime();
        Log.i(TAG, "Beginning synchronization");

        // Load parameters
        OfficeTypes[] officeTypesToSync = this.getOfficeTypesToSync();
        int daysAheadToSync = this.getDaysAheadToSync();
        AelfDate retentionCutoffDate = this.getRetentionCutoffDate();

        Log.i(TAG, "Pref lectures="+Arrays.toString(officeTypesToSync));
        Log.i(TAG, "Pref durée="+daysAheadToSync+" days ahead");
        Log.i(TAG, "Pref conservation_cutoff="+retentionCutoffDate.toIsoString());

        // Initialize the worker pool
        ExecutorService executorService = Executors.newFixedThreadPool(SYNC_WORKER_COUNT);

        // Schedule task to load this week's offices checksum (up to 5 sec when the server cache is cold)
        FutureTask<OfficesChecksums> officesChecksumFetcher = new FutureTask<>(this::loadOfficesChecksums);
        executorService.execute(officesChecksumFetcher);

        // Get current cache status
        CacheEntries cachedOffices = mController.listCachedEntries(new AelfDate());

        // Enqueue initial synchronisation tasks
        enqueueInitialFetchTasks(daysAheadToSync, officeTypesToSync, cachedOffices, executorService);

        // Enqueue refresh tasks
        enqueueCacheRefreshTasks(officesChecksumFetcher, officeTypesToSync, cachedOffices, executorService);

        // Complete synchronisation
        try {
            // Execute all tasks and wait for completion
            executorService.shutdown();
            boolean success_within_time = executorService.awaitTermination(MAX_RUN_TIME, TimeUnit.SECONDS);

            if(!success_within_time) {
                Log.w(TAG, "Time budget exceeded, cancelling sync");
                throw new InterruptedException("Time budget exceeded, cancelling sync");
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Sync was interrupted, scheduling retry");
            this.errors.incrementAndGet();
        } finally {
            long stop = SystemClock.elapsedRealtime();
            Log.i(TAG, "Sync duration: "+(stop-start)/1000.0+"s");

            SharedPreferences.Editor editor = this.mSyncStats.edit();
            editor.putLong(SettingsActivity.KEY_APP_SYNC_LAST_ATTEMPT, stop);

            if (this.errors.get() == 0) {
                editor.putLong(SettingsActivity.KEY_APP_SYNC_LAST_SUCCESS, stop);
            }

            editor.apply();
        }

        // Cleanup older readings
        this.mController.truncateBefore(retentionCutoffDate);

        // Report status
        if (errors.get() > 0) {
            return Result.retry();
        }
        return Result.success();
    }

    private OfficesChecksums loadOfficesChecksums() {
        try {
            return mController.loadOfficesChecksums(new AelfDate(), 7);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch future offices checksums", e);
            return null;
        }
    }

    private void enqueueInitialFetchTasks(int daysAheadToSync, OfficeTypes[] officeTypesToSync, CacheEntries cachedOffices, ExecutorService executorService) {
        for (int i = 0; i < daysAheadToSync; i++) {
            // Enqueue sync tasks for the day
            AelfDate when = new AelfDate();
            when.add(Calendar.DATE, i);

            // Sync office and information for that day, if needed
            for (OfficeTypes what: officeTypesToSync) {
                // Do we need to sync ?
                if (cachedOffices.containsKey(new CacheEntryIndex(what, when))) {
                    continue;
                }

                Log.i(TAG, what.urlName()+" for "+when.toIsoString()+" SCHEDULING INITIAL FETCH");
                executorService.execute(() -> this.syncReading(what, when));
            }
        }
    }

    private void enqueueCacheRefreshTasks(FutureTask<OfficesChecksums> officesChecksumFetcher, OfficeTypes[] officeTypesToSync, CacheEntries cachedOffices, ExecutorService executorService) {
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
            for (OfficeTypes what: officeTypesToSync) {
                // Only if we have a cache entry (otherwise the loop above already fetched it)
                CacheEntry cacheEntry = cachedOffices.get(new CacheEntryIndex(what, when));
                if (cacheEntry == null) {
                    continue;
                }

                // Only if we have a checksum for this day
                OfficeChecksum serverChecksum = serverChecksums.getOfficeChecksum(what, when);
                if (serverChecksum == null) {
                    continue;
                }

                // And if the checksum is different than the one in cache
                if (cacheEntry.checksum().equals(serverChecksum.checksum())) {
                    continue;
                }

                // Finally, only update if the server entry is more recent
                if(serverChecksum.generationDate().isValid() && serverChecksum.generationDate().before(cacheEntry.creationDate())) {
                    continue;
                }

                // Schedule refresh
                Log.i(TAG, what.urlName()+" for "+when.toIsoString()+" SCHEDULING REFRESH (outdated)");
                executorService.execute(() -> this.syncReading(what, when));
            }
        }
    }
}
