package co.epitre.aelf_lectures;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.view.ViewPager;
import android.support.v4.view.WindowCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import co.epitre.aelf_lectures.data.LectureItem;
import co.epitre.aelf_lectures.data.LecturesController;
import co.epitre.aelf_lectures.data.LecturesController.WHAT;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

class WhatWhen {
    public LecturesController.WHAT what;
    public GregorianCalendar when;
    public int position;
    public boolean useCache = true;
}

public class LecturesActivity extends SherlockFragmentActivity implements DatePickerFragment.CalendarDialogListener,
                                                                  ActionBar.OnNavigationListener {

	public static final String TAG = "AELFLecturesActivity";
    public static final String PREFS_NAME = "aelf-prefs";
    public static final long DATE_TODAY = 0;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    LecturesController lecturesCtrl = null;
    SectionsPagerAdapterFragment mSectionsPagerAdapter;
    WhatWhen whatwhen;
    Menu mMenu;

    List<LectureItem> networkError = new ArrayList<LectureItem>(1);

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
    public static final long SYNC_INTERVAL = 60L*60L*22L;
    // Instance fields
    Account mAccount;
    
    // action bar
    protected ActionBar actionBar;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    // TODO: detect first launch + trigger initial sync

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

    	// ---- need upgrade ?
    	int currentVersion, savedVersion;

    	// current version
    	PackageInfo packageInfo;
    	try {
    		packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
    	} catch (NameNotFoundException e) {
    		throw new RuntimeException("Could not determine current version");
    	}
    	currentVersion = packageInfo.versionCode;

    	// load saved version, if any
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        savedVersion = settings.getInt("version", -1);

    	// upgrade logic, primitive at the moment...
    	if(savedVersion != currentVersion) {
    		if(savedVersion < 11) {
    			// delete cache DB: needs to force regenerate
    			getApplicationContext().deleteDatabase("aelf_cache.db");
    		}

    		// update saved version
    		SharedPreferences.Editor editor = settings.edit();
    		editor.putInt("version", currentVersion);
    		editor.commit();
    	}
    	// ---- end upgrade

    	// create dummy account for our background sync engine
    	mAccount = CreateSyncAccount(this);

    	// init the lecture controller
    	lecturesCtrl = LecturesController.getInstance(this);

    	// need restore state ?
    	whatwhen = new WhatWhen();
    	if(savedInstanceState != null) {
        	// Restore saved instance state. Especially useful on screen rotate on older phones
            whatwhen.what = WHAT.values()[savedInstanceState.getInt("what")];
            whatwhen.position = savedInstanceState.getInt("position");
            
            long timestamp = savedInstanceState.getLong("when");
            whatwhen.when = new GregorianCalendar();
            if(timestamp != DATE_TODAY) {
            	whatwhen.when.setTimeInMillis(timestamp);
            }
    	} else {
	    	// load the "lectures" for today
	    	whatwhen.when = new GregorianCalendar();
	    	whatwhen.what = WHAT.MESSE;
	    	whatwhen.position = 0; // 1st lecture of the office
    	}

    	// error handler
    	networkError.add(new LectureItem("Erreur Réseau", "<p>Connexion au serveur AELF impossible<br />Veuillez ré-essayer plus tard.</p>", "erreur"));
    	
    	// TODO: explore possible overlay action bar lifecycle
    	//requestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
    	
    	// some UI. Most UI init are done in the prev async task
    	setContentView(R.layout.activity_lectures);
    	
    	// prevent phone sleep
    	findViewById(android.R.id.content).setKeepScreenOn(true);

    	// Spinner
        actionBar = getSupportActionBar();

        Context context = actionBar.getThemedContext();
        ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(context, R.array.spinner, R.layout.sherlock_spinner_item);
        list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(list, this);

        // restore active navigation item
        actionBar.setSelectedNavigationItem(whatwhen.what.getPosition());

    	// finally, turn on periodic lectures caching
    	if(mAccount != null) {
    		ContentResolver.setIsSyncable(mAccount, AUTHORITY, 1);
    		ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);
    		ContentResolver.addPeriodicSync(mAccount, AUTHORITY, new Bundle(1), SYNC_INTERVAL);
    	}
    }
    
    @SuppressLint("NewApi")
    public void prepare_fullscreen() {
    	// Hide status (top) bar. Navigation bar (> 4.0) still visible.
    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

    	if (android.os.Build.VERSION.SDK_INT < 14) // 4.0 min
    		return;
    	
    	// Android 4.0+: make navigation bar 'discret' ('dots' instead of icons)
    	int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE;
    	
    	// for android.os.Build.VERSION.SDK_INT >= 16 (4.1)
    	// we should explore
    	//  - navbar workflow.    --> hide with View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    	//  - actionbar workflow. --> hide with View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    	// BUT: navbar shows automatically and dont hide :(
    	//      actionbar does not show. (actionBar.hide/show();)
    	// an idea is to deal with scroll events on the UI.

    	// Android 4.4+: hide navigation bar, make it accessible via edge scroll
    	if (android.os.Build.VERSION.SDK_INT >= 19) {
	    	uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
	    			  |  View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    	}

    	// apply settings
    	View decorView = getWindow().getDecorView();
    	
    	decorView.setSystemUiVisibility(uiOptions);
  	
    	// TODO: explore artificially reducing luminosity
    	/*try {
    		int brightness = android.provider.Settings.System.getInt(
    	    getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
    		Log.v(TAG, "brightness="+brightness);
    	} catch (SettingNotFoundException e) {
    	    // don't care
    	}*/
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	// manage application's intrusiveness for different Android versions
    	super.onWindowFocusChanged(hasFocus);
    	
    	if(hasFocus)
    		prepare_fullscreen();
    }
    
    private boolean isSameDay(GregorianCalendar when, GregorianCalendar other){
    	return (when.get(GregorianCalendar.ERA) == other.get(GregorianCalendar.ERA) &&
                when.get(GregorianCalendar.YEAR) == other.get(GregorianCalendar.YEAR) &&
                when.get(GregorianCalendar.DAY_OF_YEAR) == other.get(GregorianCalendar.DAY_OF_YEAR));
    }
    
    private boolean isToday(GregorianCalendar when){
    	GregorianCalendar today = new GregorianCalendar();
    	return isSameDay(when, today);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	// Save instance state. Especially useful on screen rotate on older phones
    	// - what --> mass, tierce, ... ? (id)
    	// - when --> date (timestamp)
    	// - position --> active tab (id)
    	super.onSaveInstanceState(outState);
    	
    	if(outState == null) return;
    	
    	int position = 0; // first slide by default
    	int what = 0; // "Messe" by default
    	long when = DATE_TODAY;
    	
    	if(whatwhen != null) {
    		if(whatwhen.what != null) what = whatwhen.what.getPosition();
    		if(mViewPager    != null) position = mViewPager.getCurrentItem();
    		if(whatwhen.when != null && !isToday(whatwhen.when)) {
    			when = whatwhen.when.getTimeInMillis();
    		}
    	}
    	
    	outState.putInt("what", what);
    	outState.putInt("position", position);
    	outState.putLong("when", when);
    }

    public boolean onAbout(MenuItem item) {
    	AboutDialogFragment aboutDialog = new AboutDialogFragment();
    	aboutDialog.show(getSupportFragmentManager(), "aboutDialog");
    	return true;
    }

    public boolean onSyncPref(MenuItem item) {
    	Intent intent = new Intent(this, SyncPrefActivity.class);
    	startActivity(intent);
    	return true;
    }

    public boolean onSyncDo(MenuItem item) {
        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        // start sync
        ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);

        // done
    	return true;
    }
    
    public boolean onRefresh(MenuItem item) {
    	whatwhen.useCache = false;
    	whatwhen.position = mViewPager.getCurrentItem();
    	new DownloadXmlTask().execute(whatwhen);
    	return true;
    }

    public boolean onCalendar(MenuItem item) {
    	Bundle args = new Bundle();
    	args.putLong("time", whatwhen.when.getTimeInMillis());

    	DatePickerFragment calendarDialog = new DatePickerFragment();
    	calendarDialog.setArguments(args);
        calendarDialog.show(getSupportFragmentManager(), "datePicker");

        return true;
    }

    @SuppressLint("SimpleDateFormat") // I know but currently French only
    public void onCalendarDialogPicked(int year, int month, int day) {
    	GregorianCalendar date = new GregorianCalendar(year, month, day);
    	
    	// do not refresh if date did not change to avoid unnecessary flickering
    	if(isSameDay(whatwhen.when, date))
    		return;
    	
    	whatwhen.when = date;
    	whatwhen.position = mViewPager.getCurrentItem(); // keep on the same reading on date change
    	new DownloadXmlTask().execute(whatwhen);

    	// Update to date button with "this.date"
    	MenuItem calendarItem = mMenu.findItem(R.id.action_calendar);
    	SimpleDateFormat actionDateFormat = new SimpleDateFormat("E d MMM y"); //TODO: move str to cst
    	calendarItem.setTitle(actionDateFormat.format(whatwhen.when.getTime()));
    }

    @Override
    public boolean onNavigationItemSelected(int position, long itemId) {
    	// Are we actually *changing* ? --> maybe not if coming from state reload
    	if(whatwhen.what != LecturesController.WHAT.values()[position]) {
    		whatwhen.what = LecturesController.WHAT.values()[position];
    		whatwhen.position = 0; // on what change, move to 1st
    	}
    	new DownloadXmlTask().execute(whatwhen);
    	return true;
    }

    @SuppressLint("SimpleDateFormat") // I know but currently French only
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	// Inflate the menu; this adds items to the action bar
    	getSupportMenuInflater().inflate(R.menu.lectures, menu);

    	// Update to date button with "this.date"
    	MenuItem calendarItem = menu.findItem(R.id.action_calendar);
    	SimpleDateFormat actionDateFormat = new SimpleDateFormat("E d MMM y"); //TODO: move str to cst
    	calendarItem.setTitle(actionDateFormat.format(whatwhen.when.getTime()));

    	mMenu = menu;

    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_about:
            return onAbout(item);
        case R.id.action_sync_settings:
        	return onSyncPref(item);
        case R.id.action_sync_do:
        	return onSyncDo(item);
        case R.id.action_refresh:
        	return onRefresh(item);
        case R.id.action_calendar:
            return onCalendar(item);
        }
        return true;
    }

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    public static Account CreateSyncAccount(Context context) {
    	// Create the account type and default account
    	Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
    	// Get an instance of the Android account manager
    	AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
    	/*
    	 * Add the account and account type, no password or user data
    	 * If successful, return the Account object, otherwise report an error.
    	 */
    	if (accountManager.addAccountExplicitly(newAccount, null, null)) {
    		return newAccount;
    	} else {
    		return accountManager.getAccountsByType(ACCOUNT_TYPE)[0];
    	}
    }

    protected void setLoading(final boolean loading) {
    	final RelativeLayout loadingOverlay = (RelativeLayout)findViewById(R.id.loadingOverlay);
    	final ProgressBar loadingIndicator = (ProgressBar)findViewById(R.id.loadingIndicator);

    	loadingOverlay.post(new Runnable() {
    		public void run() {
    			if(loading) {
    				loadingIndicator.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.sepia_fg), android.graphics.PorterDuff.Mode.MULTIPLY);
    				loadingOverlay.setVisibility(View.VISIBLE);
    			} else {
    				loadingOverlay.setVisibility(View.INVISIBLE);
    			}
    	    }
    	});
    }

    // Async loader
    private class DownloadXmlTask extends AsyncTask<WhatWhen, Void, List<LectureItem>> {
    	@Override
    	protected List<LectureItem> doInBackground(WhatWhen... whatwhen) {
    		WhatWhen ww = whatwhen[0];

    		try {
    			if(ww.useCache) {
	    			// attempt to load from cache: skip loading indicator (avoids flickering)
	    			List<LectureItem> lectures = lecturesCtrl.getLecturesFromCache(ww.what, ww.when);
	    			if(lectures != null) 
	    				return lectures;
    			}

    			// attempts to load from network, with loading indicator
    			setLoading(true);
    			return lecturesCtrl.getLecturesFromNetwork(ww.what, ww.when);
    		} catch (IOException e) {
    			Log.e(TAG, "I/O error while loading. AELF servers down ?");
    			setLoading(false);
    			return null;
    		} finally {
    			// "useCache=false" is "fire and forget"
    			ww.useCache = true;
    		}
    	}

    	@Override
    	protected void onPostExecute(final List<LectureItem> lectures) {
    		// Force running on UI thread to attempt WorkAround loads of java.lang.IllegalStateException: 
    		//     Fragment e{4a335af8} is not currently in the FragmentManager
    		runOnUiThread(new Runnable() {
    		    public void run() {
    		    	List<LectureItem> pager_data;

    	    		if(lectures != null) {
    	    			pager_data = lectures;
    	    		} else {
    	    			pager_data = networkError;
    	    		}

    	    		// Create the adapter that will return a fragment for each of the three
    	    		// primary sections of the app.
    		    	mSectionsPagerAdapter = new SectionsPagerAdapterFragment(getSupportFragmentManager(), pager_data);

    	    		// Set up the ViewPager with the sections adapter.
    	    		mViewPager = (ViewPager) findViewById(R.id.pager);
    	    		mViewPager.setAdapter(mSectionsPagerAdapter);
    	    		mViewPager.setCurrentItem(whatwhen.position);

    	    		setLoading(false);
    		    }
    		});
    	}
    }

}
