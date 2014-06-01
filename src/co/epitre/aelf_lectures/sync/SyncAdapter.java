package co.epitre.aelf_lectures.sync;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.SyncPrefActivity;
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

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String TAG = "AELFSyncAdapter";
    public static final int SYNC_NOT_ID = 1;

    Context mContext;
    NotificationManager mNotificationManager;
    LecturesController mController;

    NotificationCompat.Builder mNotificationBuilder;

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
    private void showNotification(int max, int cur) {
    	mNotificationBuilder.setProgress(max, cur, false);
        mNotificationManager.notify(SYNC_NOT_ID, mNotificationBuilder.build());
    }

    private void cancelNotification() {
        mNotificationManager.cancel(SYNC_NOT_ID);
    }

    // max is either
    //  - 1 --> messe(0) && metas (8)
    //  - len(offices)
    private void syncDay(GregorianCalendar when, int max) throws IOException {
		/*if(max == 1) { // FIXME: temporarily disable meta sync
			// that's terrible code... Should use a list of items to sync and changing the IDs has terrible side effects...
			// but at least we avoid doing it twice
			mController.getLectures(LecturesController.WHAT.METAS, when, false);
		}*/
        while(max-- > 0) {
        	LecturesController.WHAT what = LecturesController.WHAT.values()[max];
        	// make sure it is not in cache
        	if(mController.getLecturesFromCache(what, when) == null) {
        		mController.getLecturesFromNetwork(what, when);
        	}
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

        // ** PREFS **

        // defaults
        Resources res = mContext.getResources();
        String pLectures = res.getString(R.string.pref_lectures_def);
        String pDuree    = res.getString(R.string.pref_duree_def);
        String pConserv  = res.getString(R.string.pref_conserv_def);

        // read preferences
        SharedPreferences syncPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        pLectures = syncPref.getString(SyncPrefActivity.KEY_PREF_SYNC_LECTURES, pLectures);
        pDuree    = syncPref.getString(SyncPrefActivity.KEY_PREF_SYNC_DUREE,    pDuree);
        pConserv  = syncPref.getString(SyncPrefActivity.KEY_PREF_SYNC_CONSERV,  pConserv);

        Log.i(TAG, "Pref lectures="+pLectures);
        Log.i(TAG, "Pref durée="+pDuree);
        Log.i(TAG, "Pref conservation="+pConserv);

        LecturesController controller = LecturesController.getInstance(this.getContext());

        // turn params into something usable
        int todo = 1;
        int done = 0;
        int whatMax = (pLectures.equals("messe-offices"))?LecturesController.WHAT.values().length-1:1; // -1 --> FIXME: temporarily disable meta sync
        GregorianCalendar whenMax = new GregorianCalendar();

        if(pDuree.equals("auj")) {
        	// take tomorrow for free as well or we might be quite late if running at 23h50..
        	whenMax.add(Calendar.DATE, 1);
        	todo += 1;
        } else if(pDuree.equals("auj-dim")) {
        	todo += 2;
        } else if(pDuree.equals("semaine")) {
        	whenMax.add(Calendar.DATE, 7);
        	todo += 7;
        } else if(pDuree.equals("mois")) {
        	whenMax.add(Calendar.DATE, 31);
        	todo += 31;
        }

        // notify user
        this.showNotification(todo, done);

        // ** SYNC **
        try {
        	// loop until when > dayMax
        	GregorianCalendar when = new GregorianCalendar();
        	do {
        		syncDay(when, whatMax);
        		when.add(Calendar.DATE, +1);
        		this.showNotification(todo, ++done);
        	} while(when.before(whenMax));

        	// finally, do we need to explicitly grab next Sunday ?
        	if(pDuree.equals("auj-dim")) {
        		when = new GregorianCalendar();
        		do when.add(Calendar.DATE, +1); while (when.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY); // next Sunday
        		syncDay(when, whatMax);
        		this.showNotification(todo, ++done);
        	}
    	} catch (IOException e) {
    		// Aelf servers down ? It appends ...
    		Log.e(TAG, "I/O error while syncing. AELF servers down ?");
    		syncResult.delayUntil = 60L*15; // Wait 15min before retrying
    	} finally {
    		this.cancelNotification();
    	}

        // ** CLEANUP **
        GregorianCalendar minConserv = new GregorianCalendar();
        if(pConserv.equals("semaine")) {
        	minConserv.add(Calendar.DATE, -7);
        } else if (pConserv.equals("mois")) {
        	minConserv.add(Calendar.MONTH, -1);
    	} else if (pConserv.equals("toujours")) {
    		// let's be honest: If I keep all, users will kill me !
        	minConserv.add(Calendar.YEAR, -1);
    	}
        controller.truncateBefore(minConserv);

    }
}
