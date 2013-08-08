package co.epitre.aelf_lectures;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.widget.ArrayAdapter;

import co.epitre.aelf_lectures.data.LectureItem;
import co.epitre.aelf_lectures.data.LecturesController;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

class WhatWhen {
	public LecturesController.WHAT what;
	public GregorianCalendar when;
}

public class LecturesActivity extends SherlockFragmentActivity implements DatePickerFragment.CalendarDialogListener,
                                                                  ActionBar.OnNavigationListener {

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
	// Sync interval in ms. ~ 1 Day
	public static final long SYNC_INTERVAL = 1000L*60L*60L*24L;
	// Instance fields
	Account mAccount;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	
	// TODO: detect first launch + trigger initial sync

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// create dummy account for our background sync engine
		mAccount = CreateSyncAccount(this);

		// init the lecture controller
		lecturesCtrl = LecturesController.getInstance(this);

		// load the "lectures" for today
		whatwhen = new WhatWhen();
		whatwhen.when = new GregorianCalendar();
		whatwhen.what = LecturesController.WHAT.MESSE;
		
		// error handler
		networkError.add(new LectureItem("Erreur Réseau", "<p>Connexion au serveur AELF impossible<br />Veuillez ré-essayer plus tard.</p>", "erreur", -1));
		
		// some UI. Most UI init are done in the prev async task
		setContentView(R.layout.activity_lectures);
		
		// Spinner
	    ActionBar actionBar = getSupportActionBar();
	    
	    Context context = actionBar.getThemedContext();
	    ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(context, R.array.spinner, R.layout.sherlock_spinner_item);
	    list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
	    
	    actionBar.setDisplayShowTitleEnabled(false);
	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
	    actionBar.setListNavigationCallbacks(list, this);
		
		
		/*ActionBar actionBar = getSupportActionBar();
		SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(actionBar.getThemedContext(), R.array.spinner,
		          android.R.layout.simple_spinner_dropdown_item);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);*/
		
		// finally, turn on periodic lectures caching
		if(mAccount != null) {
			ContentResolver.setIsSyncable(mAccount, AUTHORITY, 1);
			ContentResolver.addPeriodicSync(mAccount, AUTHORITY, new Bundle(1), SYNC_INTERVAL);
		}
	}
	
	public boolean onAbout(MenuItem item) {
		AboutDialogFragment aboutDialog = new AboutDialogFragment();
		aboutDialog.show(getSupportFragmentManager(), "aboutDialog");
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
		whatwhen.when = new GregorianCalendar(year, month, day);
		new DownloadXmlTask().execute(whatwhen);
		
		// Update to date button with "this.date"
		MenuItem calendarItem = mMenu.findItem(R.id.action_calendar);
		SimpleDateFormat actionDateFormat = new SimpleDateFormat("E d MMM y"); //TODO: move str to cst
		calendarItem.setTitle(actionDateFormat.format(whatwhen.when.getTime()));
	}
	
	@Override
	public boolean onNavigationItemSelected(int position, long itemId) {
		// FIXME: range check
		whatwhen.what = LecturesController.WHAT.values()[position];
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
		Account newAccount = new Account(
				ACCOUNT, ACCOUNT_TYPE);
		// Get an instance of the Android account manager
		AccountManager accountManager =
				(AccountManager) context.getSystemService(
						ACCOUNT_SERVICE);
		/*
		 * Add the account and account type, no password or user data
		 * If successful, return the Account object, otherwise report an error.
		 */
		if (accountManager.addAccountExplicitly(newAccount, null, null)) {
			return newAccount;
		} else {
			return null;
		}
	}
	
	// Async loader
	private class DownloadXmlTask extends AsyncTask<WhatWhen, Void, List<LectureItem>> {
		@Override
		protected List<LectureItem> doInBackground(WhatWhen... whatwhen) {
			try {
				WhatWhen ww = whatwhen[0];
				return lecturesCtrl.getLectures(ww.what, ww.when);
			} catch (IOException e) {
				// TODO print error message
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(List<LectureItem> lectures) {
			// Create the adapter that will return a fragment for each of the three
			// primary sections of the app.
			if(lectures == null) {
				lectures = networkError;
			}
			mSectionsPagerAdapter = new SectionsPagerAdapterFragment(getSupportFragmentManager(), lectures);

			// Set up the ViewPager with the sections adapter.
			mViewPager = (ViewPager) findViewById(R.id.pager);
			mViewPager.setAdapter(mSectionsPagerAdapter);
			if(whatwhen.what == LecturesController.WHAT.MESSE) {
				mViewPager.setCurrentItem(lectures.size()-1); // directly move to Gospel
			}
		}
	}

}
