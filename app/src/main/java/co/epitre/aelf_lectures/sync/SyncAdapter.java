package co.epitre.aelf_lectures.sync;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.SyncPrefActivity;
import co.epitre.aelf_lectures.data.AelfDate;
import co.epitre.aelf_lectures.data.LecturesController;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.getsentry.raven.android.Raven;
import com.getsentry.raven.event.BreadcrumbBuilder;
import com.getsentry.raven.event.Breadcrumbs;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.extra.PiwikApplication;
import org.piwik.sdk.extra.TrackHelper;

// FIXME: this class is a *mess*. We need to rewrite it !

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "AELFSyncAdapter";
    private static final int SYNC_NOT_ID = 1;

    private Context mContext;
    private NotificationManager mNotificationManager;
    private LecturesController mController;

    private NotificationCompat.Builder mNotificationBuilder;

    private int mTodo;
    private int mDone;

    /**
     * Statistics
     */
    Tracker tracker;

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        Object service = context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.tracker = ((PiwikApplication) context.getApplicationContext()).getTracker();
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
        mNotificationManager.notify(SYNC_NOT_ID, mNotificationBuilder.build());
    }

    private void cancelNotification() {
        mNotificationManager.cancel(SYNC_NOT_ID);
    }

    private boolean isInCache(LecturesController.WHAT what, AelfDate when) {
        try {
            return mController.getLecturesFromCache(what, when, false) != null;
        } catch (IOException e) {
            return false;
        }
    }

    // Sync one reading for the day
    private void syncReading(LecturesController.WHAT what, AelfDate when, SyncResult syncResult) {
        // Load from network, if not in cache and not outdated
        if(!isInCache(what, when)) {
            try {
                mController.getLecturesFromNetwork(what, when);
            } catch (IOException e) {
                Log.e(TAG, "I/O error while syncing. AELF servers down ?");
                Raven.capture(e);
                syncResult.stats.numIoExceptions++;
            }
        }
        mDone++;
        updateNotification();
    }

    // Sync all readings for the day
    private void syncDay(AelfDate when, int max, SyncResult syncResult) {
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

        // ** PREFS **

        // defaults
        Resources res = mContext.getResources();
        String pLectures = res.getString(R.string.pref_lectures_def);
        String pDuree    = res.getString(R.string.pref_duree_def);
        String pConserv  = res.getString(R.string.pref_conserv_def);

        // read preferences
        SharedPreferences syncPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences syncStat = mContext.getSharedPreferences("sync-stats", Context.MODE_PRIVATE);
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
        GregorianCalendar whenMax = new GregorianCalendar();
        long currentTimeMillis = System.currentTimeMillis();

        switch (pDuree) {
            case "auj":
                // take tomorrow for free as well or we might be quite late if running at 23h50..
                whenMax.add(Calendar.DATE, 1);
                daysToSync += 1;
                break;
            case "auj-dim":
                daysToSync += 2;
                break;
            case "semaine":
                whenMax.add(Calendar.DATE, 7);
                daysToSync += 7;
                break;
            case "mois":
                whenMax.add(Calendar.DATE, 31);
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
            // loop until when > dayMax
            AelfDate when = new AelfDate();
            do {
                syncDay(when, whatMax, syncResult);
                when.add(Calendar.DATE, +1);
            } while(when.before(whenMax));

            // finally, do we need to explicitly grab next Sunday ?
            if(pDuree.equals("auj-dim")) {
                when = new AelfDate();
                do when.add(Calendar.DATE, +1); while (when.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY); // next Sunday
                syncDay(when, whatMax, syncResult);
            }
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
            TrackHelper.track().event("Office", "sync."+errorName).name(pLectures+"."+pDuree).value(1f).with(tracker);

            // Internally track last sync data in DEDICATED store to avoid races with user set preferences (last write wins, hence a sync would overwrite any changes...)
            SharedPreferences.Editor editor = syncStat.edit();
            editor.putLong(SyncPrefActivity.KEY_APP_SYNC_LAST_ATTEMPT, currentTimeMillis);
            if (errorName.equals("success")) {
                editor.putLong(SyncPrefActivity.KEY_APP_SYNC_LAST_SUCCESS, currentTimeMillis);
            }
            editor.apply();
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
        return (currentTimeMillis - lastSync) / 1000 / 3600;
    }

    public static long getLastSyncAttemptAgeMillis(Context ctx) {
        return getHoursSincePreference(ctx, SyncPrefActivity.KEY_APP_SYNC_LAST_ATTEMPT);
    }

    public static long getLastSyncSuccessAgeMillis(Context ctx) {
        return getHoursSincePreference(ctx, SyncPrefActivity.KEY_APP_SYNC_LAST_SUCCESS);
    }
}
