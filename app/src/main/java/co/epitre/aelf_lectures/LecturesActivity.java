package co.epitre.aelf_lectures;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.getsentry.raven.android.Raven;
import com.getsentry.raven.event.BreadcrumbBuilder;
import com.getsentry.raven.event.Breadcrumbs;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.extra.PiwikApplication;
import org.piwik.sdk.extra.TrackHelper;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;


import co.epitre.aelf_lectures.data.AelfDate;
import co.epitre.aelf_lectures.data.LectureItem;
import co.epitre.aelf_lectures.data.LecturesController;
import co.epitre.aelf_lectures.data.LecturesController.WHAT;
import co.epitre.aelf_lectures.data.WhatWhen;
import co.epitre.aelf_lectures.sync.SyncAdapter;

public class LecturesActivity extends AppCompatActivity implements
        DatePickerFragment.CalendarDialogListener,
        ActionBar.OnNavigationListener,
        LectureFragment.LectureLinkListener,
        LectureLoadProgressListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        NetworkStatusMonitor.NetworkStatusChangedListener {

    public static final String TAG = "AELFLecturesActivity";
    public static final long DATE_TODAY = 0;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private boolean isLoading = false;
    private boolean isSuccess = true;
    DownloadXmlTask currentRefresh = null;
    Lock preventCancel = new ReentrantLock();
    WhatWhen whatwhen;
    WhatWhen whatwhen_previous = null;
    Menu mMenu;

    /**
     * Gesture detector. Detect single taps that do not look like a dismiss to toggle
     * full screen mode.
     */
    private boolean isFocused = true;
    private boolean isFullScreen = true;
    private boolean isMultiWindow = false;
    private boolean isInLongPress = false;
    private View statusBarBackgroundView = null;
    private GestureDetectorCompat mGestureDetector;

    /**
     * Global managers / resources
     */
    NetworkStatusMonitor networkStatusMonitor = NetworkStatusMonitor.getInstance();
    SharedPreferences settings = null;

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
    // Instance fields
    Account mAccount;

    // action bar
    protected ActionBar actionBar;

    /**
     * Statistics
     */
    Tracker tracker;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    LecturePagerAdapter lecturesPagerAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load Tracker
        tracker = ((PiwikApplication) getApplication()).getTracker();

        // ---- need upgrade ?
        int currentVersion, savedVersion;

        // current version
        PackageInfo packageInfo;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            Raven.capture(e);
            throw new RuntimeException("Could not determine current version");
        }
        currentVersion = packageInfo.versionCode;

        // load saved version, if any
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(this);
        savedVersion = settings.getInt(SyncPrefActivity.KEY_APP_VERSION, -1);


        // upgrade logic, primitive at the moment...
        SharedPreferences.Editor editor = settings.edit();
        if (savedVersion != currentVersion) {
            // update saved version
            editor.putInt(SyncPrefActivity.KEY_APP_VERSION, currentVersion);
            editor.putInt(SyncPrefActivity.KEY_APP_PREVIOUS_VERSION, savedVersion);
            editor.putInt(SyncPrefActivity.KEY_APP_CACHE_MIN_VERSION, 33); // Invalidate all readings loaded before this version
        }

        // Create the "WiFi" only setting on upgrade if it does not exist. The idea is that we do not
        // want to break existing users so that they should default to 'false', wherehas we default new
        // users to 'true' aka 'wifi only' to save some expensive network usage, especially in Africa.
        // as a side effect, it is expected to reduce error rates as WiFi is generally more reliable.
        if(savedVersion > 0) {
            // This is an *upgrade*
            if (!settings.contains(SyncPrefActivity.KEY_PREF_SYNC_WIFI_ONLY)) {
                // Do not override setting...
                editor.putBoolean(SyncPrefActivity.KEY_PREF_SYNC_WIFI_ONLY, false);
            }
        }

        // Create the "Region" setting from the locale, if it does not exist and invalidate the cache
        if (settings.getString(SyncPrefActivity.KEY_PREF_REGION, "").equals("")) {
            // Get locale
            String locale = getResources().getConfiguration().locale.getCountry();
            String region = "romain";

            // Make a reasonable region guess
            switch (locale) {
                case "FR": region = "france";     break;
                case "BE": region = "belgique";   break;
                case "LU": region = "luxembourg"; break;
                case "CA": region = "canada";     break;
                case "CH": region = "suisse";     break;
                default:
                    if ("DZ AO AC BJ BW BF BI CM CV CF TD KM CG CD CI DG DJ EG GQ ER ET FK GA GH GI GN GW KE LS LR LY MG MW ML MR MU YT MA MZ NA NE NG RE RW SH ST SN SC SL SO ZA SD SZ TZ GM TG TA TN UG EH ZM ZW".contains(locale)) {
                        region = "afrique";
                    } else {
                        region = "romain";
                    }
            }
            editor.putString(SyncPrefActivity.KEY_PREF_REGION, region);
        }

        // migrate SyncPrefActivity.KEY_PREF_DISP_FONT_SIZE from text to int
        try {
            String fontSize = settings.getString(SyncPrefActivity.KEY_PREF_DISP_FONT_SIZE, "normal");
            int zoom;
            switch (fontSize) {
                case "big":
                    zoom = 150;
                    break;
                case "huge":
                    zoom = 200;
                    break;
                default:
                    // small is deprecated. Treat as "normal".
                    zoom = 100;
            }
            editor.putInt(SyncPrefActivity.KEY_PREF_DISP_FONT_SIZE, zoom);
        } catch (ClassCastException e) {
            // Ignore: already migrated :)
        }

        // migrate SyncPrefActivity.KEY_PREF_SYNC_DUREE
        String syncDuree = settings.getString(SyncPrefActivity.KEY_PREF_SYNC_DUREE, "mois");
        if (syncDuree.equals("auj") || syncDuree.equals("auj-dim")) {
            editor.putString(SyncPrefActivity.KEY_PREF_SYNC_DUREE, "semaine");
        }

        editor.apply();
        // ---- end upgrade

        // create dummy account for our background sync engine
        try {
            mAccount = CreateSyncAccount();
        } catch (SecurityException e) {
            // WTF ? are denied the tiny subset of autorization we ask for ? Anyway, fallback to best effort
            Log.w(TAG, "Create/Get sync account was DENIED");
            Raven.capture(e);
            mAccount = null;
        }

        // Select where to go from here
        whatwhen = new WhatWhen();
        String openSource;

        Uri uri = this.getIntent().getData();
        if (uri != null) {
            openSource = "intent";
            parseIntentUri(whatwhen, uri);
        } else if (savedInstanceState != null) {
            // Restore saved instance state. Especially useful on screen rotate on older phones
            openSource = "restore";
            whatwhen.what = WHAT.values()[savedInstanceState.getInt("what")];
            whatwhen.position = savedInstanceState.getInt("position");

            long timestamp = savedInstanceState.getLong("when");
            if (timestamp == DATE_TODAY) {
                whatwhen.when = new AelfDate();
                whatwhen.today = true;
                whatwhen.position = 0;
            } else {
                whatwhen.when = new AelfDate(timestamp);
                whatwhen.today = false;
            }

        } else {
            // load the "lectures" for today
            openSource = "fresh";
            whatwhen.when = new AelfDate();
            whatwhen.today = true;
            whatwhen.what = WHAT.MESSE;
            whatwhen.position = 0; // 1st lecture of the office
        }

        // Track app open source + target
        Breadcrumbs.record(new BreadcrumbBuilder().setMessage("Open "+openSource).build());
        TrackHelper.track().event("OfficeActivity", "open."+openSource).name(whatwhen.toTrackerName()).value(1f).with(tracker);

        // some UI. Most UI init are done in the prev async task
        setContentView(R.layout.activity_lectures);

        // prevent phone sleep
        findViewById(android.R.id.content).setKeepScreenOn(true);

        // Spinner
        actionBar = getSupportActionBar();
        assert actionBar != null;

        Context context = actionBar.getThemedContext();
        ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(context, R.array.spinner, R.layout.support_simple_spinner_dropdown_item);
        list.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(list, this);

        // On older phones >= 44 < 6.0, we can set status bar to translucent but not its color.
        // the trick is to place a view under the status bar to emulate it.
        // cf http://stackoverflow.com/questions/22192291/how-to-change-the-status-bar-color-in-android
        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            statusBarBackgroundView = new View(this);
            statusBarBackgroundView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            statusBarBackgroundView.getLayoutParams().height = get_status_bar_height();
            ((ViewGroup) getWindow().getDecorView()).addView(statusBarBackgroundView);
            statusBarBackgroundView.setBackgroundColor(ContextCompat.getColor(this, R.color.aelf_dark));
        }


        // Turn on periodic lectures caching
        if (mAccount != null) {
            ContentResolver.setIsSyncable(mAccount, AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);
            ContentResolver.addPeriodicSync(mAccount, AUTHORITY, new Bundle(1), SYNC_INTERVAL);

            // If the account has not been synced in the last 48h OR never be synced at all, force sync
            long hours = SyncAdapter.getLastSyncSuccessAgeHours(this);
            if (hours >= 48 || hours < 0) {
                Log.w(TAG, "Automatic sync has not worked for at least 2 full days, attempting to force sync");
                do_manual_sync("outdated");
            }
        }

        // Install gesture detector
        mGestureDetector = new GestureDetectorCompat(this, new TapGestureListener());

        // Init display state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isMultiWindow = isInMultiWindowMode();
        }

        // Finally, refresh UI
        loadLecture(whatwhen);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        networkStatusMonitor.registerNetworkStatusChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        networkStatusMonitor.unregisterNetworkStatusChangeListener(this);
    }

    private void parseIntentUri(WhatWhen whatwhen, Uri uri) {
        // Parse intent URI, update whatwhen in place
        // http://www.aelf.org/                                        --> messe du jour, 1ère lecture
        // http://www.aelf.org/#messe1_lecture4                        --> messe du jour, lecture N
        // http://www.aelf.org/2017-01-27/romain/messe                 --> messe du 2017-01-27, calendrier romain
        // http://www.aelf.org/2017-01-27/romain/messe#messe1_lecture3 --> messe du 2017-01-27, calendrier romain, lecture N
        // http://www.aelf.org/2017-01-27/romain/complies              --> office des complies du 2017-01-27
        // http://www.aelf.org/2017-01-27/romain/complies#office_psaume1 --> office_TYPE[N]
        // Legacy shortcut URLs:
        // https://www.aelf.org/office-[NOM]

        String path = uri.getPath();
        String host = uri.getHost();
        String fragment = uri.getFragment();

        // Set default values
        whatwhen.what = WHAT.MESSE;
        whatwhen.when = new AelfDate();
        whatwhen.today = true;
        whatwhen.position = 0; // 1st lecture of the office

        if (host.equals("www.aelf.org")) {
            // AELF Website
            String[] chunks = path.split("/");

            if (chunks.length == 2 && chunks[1].startsWith("office-")) {
                // Attempt to parse a legacy URL
                String office_name = chunks[1].substring(7).toUpperCase();
                try {
                    whatwhen.what = WHAT.valueOf(office_name);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Failed to parse office '" + chunks[2] + "', falling back to messe", e);
                    whatwhen.what = WHAT.MESSE;
                }
            } else {
                // Attempt to parse NEW url format, starting with a date
                if (chunks.length >= 2) {
                    // Does it look like a date ?
                    String potential_date = chunks[1];
                    if (potential_date.matches("20[0-9]{2}-[0-9]{2}-[0-9]{2}")) {
                        String[] date_chunks = potential_date.split("-");
                        whatwhen.when.set(
                                Integer.parseInt(date_chunks[0]),
                                Integer.parseInt(date_chunks[1]) - 1,
                                Integer.parseInt(date_chunks[2])
                        );
                    } else {
                        Log.w(TAG, "String '" + potential_date + "' should look like a date, but it does not!");
                    }
                }

                // Attempt to parse office
                if (chunks.length >= 4) {
                    String office_name = chunks[3].toUpperCase();
                    try {
                        whatwhen.what = WHAT.valueOf(office_name);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Failed to parse office '" + chunks[2] + "', falling back to messe", e);
                        whatwhen.what = WHAT.MESSE;
                    }
                }

                // Finally, grab anchor
                whatwhen.anchor = fragment;
            }
        }
    }

    protected int get_status_bar_height() {
        // Get status bar height
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight = 0;
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    public void prepare_fullscreen() {
        // This code is a plate of spagetti but fullscreen is such a mess that I'm not even sure it's
        // possible to make it clean...
        Window window = getWindow();

        // Fullscreen does not make sense when in multi-window mode
        boolean doFullScreen = isFullScreen && !isMultiWindow && isFocused;

        // Some users wants complete full screen, no status bar at all. This is NOT compatible with multiwindow mode / non focused
        boolean hideStatusBar = settings.getBoolean(SyncPrefActivity.KEY_PREF_DISP_FULLSCREEN, false) && !isMultiWindow;

        // Android < 4.0 --> skip most logic
        if (Build.VERSION.SDK_INT < 14) {
            // Hide status (top) bar. Navigation bar (> 4.0) still visible.
            if (doFullScreen) {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            return;
        }

        Display getOrient = getWindowManager().getDefaultDisplay();
        boolean is_portrait = getOrient.getRotation() == Surface.ROTATION_0 || getOrient.getRotation() == Surface.ROTATION_180;
        int uiOptions = 0;

        // When the user wants fullscreen, always hide the status bar, even after a "tap"
        if (hideStatusBar) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // On Android versions supporting transluent but not colored status bar, manage "color" visibility
        if (statusBarBackgroundView != null && Build.VERSION.SDK_INT >= 11) {
            statusBarBackgroundView.setAlpha(hideStatusBar?0f:1f);
        }

        if (doFullScreen) {
            uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;

            // Translucent bar, *ONLY* in portait mode (broken in landscape)
            if (is_portrait) {
                uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }
            if (Build.VERSION.SDK_INT >= 19) {
                uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
        }

        // Translucent bar, *ONLY* in portrait mode (broken in landscape)
        View pagerPaddingView = findViewById(R.id.pager_padding);
        if (Build.VERSION.SDK_INT >= 19) {
            if (is_portrait && !isMultiWindow) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            } else  {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }

            // Always compensate the height but only on specific version Or *always* in portrait. Yeah!
            if (!isMultiWindow) {
                if (is_portrait || Build.VERSION.SDK_INT < 21) {
                    int height = actionBar.getHeight();
                    if (!hideStatusBar) {
                        height += get_status_bar_height();
                    }
                    pagerPaddingView.getLayoutParams().height = height;
                }
            } else {
                // When switching between modes, reset height
                pagerPaddingView.getLayoutParams().height = 0;
            }
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            pagerPaddingView.getLayoutParams().height = 0;
        }

        // Apply settings
        if (Build.VERSION.SDK_INT >= 11) {
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    public boolean do_manual_sync(String reason) {
        Breadcrumbs.record(new BreadcrumbBuilder().setMessage("Starting "+reason+" sync ").build());

        if (mAccount == null) {
            Log.w(TAG, "Failed to run manual sync: we have no account...");
            TrackHelper.track().event("OfficeActivity", "sync."+reason).name("no-account").value(1f).with(tracker);
            return false;
        }

        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        // start sync
        ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);
        TrackHelper.track().event("OfficeActivity", "sync."+reason).name("start").value(1f).with(tracker);

        // done
        return true;
    }

    private void toggleFullscreen() {
        isFullScreen = !isFullScreen;
        prepare_fullscreen();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // manage application's intrusiveness for different Android versions
        super.onWindowFocusChanged(hasFocus);

        // Always pretend we are going fullscreen. This limits flickering considerably
        isFullScreen = hasFocus;
        isFocused = hasFocus;
        prepare_fullscreen();
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        // Force fullscreen to false and refresh screen
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        isFullScreen = false;
        isMultiWindow = isInMultiWindowMode;
        prepare_fullscreen();
    }

    private void loadLecture(WhatWhen whatwhen) {
        // Cancel any pending load
        cancelLectureLoad(false);

        // Refresh UI
        actionBar.setSelectedNavigationItem(whatwhen.what.getPosition());
        updateCalendarButtonLabel();

        // Start Loading
        preventCancel.lock();
        try {
            DownloadXmlTask loader = new DownloadXmlTask(this.getApplicationContext(), whatwhen, this);
            loader.execute();
            whatwhen.useCache = true; // cache override are one-shot
            currentRefresh = loader;
        } finally {
            preventCancel.unlock();
        }
    }

    public void cancelLectureLoad(boolean restore) {
        preventCancel.lock();
        try {
            currentRefresh.cancel(true);
            if (currentRefresh.future != null) {
                currentRefresh.future.cancel(true);
            }

            Thread.sleep(100); // FIXME!!
        } catch (NullPointerException e) {
            // Asking for permission is racy
        } catch (InterruptedException e) {
            Raven.capture(e);
        } finally {
            currentRefresh = null;
            setLoading(false); // FIXME: should be in the cancel code path in the task imho
            preventCancel.unlock();
        }

        // Restore readings
        if (restore && whatwhen_previous != null) {
            whatwhen = whatwhen_previous;
            whatwhen_previous = null;
            whatwhen.useCache = true; // Make it fast, we are restoring !

            // Load lectures
            loadLecture(whatwhen);
        }
    }

    public void cancelLectureLoad(View v) {
        // Hack: if this event is triggered, there was a "tap", hence we toggled fullscreen mode
        // ==> revert. This will flicker. But that's OK for now.
        toggleFullscreen();

        // Cancel lecture load + restore previous state
        cancelLectureLoad(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save instance state. Especially useful on screen rotate on older phones
        // - what --> mass, tierce, ... ? (id)
        // - when --> date (timestamp)
        // - position --> active tab (id)
        super.onSaveInstanceState(outState);

        if (outState == null) return;

        int position = 0; // first slide by default
        int what = 0; // "Messe" by default
        long when = DATE_TODAY;

        if (whatwhen != null) {
            if (whatwhen.what != null) what = whatwhen.what.getPosition();
            if (mViewPager != null) position = mViewPager.getCurrentItem();
            if (whatwhen.when != null && !whatwhen.today && !whatwhen.when.isToday()) {
                when = whatwhen.when.getTimeInMillis();
            }
        }

        outState.putInt("what", what);
        outState.putInt("position", position);
        outState.putLong("when", when);
    }

    public boolean onAbout() {
        AboutDialogFragment aboutDialog = new AboutDialogFragment();
        aboutDialog.show(getSupportFragmentManager(), "aboutDialog");
        Breadcrumbs.record(new BreadcrumbBuilder().setMessage("Show About dialog").build());
        TrackHelper.track().event("OfficeActivity", "action.about").name("show").value(1f).with(tracker);
        return true;
    }

    public boolean onSyncPref() {
        Intent intent = new Intent(this, SyncPrefActivity.class);
        startActivity(intent);
        Breadcrumbs.record(new BreadcrumbBuilder().setMessage("Show Preference screen").build());
        TrackHelper.track().event("OfficeActivity", "action.preferences").name("show").value(1f).with(tracker);
        return true;
    }

    public boolean onSyncDo() {
        return do_manual_sync("manual");
    }

    public boolean onRefresh(String reason) {
        whatwhen.useCache = false;
        whatwhen.anchor = null;
        if (mViewPager != null) {
            whatwhen.position = mViewPager.getCurrentItem();
        } else {
            whatwhen.position = 0;
        }
        this.whatwhen_previous = null;
        Breadcrumbs.record(new BreadcrumbBuilder().setMessage("Refresh "+whatwhen.toUrlName()).build());
        TrackHelper.track().event("OfficeActivity", "action.refresh."+reason).name(whatwhen.toTrackerName()).value(1f).with(tracker);
        this.loadLecture(whatwhen);
        return true;
    }

    public boolean onCalendar() {
        Bundle args = new Bundle();
        args.putLong("time", whatwhen.when.getTimeInMillis());

        DatePickerFragment calendarDialog = new DatePickerFragment();
        calendarDialog.setArguments(args);
        calendarDialog.show(getSupportFragmentManager(), "datePicker");

        Breadcrumbs.record(new BreadcrumbBuilder().setMessage("Show Calendar").build());
        TrackHelper.track().event("OfficeActivity", "action.calendar").name("show").value(1f).with(tracker);

        return true;
    }

    public boolean onShare() {
        // Make sure we DO have something to share
        // FIXME: racy, the loader will update it and it's in a thread
        if (lecturesPagerAdapter == null || mViewPager == null) {
            return false;
        }

        // Get current position
        int position = mViewPager.getCurrentItem();
        LectureItem lecture = lecturesPagerAdapter.getLecture(position);

        // Build URL
        String url = "http://www.aelf.org/"+whatwhen.when.toIsoString()+"/romain/"+whatwhen.what.urlName();
        if (lecture.key != null) {
            url += "#"+lecture.key;
        }

        // Build the data
        String prettyDate = whatwhen.when.toPrettyString();

        // Build the subject and message
        String message;
        String subject;
        if (whatwhen.what == WHAT.MESSE && whatwhen.today) {
            // If this is Today's mass, let's be concise
            if (lecture.title != null) {
                message = lecture.title;
            } else {
                message = lecture.shortTitle;
            }
        } else {
            // Generic case
            message = lecture.shortTitle+" "+whatwhen.what.prettyName();

            // Append date if not today
            if (!whatwhen.today) {
                message += " " + prettyDate;
            }

            // Append title if defined
            if (lecture.title != null) {
                message += ": "+lecture.title;
            }
        }

        // Append the reference, IF defined AND not the same as the title
        if (lecture.reference != null && !lecture.reference.equals("") && !lecture.reference.equalsIgnoreCase(lecture.shortTitle)) {
            message += " ("+lecture.reference+")";
        }

        // Append the link
        message += ". "+url;

        // Generate the subject, let's be concise
        subject = lecture.shortTitle+" "+whatwhen.what.prettyName();
        if (!whatwhen.today) {
            subject += " " + prettyDate;
        }

        // Create the intent
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)));

        // Track
        Breadcrumbs.record(new BreadcrumbBuilder().setMessage("Share "+whatwhen.toUrlName()).build());
        TrackHelper.track().event("OfficeActivity", "share").name(whatwhen.toTrackerName()).value(1f).with(tracker);

        // All done !
        return true;
    }

    private void updateCalendarButtonLabel() {
        if(mMenu == null) {
            return;
        }
        MenuItem calendarItem = mMenu.findItem(R.id.action_calendar);
        calendarItem.setTitle(whatwhen.when.toShortPrettyString());
    }

    public void onCalendarDialogPicked(int year, int month, int day) {
        AelfDate date = new AelfDate(year, month, day);

        // do not refresh if date did not change to avoid unnecessary flickering
        if (whatwhen.when.isSameDay(date))
            return;

        // Reset pager
        this.whatwhen_previous = whatwhen.copy();
        whatwhen.today = date.isToday();
        whatwhen.when = date;
        whatwhen.position = 0;
        whatwhen.anchor = null;

        Breadcrumbs.record(new BreadcrumbBuilder().setMessage("Set date "+whatwhen.toUrlName()).build());
        this.loadLecture(whatwhen);

        // Update to date button with "this.date"
        updateCalendarButtonLabel();
    }

    @Override
    public boolean onNavigationItemSelected(int position, long itemId) {
        // Are we actually *changing* ? --> maybe not if coming from state reload
        if (whatwhen.what.getPosition() == position) {
            return true;
        }

        // Load new state
        whatwhen.what = LecturesController.WHAT.values()[position];
        whatwhen.position = 0; // on what change, move to 1st
        whatwhen.anchor = null;
        whatwhen_previous = whatwhen.copy();

        // Track
        Breadcrumbs.record(new BreadcrumbBuilder().setMessage("Set office "+whatwhen.toUrlName()).build());
        TrackHelper.track().event("OfficeActivity", "action.select-office").name("show").value(1f).with(tracker);

        // Load
        this.loadLecture(whatwhen);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar
        getMenuInflater().inflate(R.menu.lectures, menu);
        mMenu = menu;

        // Make the share image white
        Drawable normalDrawable = ContextCompat.getDrawable(this, R.drawable.ic_share_black_24dp);
        Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
        DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, R.color.white));

        // Update to date button with "this.date"
        updateCalendarButtonLabel();
        updateMenuNetworkVisibility();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                return onAbout();
            case R.id.action_sync_settings:
                return onSyncPref();
            case R.id.action_sync_do:
                return onSyncDo();
            case R.id.action_refresh:
                return onRefresh("menu");
            case R.id.action_calendar:
                return onCalendar();
            case R.id.action_share:
                return onShare();
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        try {
            return super.dispatchTouchEvent(event);
        } catch (IndexOutOfBoundsException e) {
            // Ignore: most likely caused because the app is loading and the pager view is not yet ready
            // but still forward to sentry as I'd rather be sure. Good news is: we need to overload this
            // function anyway :)
            Raven.capture(e);
        }
        return false; // Fallback: consider event as not consumed
    }

    @Override
    public boolean onLectureLink(Uri link) {
        // This comes from a tap event --> revert
        toggleFullscreen();

        // Handle special URLs
        String scheme = link.getScheme();
        String host = link.getHost();
        String path = link.getPath();

        if (scheme.equals("aelf")) {
            if (host.equals("app.epitre.co")) {
                // Handle action URL
                if (path.equals("/action/refresh")) {
                    onRefresh("lectureLink");
                }
            }
        } else {
            // Go to the reading
            parseIntentUri(whatwhen, link);
            Breadcrumbs.record(new BreadcrumbBuilder().setMessage("Open internal link " + whatwhen.toUrlName()).build());
            TrackHelper.track().event("OfficeActivity", "open.internal-link").name(whatwhen.toTrackerName()).value(1f).with(tracker);
            loadLecture(whatwhen);
        }

        // All good
        return true;
    }

    // Detect important / global option change
    // FIXME: this should probably be in the application. Should also move the account managment there
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SyncPrefActivity.KEY_PREF_SYNC_WIFI_ONLY)) {
            killPendingSyncs();
        } else if (key.equals(SyncPrefActivity.KEY_PREF_PARTICIPATE_SERVER)) {
            killPendingSyncs();
        } else if (key.equals(SyncPrefActivity.KEY_PREF_PARTICIPATE_BETA)) {
            killPendingSyncs();
        } else if (key.equals(SyncPrefActivity.KEY_PREF_REGION)) {
            // Invalidate cache
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong(SyncPrefActivity.KEY_APP_CACHE_MIN_DATE, new AelfDate().getTimeInMillis());
            editor.apply();
        }
    }

    // If there is any sync in progress, terminate it. This allows the sync engine to pick up any
    // important preference changes
    // TODO: use some sort of signaling instead...
    private void killPendingSyncs() {
        // If the preference changed, cancel any running sync so that we either stop waiting for
        // the wifi, either stop either the network
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // This API is not available on Android < 11, keep it best effort
            return;
        }

        if (ContentResolver.getCurrentSyncs().isEmpty()) {
            // There is no sync in progress
            return;
        }

        // Cancel sync
        ContentResolver.cancelSync(mAccount, AUTHORITY);

        // Kill any background processes
        ActivityManager am = (ActivityManager)getApplicationContext().getSystemService(ACTIVITY_SERVICE);
        String packageName = getPackageName();
        if (packageName != null && am != null) {
            am.killBackgroundProcesses(packageName);
        }

        // Cleanup any notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(LecturesApplication.NOTIFICATION_SYNC_PROGRESS);
    }

    @Override
    public void onNetworkStatusChanged(NetworkStatusMonitor.NetworkStatusEvent networkStatusEvent) {
        switch (networkStatusEvent) {
            case NETWORK_ON:
                updateMenuNetworkVisibility();

                // Attempt to reload current slide, If there was an error.
                if (!this.isSuccess) {
                    onRefresh("networkUp");
                }
                break;
            case NETWORK_OFF:
                updateMenuNetworkVisibility();
                break;
        }
    }

    private void updateMenuNetworkVisibility() {
        if(mMenu == null) {
            return;
        }

        boolean visible = networkStatusMonitor.isNetworkAvailable();
        mMenu.findItem(R.id.action_refresh).setVisible(visible);
        mMenu.findItem(R.id.action_sync_do).setVisible(visible);
    }

    /**
     * Detect simple taps that are not immediately following a long press (ie: skip cancels)
     */
    private class TapGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent event) {
            isInLongPress = true;
            Log.d(TAG, "onLongPress: " + event.toString());
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            if (!isInLongPress) {
                // Disabled: noisy, low value
                // TrackHelper.track().event("OfficeActivity", "fullscreen.toggle").name("tap").value(1f).with(tracker);
                toggleFullscreen();
            }
            isInLongPress = false;
            return true;
        }
    }

    /**
     * Create a new dummy account for the sync adapter
     */
    public Account CreateSyncAccount() {
        Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);

        // Create the account explicitly. If account creation fails, it means that it already exists.
        // In this case, keep and return the dummy instance. We'll need to trigger manual sync
        accountManager.addAccountExplicitly(newAccount, null, null);
        return newAccount;
    }

    protected void setLoading(final boolean loading) {
        final RelativeLayout loadingOverlay = (RelativeLayout)findViewById(R.id.loadingOverlay);
        final ProgressBar loadingIndicator = (ProgressBar)findViewById(R.id.loadingIndicator);
        final Button cancelButton = (Button)findViewById(R.id.cancelButton);

        // Do not trigger animations. That causes flickering.
        if (isLoading == loading) {
            return;
        }
        isLoading = loading;

        loadingOverlay.post(new Runnable() {
            public void run() {
                if(loading) {
                    Animation fadeIn = new AlphaAnimation(0, 1);
                    fadeIn.setInterpolator(new DecelerateInterpolator());
                    fadeIn.setDuration(500);

                    Animation buttonFadeIn = new AlphaAnimation(0, 1);
                    buttonFadeIn.setInterpolator(new DecelerateInterpolator());
                    buttonFadeIn.setStartOffset(2500);
                    buttonFadeIn.setDuration(500);

                    loadingIndicator.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(LecturesActivity.this, R.color.sepia_fg), android.graphics.PorterDuff.Mode.MULTIPLY);
                    cancelButton.setVisibility(View.VISIBLE);
                    cancelButton.setAnimation(buttonFadeIn);
                    loadingOverlay.setVisibility(View.VISIBLE);
                    loadingOverlay.setAnimation(fadeIn);
                } else {
                    Animation fadeOut = new AlphaAnimation(1, 0);
                    fadeOut.setInterpolator(new DecelerateInterpolator());
                    fadeOut.setDuration(250);

                    cancelButton.setVisibility(View.GONE);
                    loadingOverlay.setVisibility(View.INVISIBLE);
                    loadingOverlay.setAnimation(fadeOut);
                }
            }
        });
    }

    //
    // Async load callbacks. Guaranted to be called on main UI thread
    //

    public void onLectureLoadProgress(LectureLoadProgress progress) {
        switch (progress) {
            case LOAD_START:
                setLoading(true);
                break;
            case LOAD_FAIL:
                setLoading(false);
                Toast.makeText(this, "Oups... Impossible de rafraîchir.", Toast.LENGTH_SHORT).show();
                break;
            case LOAD_DONE:
                setLoading(false);
                break;
        }
    }

    public void onLectureLoaded(List<LectureItem> lectures, boolean isSuccess) {
        preventCancel.lock();
        this.isSuccess = isSuccess;
        try {
            // If we have an anchor, attempt to find corresponding position
            if (isSuccess) {
                if (whatwhen.anchor != null && lectures != null) {
                    int position = -1;
                    for (LectureItem lecture : lectures) {
                        position++;
                        if (whatwhen.anchor.equals(lecture.key)) {
                            whatwhen.position = position;
                            break;
                        }
                    }
                }
            } else {
                whatwhen.position = 0;
            }

            // Set up the ViewPager with the sections adapter.
            try {
                // 1 slide fragment <==> 1 lecture
                lecturesPagerAdapter = new LecturePagerAdapter(getSupportFragmentManager(), lectures);

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                mViewPager = (ViewPager) findViewById(R.id.pager);
                mViewPager.setAdapter(lecturesPagerAdapter);
                mViewPager.setCurrentItem(whatwhen.position);

                transaction.commit();
                setLoading(false);
            } catch (IllegalStateException e) {
                // Fragment manager has gone away, will reload anyway so silently give up
            } finally {
                currentRefresh = null;
                preventCancel.unlock();
            }
        } catch (Exception e) {
            Raven.capture(e);
            throw e;
        }
    }

}
