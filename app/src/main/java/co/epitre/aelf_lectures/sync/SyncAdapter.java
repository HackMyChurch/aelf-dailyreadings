package co.epitre.aelf_lectures.sync;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import co.epitre.aelf_lectures.LecturesApplication;
import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.SyncPrefActivity;
import co.epitre.aelf_lectures.data.AelfDate;
import co.epitre.aelf_lectures.data.LectureFuture;
import co.epitre.aelf_lectures.data.LecturesController;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.getsentry.raven.android.Raven;
import com.getsentry.raven.event.BreadcrumbBuilder;
import com.getsentry.raven.event.Breadcrumbs;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;

// FIXME: this class is a *mess*. We need to rewrite it !

// Monitor network state change. In particular wait for the wifi to be connected
class NetworkReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkReceiver";
    public final Object whenNetworkOk = new Object();

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();

        // Was the WiFi enabled ?
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.i(TAG, "System tells us that WiFi was just connected");
            synchronized (whenNetworkOk) {
                whenNetworkOk.notifyAll();
            }
        }
    }
}

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "AELFSyncAdapter";

    private Context mContext;
    private NotificationManager mNotificationManager;
    private LecturesController mController;

    private NotificationCompat.Builder mNotificationBuilder;

    private LinkedList<LectureFuture> pendingDownloads = new LinkedList<>();
    private int mTodo;
    private int mDone;

    private static final long MAX_RUN_TIME = TimeUnit.MINUTES.toMillis(30);

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        Object service = context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.mNotificationManager = (NotificationManager) service;
        this.mContext = context;
        this.mController = LecturesController.getInstance(this.getContext());

        PendingIntent intent =
                PendingIntent.getActivity(
                mContext,
                0,
                new Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT
            );

        mNotificationBuilder = new NotificationCompat.Builder(mContext)
            .setContentTitle("AELF")
            .setContentText("Pré-chargement des lectures...")
            .setContentIntent(intent)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true);
    }

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    // code to display a sync notification
    // http://stackoverflow.com/questions/5061760/how-does-one-animate-the-android-sync-status-icon
    private void updateNotification() {
        mNotificationBuilder.setProgress(mTodo, mDone, false);
        mNotificationManager.notify(LecturesApplication.NOTIFICATION_SYNC_PROGRESS, mNotificationBuilder.build());
    }

    private void cancelNotification() {
        mNotificationManager.cancel(LecturesApplication.NOTIFICATION_SYNC_PROGRESS);
    }

    // Sync one reading for the day
    private void syncReading(LecturesController.WHAT what, AelfDate when, SyncResult syncResult) throws InterruptedException {
        // Load from network, if not in cache and not outdated
        if(!mController.isLecturesInCache(what, when, false)) {
            try {
                Log.i(TAG, what.urlName()+" for "+when.toIsoString()+" QUEUED");
                pendingDownloads.add(mController.getLecturesFromNetwork(what, when));
            } catch (IOException e) {
                if (e.getCause() instanceof InterruptedException) {
                    throw (InterruptedException) e.getCause();
                }
                // Error already propagated to Sentry. Do not propagate twice !
                Log.e(TAG, "I/O error while syncing");
                syncResult.stats.numIoExceptions++;
            }
        } else {
            Log.i(TAG, what.urlName()+" for "+when.toIsoString()+" SKIPPED");
        }
    }

    // Sync all readings for the day
    private void syncDay(AelfDate when, int max, SyncResult syncResult) throws InterruptedException {
        syncReading(LecturesController.WHAT.METAS, when, syncResult);
        while(max-- > 0) {
            LecturesController.WHAT what = LecturesController.WHAT.values()[max];
            syncReading(what, when, syncResult);
        }
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
        Breadcrumbs.record(new BreadcrumbBuilder().setMessage("Starting scheduled background sync").build());

        // Load preferences, but not yet there value
        SharedPreferences syncPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences syncStat = mContext.getSharedPreferences("sync-stats", Context.MODE_PRIVATE);

        // If this is not a manual sync and we are supposed to wait for a wifi network, wait for it
        boolean isManualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL);
        boolean wifiOnly = syncPref.getBoolean(SyncPrefActivity.KEY_PREF_SYNC_WIFI_ONLY, true);
        if (wifiOnly && !isManualSync) {
            // Wait for wifi
            Log.i(TAG, "This is a scheduled sync and users requested WiFi, checking network status");

            // Set up a network state change listener
            NetworkReceiver networkReceiver = new NetworkReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(CONNECTIVITY_ACTION);
            // intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
            mContext.registerReceiver(networkReceiver, intentFilter);

            // check if we are already connected to a wifi OR wait for wifi
            try {
                ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                while (activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) {
                    Log.w(TAG, "WiFi is not connected, waiting for it");
                    synchronized (networkReceiver.whenNetworkOk) {
                        networkReceiver.whenNetworkOk.wait();
                    }

                    // Refresh network status
                    activeNetwork = cm.getActiveNetworkInfo();
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting for WiFi, schedule retry");

                // Reschedule and exit
                syncResult.stats.numIoExceptions++;
                return;
            } finally {
                // unregister listener
                mContext.unregisterReceiver(networkReceiver);
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
            case "auj":
                // take tomorrow for free as well or we might be quite late if running at 23h50..
                daysToSync += 1;
                break;
            case "auj-dim":
                daysToSync += 2;
                break;
            case "semaine":
                daysToSync += 7;
                break;
            case "mois":
                daysToSync += 31;
                break;
        }

        mTodo = daysToSync * (whatMax+1); // all readings + meta for all days
        mDone = 0;

        // notify user
        updateNotification();

        // ** SYNC **
        String errorName = "success";
        try {
            // Pre-Load 'daysToSync'. It is important to create a new date instance. Otherwise, all
            // future would be sharing the same date instance and save more or less on the same day
            // which is not quite good...
            for (int i = 0; i < daysToSync; i++) {
                AelfDate when = new AelfDate();
                when.add(Calendar.DATE, i);
                syncDay(when, whatMax, syncResult);
            }

            // Load next sunday
            if (pDuree.equals("auj-dim")) {
                AelfDate when = new AelfDate();
                do when.add(Calendar.DATE, +1);
                while (when.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY); // next Sunday
                syncDay(when, whatMax, syncResult);
            }

            // Wait for the downloads
            while (!pendingDownloads.isEmpty()) {
                // Compute remaining time budget, set min to 1 to give a chance to already completed tasks
                // to be collected
                long timeBudget = currentTimeMillis + MAX_RUN_TIME - System.currentTimeMillis();
                timeBudget = Math.max(timeBudget, 1);

                LectureFuture future = pendingDownloads.pop();
                try {
                    future.get(timeBudget, TimeUnit.MILLISECONDS);
                    Log.i(TAG, future.what.urlName() + " for " + future.when.toIsoString() + " SUCCESS !!");
                    mDone++;
                } catch (InterruptedException e) {
                    throw e;
                } catch (TimeoutException e) {
                    Log.e(TAG, "Sync time budget exceeded, cancelling");
                    future.cancel(true);
                    Raven.capture(e);
                    syncResult.stats.numIoExceptions++;
                    mDone++;
                } catch (ExecutionException e) {
                    // This is actually just a wrapped IOException
                    Log.e(TAG, "I/O error while syncing");
                    syncResult.stats.numIoExceptions++;
                }

                updateNotification();
            }
        } catch (InterruptedException e) {
            Log.i(TAG, "Sync was interrupted, scheduling retry");
            syncResult.stats.numIoExceptions++;
        } catch (Exception e) {
            Raven.capture(e);
            errorName = "error."+e.getClass().getName();
            throw e;
        }
        finally {
            // Mark sync as done as far as the user is concerned
            this.cancelNotification();

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

    public static long getLastSyncAttemptAgeHours(Context ctx) {
        return getHoursSincePreference(ctx, SyncPrefActivity.KEY_APP_SYNC_LAST_ATTEMPT);
    }

    public static long getLastSyncSuccessAgeHours(Context ctx) {
        return getHoursSincePreference(ctx, SyncPrefActivity.KEY_APP_SYNC_LAST_SUCCESS);
    }
}
