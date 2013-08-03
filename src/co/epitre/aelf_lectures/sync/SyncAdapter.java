package co.epitre.aelf_lectures.sync;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import data.LecturesController;
import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	public static final String TAG = "SyncAdapter";
	// Global variables
    // Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;
    
    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }
    
    /*
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
    	
    	LecturesController controller = LecturesController.getInstance(this.getContext());
    	
    	try {
    		GregorianCalendar date = new GregorianCalendar(); // today
			controller.getLectures(LecturesController.WHAT.MESSE, date);
			date.add(Calendar.DATE, -1); // yesterday
	    	controller.getLectures(LecturesController.WHAT.MESSE, date);
	    	date.add(Calendar.DATE, +2); // tomorrow
	    	controller.getLectures(LecturesController.WHAT.MESSE, date);
	    	while (date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) date.add(Calendar.DATE, +1); // next Sunday
	    	controller.getLectures(LecturesController.WHAT.MESSE, date);
		} catch (IOException e) {
			// Aelf servers down ? It appends ...
			syncResult.delayUntil = 60L*15; // Wait 15min before retrying
		}
    	
    	
    }
}
