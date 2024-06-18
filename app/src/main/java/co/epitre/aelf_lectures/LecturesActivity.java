package co.epitre.aelf_lectures;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import co.epitre.aelf_lectures.base.BaseActivity;
import co.epitre.aelf_lectures.base.DialogsKt;
import co.epitre.aelf_lectures.base.SectionFragment;
import co.epitre.aelf_lectures.bible.SectionBibleFragment;
import co.epitre.aelf_lectures.lectures.SectionLecturesFragment;
import co.epitre.aelf_lectures.lectures.data.OfficeTypes;
import co.epitre.aelf_lectures.settings.SettingsActivity;
import co.epitre.aelf_lectures.sync.SyncManager;

public class LecturesActivity extends BaseActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = "AELFLecturesActivity";

    /**
     * Global managers / resources
     */

    // action bar
    protected ActionBar actionBar;

    /**
     * Navigation
     */

    private Toolbar toolbar;
    private NavigationView drawerView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call parent (handles night mode)
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
        savedVersion = settings.getInt(SettingsActivity.KEY_APP_VERSION, -1);

        // upgrade logic, primitive at the moment...
        SharedPreferences.Editor editor = settings.edit();
        if (savedVersion != currentVersion) {
            // update saved version
            editor.putInt(SettingsActivity.KEY_APP_VERSION, currentVersion);
            editor.putInt(SettingsActivity.KEY_APP_PREVIOUS_VERSION, savedVersion);
            DialogsKt.displayWhatsNewDialog(this);

            // Purge cache on upgrade (get new Bible index if any, ...)
            deleteCache();

            // Trigger a new sync, in case the DB needs a refresh
            SyncManager.getInstance(this).triggerSync();
        }

        // migrate SettingsActivity.KEY_PREF_DISP_FONT_SIZE from text to int
        try {
            String fontSize = settings.getString(SettingsActivity.KEY_PREF_DISP_FONT_SIZE, "normal");
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
            editor.putInt(SettingsActivity.KEY_PREF_DISP_FONT_SIZE, zoom);
        } catch (ClassCastException e) {
            // Ignore: already migrated :)
        }

        // migrate SettingsActivity.KEY_PREF_SYNC_DUREE
        String syncDuree = settings.getString(SettingsActivity.KEY_PREF_SYNC_DUREE, "mois");
        if (syncDuree.equals("auj") || syncDuree.equals("auj-dim")) {
            editor.putString(SettingsActivity.KEY_PREF_SYNC_DUREE, "semaine");
        }

        // delete legacy preference
        editor.remove(SettingsActivity.KEY_PREF_DISP_NIGHT_MODE);

        editor.apply();
        // ---- end upgrade

        // some UI. Most UI init are done in the prev async task
        setContentView(R.layout.activity_lectures);

        // prevent phone sleep
        findViewById(android.R.id.content).setKeepScreenOn(true);

        // Action bar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerView = findViewById(R.id.drawer_navigation_view);
        drawerView.setNavigationItemSelectedListener(this);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerToggle.syncState();
        drawerLayout.addDrawerListener(drawerToggle);

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
        drawerView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            @NonNull
            public WindowInsets onApplyWindowInsets(@NonNull View view, @NonNull WindowInsets windowInsets) {
                View navigationMenuView = findViewById(com.google.android.material.R.id.design_navigation_view);
                navigationMenuView.setPaddingRelative(0, 0, 0, windowInsets.getSystemWindowInsetBottom());
                return windowInsets.consumeSystemWindowInsets();
            }
        });

        // Task switcher color
        Bitmap appIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        String appName = getString(R.string.app_name);
        int appColor = getResources().getColor(R.color.dark_aelf_primary_dark);
        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(appName, appIcon, appColor);
        setTaskDescription(taskDescription);

        // Route the application
        if (savedInstanceState != null) {
            // Nothing to do
        } else if (handleIntent(getIntent())) {
            // Called from a search or link
        } else {
            setSection(new SectionLecturesFragment());
        }

        // Setup the (full) screen
        prepare_fullscreen();
    }

    @Override
    protected void onStart() {
        super.onStart();
        lastStopRestartIfNeeded();
    }

    @Override
    protected void onStop() {
        super.onStop();
        lastStopRecord();
    }

    public void setHomeButtonEnabled(boolean enabled, View.OnClickListener onToolbarNavigationClickListener) {
        drawerToggle.setDrawerIndicatorEnabled(!enabled);
        drawerToggle.setToolbarNavigationClickListener(onToolbarNavigationClickListener);
        drawerToggle.syncState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    public void onIntent(Intent intent) {
        onNewIntent(intent);
    }

    private boolean handleIntent(Intent intent) {
        Uri uri = intent.getData();

        // Finally, load inner fragment
        if (uri != null) {
            onLink(uri);
        } else if(Intent.ACTION_SEARCH.equals(intent.getAction())) {
            onSearch();
        } else {
            return false;
        }

        // Reset the "pause" timer so we don't reset after handling the intent
        lastStopReset();

        return true;
    }

    private SectionFragment getCurrentSectionFragment() {
        return (SectionFragment) getSupportFragmentManager().findFragmentById(R.id.section_container);
    }

    private void setSection(SectionFragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.section_container, fragment);
        if (getCurrentSectionFragment() != null) {
            fragmentTransaction.addToBackStack(null);
        }
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
        Window window = getWindow();

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
        uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        if (has_bottom_navigation_bar && !isInMultiWindowMode()) {
            // Portrait mode: enable translucent navigation bar and compensate height
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            toolbar.setPadding(0, get_status_bar_height(), 0, 0);
        } else  {
            // FIXME: looks like we have always the same padding
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            toolbar.setPadding(0, 0, 0, 0);
        }

        // Apply settings
        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public boolean isInMultiWindowMode() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return super.isInMultiWindowMode();
        } else {
            return false;
        }
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        // Force fullscreen to false and refresh screen
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        prepare_fullscreen();
    }

    public boolean onSyncPref() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
    }

    public boolean onSyncDo() {
        SyncManager.getInstance(this).triggerSync(5, TimeUnit.SECONDS);
        return true;
    }

    public boolean onApplyOptimalSyncSettings() {
        SharedPreferences.Editor editor = settings.edit();

        // Reset sync settings
        editor.putString(SettingsActivity.KEY_PREF_SYNC_DUREE, "mois");
        editor.putString(SettingsActivity.KEY_PREF_SYNC_LECTURES, "messe-offices");
        editor.putBoolean(SettingsActivity.KEY_PREF_SYNC_WIFI_ONLY, false);

        // Reset test settings
        editor.putString(SettingsActivity.KEY_PREF_PARTICIPATE_SERVER, "");
        editor.putBoolean(SettingsActivity.KEY_PREF_PARTICIPATE_BETA, false);
        editor.putBoolean(SettingsActivity.KEY_PREF_PARTICIPATE_NOCACHE, false);

        editor.apply();

        onRefresh("applied-settings");

        return true;
    }

    public boolean onRefresh(String reason) {
        return getCurrentSectionFragment().onRefresh(reason);
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
        OfficeTypes what = OfficeTypes.fromMenuId(item.getItemId());
        Intent intent;
        if (what != null) {
            Uri uri = SectionLecturesFragment.buildUri(what);
            intent = new Intent(Intent.ACTION_VIEW, uri);
        } else if (item.getItemId() == R.id.nav_bible) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.aelf.org/bible/home"));
        } else {
            // This is something else :)
            return false; // Do not select item as we do not know what this is...
        }

        onNewIntent(intent);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int itemId = item.getItemId();

        if (itemId == R.id.action_about) {
            DialogsKt.displayAboutDialog(this);
            return true;
        } else if (itemId == R.id.action_sync_settings) {
            return onSyncPref();
        } else if (itemId == R.id.action_sync_do) {
            return onSyncDo();
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        try {
            return super.dispatchTouchEvent(event);
        } catch (IndexOutOfBoundsException|NullPointerException e) {
            // Ignore: most likely caused because the app is loading and the pager view is not yet ready
        }
        return false; // Fallback: consider event as not consumed
    }

    private boolean onLink(Uri link) {
        // Handle special URLs
        String scheme = link.getScheme();
        if (scheme == null) {
            scheme = "";
        }

        String host = link.getHost();
        if (host == null) {
            host = "";
        }

        String path = link.getPath();
        if (path == null) {
            path = "";
        }

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
            if (chunks.length >= 2 && (chunks[1].equals("bible") || chunks[1].equals("search"))) {
                // Bible link
                setSection(new SectionBibleFragment());
            } else if (chunks.length == 1 || chunks.length >= 2) {
                // Home page or Office link
                setSection(new SectionLecturesFragment());
            }
        }

        // All good
        return true;
    }

    private boolean onSearch() {
        // Search is only supported by the Bible
        setSection(new SectionBibleFragment());

        // All good
        return true;
    }

    /*
     * Phone configuration change
     */

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        // Call parent (handles night mode)
        super.onConfigurationChanged(newConfig);

        // Update screen
        prepare_fullscreen();
    }

    //
    // Last pause helpers (restart if paused for too long)
    //

    private void lastStopReset() {
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(SettingsActivity.KEY_APP_SYNC_LAST_STOP);
        editor.apply();
    }

    private void lastStopRecord() {
        long currentTimeMillis = System.currentTimeMillis();
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(SettingsActivity.KEY_APP_SYNC_LAST_STOP, currentTimeMillis);
        editor.apply();
    }

    private void lastStopRestartIfNeeded() {
        // Check last pause time
        long currentTimeMillis = System.currentTimeMillis();
        long lastStopTimeMillis = settings.getLong(SettingsActivity.KEY_APP_SYNC_LAST_STOP, -1);
        if (lastStopTimeMillis < 0) {
            return;
        }

        // If the app was paused for more than 30min --> restart (the most important state is saved as preferences)
        long pauseDurationMinutes = (currentTimeMillis - lastStopTimeMillis) / 1000 / 60;
        if (pauseDurationMinutes > 60) {
            Log.i(TAG, "onResume: The application has been paused for "+pauseDurationMinutes+" seconds. Restarting.");
            Intent intent = new Intent(this, this.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            lastStopReset();
            startActivity(intent);
        }
    }

    //
    // Utils
    //

    public void deleteCache() {
        try {
            File dir = getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {}
    }

    public boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < Objects.requireNonNull(children).length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}
