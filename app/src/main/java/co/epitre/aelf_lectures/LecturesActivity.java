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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.List;

import co.epitre.aelf_lectures.data.AelfDate;
import co.epitre.aelf_lectures.data.LecturesController.WHAT;
import co.epitre.aelf_lectures.sync.SyncAdapter;

public class LecturesActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        LectureFragment.LectureLinkListener,
        DrawerLayout.DrawerListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "AELFLecturesActivity";

    /**
     * Full screen mode
     */
    private boolean isFullScreen = true;
    private boolean isMultiWindow = false;
    private View statusBarBackgroundView = null;

    /**
     * Global managers / resources
     */
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
     * Navigation
     */

    private Toolbar toolbar;
    private NavigationView drawerView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    /**
     * Sections
     */
    SectionFragmentBase sectionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Install theme before anything else
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean nightMode = settings.getBoolean(SyncPrefActivity.KEY_PREF_DISP_NIGHT_MODE, false);
        this.setTheme(nightMode ? R.style.AelfAppThemeDark : R.style.AelfAppThemeLight);

        // Restore state
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
        Resources res = getResources();
        settings.registerOnSharedPreferenceChangeListener(this);
        savedVersion = settings.getInt(SyncPrefActivity.KEY_APP_VERSION, -1);


        // upgrade logic, primitive at the moment...
        SharedPreferences.Editor editor = settings.edit();
        if (savedVersion != currentVersion) {
            // update saved version
            editor.putInt(SyncPrefActivity.KEY_APP_VERSION, currentVersion);
            editor.putInt(SyncPrefActivity.KEY_APP_PREVIOUS_VERSION, savedVersion);
            editor.putInt(SyncPrefActivity.KEY_APP_CACHE_MIN_VERSION, 45); // Invalidate all readings loaded before this version
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
            String locale = res.getConfiguration().locale.getCountry();
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
            mAccount = null;
        }

        // some UI. Most UI init are done in the prev async task
        setContentView(R.layout.activity_lectures);

        // prevent phone sleep
        findViewById(android.R.id.content).setKeepScreenOn(true);

        // Action bar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // On older phones >= 44 < 6.0, we can set status bar to translucent but not its color.
        // the trick is to place a view under the status bar to emulate it.
        // cf http://stackoverflow.com/questions/22192291/how-to-change-the-status-bar-color-in-android
        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            statusBarBackgroundView = new View(this);
            statusBarBackgroundView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            statusBarBackgroundView.getLayoutParams().height = get_status_bar_height();
            ((ViewGroup) getWindow().getDecorView()).addView(statusBarBackgroundView);
            statusBarBackgroundView.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_aelf_primary_dark));
        }

        // Navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerView = findViewById(R.id.drawer_navigation_view);
        drawerView.setNavigationItemSelectedListener(this);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerToggle.syncState();
        drawerLayout.addDrawerListener(drawerToggle);
        drawerLayout.addDrawerListener(this);

        // Open drawer on toolbar title click for easier migration / discovery
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(drawerView)) {
                    drawerLayout.closeDrawers();
                } else {
                    drawerLayout.openDrawer(drawerView);
                }
            }
        });

        // Add some padding at the bottom of the drawer and content to account for the navigation bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            drawerView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    View navigationMenuView = findViewById(R.id.design_navigation_view);
                    navigationMenuView.setPaddingRelative(0, 0, 0, windowInsets.getSystemWindowInsetBottom());
                    return windowInsets.consumeSystemWindowInsets();
                }
            });
        }

        // Task switcher color for Android >= 21
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap appIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            String appName = getString(R.string.app_name);
            int appColor = getResources().getColor(R.color.dark_aelf_primary_dark);
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(appName, appIcon, appColor);
            setTaskDescription(taskDescription);
        }

        // Turn on periodic toolbar_main caching
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

        // Init display state
        if (Build.VERSION.SDK_INT >= 24) {
            isMultiWindow = isInMultiWindowMode();
        }

        // Get intent URI, if any
        Uri uri = getIntent().getData();

        // Finally, load inner fragment
        if (uri != null) {
            onLink(uri);
        } else if (savedInstanceState != null) {
            restoreSection();
        } else {
            setSection(new SectionOfficesFragment());
        }

        // Setup the (full) screen
        prepare_fullscreen();
    }

    private void restoreSection() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        sectionFragment = (SectionFragmentBase)fragmentManager.findFragmentById(R.id.section_container);
        sectionFragment.onRestore();
    }

    private void setSection(SectionFragmentBase fragment) {
        sectionFragment = fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.section_container, sectionFragment);
        fragmentTransaction.commit();
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
        // This code is a plate of spaghetti but fullscreen is such a mess that I'm not even sure it's
        // possible to make it clean...
        Window window = getWindow();

        // Fullscreen does not make sense when in multi-window mode
        boolean doFullScreen = isFullScreen && !isMultiWindow;

        // Some users wants complete full screen, no status bar at all. This is NOT compatible with multiwindow mode / non focused
        boolean hideStatusBar = settings.getBoolean(SyncPrefActivity.KEY_PREF_DISP_FULLSCREEN, false) && !isMultiWindow;

        // Detect orientation
        Display getOrient = getWindowManager().getDefaultDisplay();
        boolean is_portrait = getOrient.getRotation() == Surface.ROTATION_0 || getOrient.getRotation() == Surface.ROTATION_180;

        // Guess the device type
        Configuration cfg = getResources().getConfiguration();
        boolean is_tablet = cfg.smallestScreenWidthDp >= 600;

        // Guess navigation bar location (bottom or side)
        boolean has_bottom_navigation_bar = is_portrait || is_tablet;

        // Build UI options
        int uiOptions = 0;

        // When the user wants fullscreen, always hide the status bar, even after a "tap"
        if (hideStatusBar) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // On Android versions supporting translucent but not colored status bar, manage "color" visibility
        if (statusBarBackgroundView != null) {
            statusBarBackgroundView.setAlpha(hideStatusBar?0f:1f);
        }

        if (doFullScreen) {
            uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;

            // Translucent bar, *ONLY* in portrait mode (broken in landscape)
            if (has_bottom_navigation_bar && hideStatusBar) {
                uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }
            if (Build.VERSION.SDK_INT >= 19) {
                uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
        }

        // Translucent bar, *ONLY* in portrait mode (broken in landscape)
        if (Build.VERSION.SDK_INT >= 19) {
            if (has_bottom_navigation_bar && !isMultiWindow) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            } else  {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }

            // Compensate status bar height in full screen, with visible toolbar, on portrait mode
            // or some specific versions in landscape too.
            if (!isMultiWindow && !hideStatusBar && (has_bottom_navigation_bar || Build.VERSION.SDK_INT < 21)) {
                toolbar.setPadding(0, get_status_bar_height(), 0, 0);
            } else {
                // When switching between modes, reset height
                toolbar.setPadding(0, 0, 0, 0);
            }

        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            toolbar.setPadding(0, 0, 0, 0);
        }

        // Apply settings
        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
    }

    public boolean do_manual_sync(String reason) {
        if (mAccount == null) {
            Log.w(TAG, "Failed to run manual sync: we have no account...");
            return false;
        }

        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        // start sync
        ContentResolver.requestSync(mAccount, AUTHORITY, settingsBundle);

        // done
        return true;
    }

    private void enterFullscreen() {
        isFullScreen = true;
        prepare_fullscreen();
    }

    private void exitFullscreen() {
        isFullScreen = false;
        prepare_fullscreen();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // manage application's intrusiveness for different Android versions
        super.onWindowFocusChanged(hasFocus);

        // If we are gaining the focus, go fullscreen
        if (hasFocus) {
            isFullScreen = true;
            prepare_fullscreen();
        }
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        // Force fullscreen to false and refresh screen
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        isMultiWindow = isInMultiWindowMode;
        prepare_fullscreen();
    }

    public boolean onAbout() {
        AboutDialogFragment aboutDialog = new AboutDialogFragment();
        aboutDialog.show(getSupportFragmentManager(), "aboutDialog");
        return true;
    }

    public boolean onSyncPref() {
        Intent intent = new Intent(this, SyncPrefActivity.class);
        startActivity(intent);
        return true;
    }

    public boolean onSyncDo() {
        return do_manual_sync("manual");
    }

    public boolean onApplyOptimalSyncSettings() {
        SharedPreferences.Editor editor = settings.edit();

        // Reset sync settings
        editor.putString(SyncPrefActivity.KEY_PREF_SYNC_DUREE, "mois");
        editor.putString(SyncPrefActivity.KEY_PREF_SYNC_LECTURES, "messe-offices");
        editor.putBoolean(SyncPrefActivity.KEY_PREF_SYNC_WIFI_ONLY, false);

        // Reset test settings
        editor.putString(SyncPrefActivity.KEY_PREF_PARTICIPATE_SERVER, "");
        editor.putBoolean(SyncPrefActivity.KEY_PREF_PARTICIPATE_BETA, false);
        editor.putBoolean(SyncPrefActivity.KEY_PREF_PARTICIPATE_NOCACHE, false);

        // Make sure sync is enabled on device
        ContentResolver.setMasterSyncAutomatically(true);
        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);

        editor.apply();

        onRefresh("applied-settings");

        return true;
    }

    public boolean onRefresh(String reason) {
        return sectionFragment.onRefresh(reason);
    }

    @Override
    /** Drawer menu callback */
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Schedule drawer close (this is async)
        drawerLayout.closeDrawers();

        // Fast path: if was already checked, do nothing more
        if (item.isChecked()) {
            return true;
        }

        // Mark item as checked / active
        item.setChecked(true);

        // Route menu item
        WHAT what = WHAT.fromMenuId(item.getItemId());
        if (what != null) {
            SectionOfficesFragment sectionOfficeFragment;
            try {
                sectionOfficeFragment = (SectionOfficesFragment) sectionFragment;
                sectionOfficeFragment.loadLecture(what);
            } catch (ClassCastException e) {
                sectionOfficeFragment = new SectionOfficesFragment();
                Bundle arguments = new Bundle();
                arguments.putInt("what", what.getPosition());
                sectionOfficeFragment.setArguments(arguments);
                setSection(sectionOfficeFragment);
            }
            return true;
        } else if (item.getItemId() == R.id.nav_bible) {
            if (!(sectionFragment instanceof SectionBibleFragment)) {
                setSection(new SectionBibleFragment());
            }
            return true;
        } else {
            // This is something else :)
            return false; // Do not select item as we do not know what this is...
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_about:
                return onAbout();
            case R.id.action_sync_settings:
                return onSyncPref();
            case R.id.action_sync_do:
                return onSyncDo();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        try {
            return super.dispatchTouchEvent(event);
        } catch (IndexOutOfBoundsException e) {
            // Ignore: most likely caused because the app is loading and the pager view is not yet ready
            // but still forward to sentry as I'd rather be sure.
        }
        return false; // Fallback: consider event as not consumed
    }

    @Override
    public boolean onLectureLink(Uri link) {
        return onLink(link);
    }

    private boolean onLink(Uri link) {
        // Handle special URLs
        String scheme = link.getScheme();
        String host = link.getHost();
        String path = link.getPath();
        String[] chunks = path.split("/");

        if (scheme.equals("aelf")) {
            if (host.equals("app.epitre.co")) {
                // Handle action URL
                if (path.equals("/action/refresh")) {
                    onRefresh("lectureLink");
                } else if (path.equals("/action/apply-optimal-sync-settings")) {
                    onApplyOptimalSyncSettings();
                }
            }
        } else if (host.equals("www.aelf.org")) {
            // Route to the appropriate fragment
            if (chunks.length >= 2 && chunks[1].equals("bible")) {
                // Bible link
                if (!(sectionFragment instanceof SectionBibleFragment)) {
                    setSection(new SectionBibleFragment());
                } else {
                    sectionFragment.onLink(link);
                }
            } else if (chunks.length == 1 || chunks.length >= 2 && chunks[1].matches("20[0-9]{2}-[0-9]{2}-[0-9]{2}")) {
                // Home page or Office link
                if (!(sectionFragment instanceof SectionOfficesFragment)) {
                    setSection(new SectionOfficesFragment());
                } else {
                    sectionFragment.onLink(link);
                }
            }
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
        } else if (key.equals(SyncPrefActivity.KEY_PREF_DISP_NIGHT_MODE)) {
            recreate();
        }
    }

    // If there is any sync in progress, terminate it. This allows the sync engine to pick up any
    // important preference changes
    // TODO: use some sort of signaling instead...
    private void killPendingSyncs() {
        // If the preference changed, cancel any running sync so that we either stop waiting for
        // the wifi, either stop either the network

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
    /**
     * Back Button
     */
    @Override
    public void onBackPressed() {

        List fragmentList = getSupportFragmentManager().getFragments();

        boolean handled = false;
        for(Object f : fragmentList) {
            if(f instanceof SectionFragmentBase) {
                handled = ((SectionFragmentBase)f).onBackPressed();

                if(handled) {
                    break;
                }
            }
        }

        if(!handled) {
            super.onBackPressed();
        }
    }

    /*
     * Orientation change
     */

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        prepare_fullscreen();
    }

    /*
     * Drawer open / close listener
     */

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {
        exitFullscreen();
    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        enterFullscreen();
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}

    @Override
    public void onDrawerStateChanged(int newState) {}
}
