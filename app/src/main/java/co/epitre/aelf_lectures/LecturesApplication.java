package co.epitre.aelf_lectures;

import static co.epitre.aelf_lectures.settings.SettingsActivity.KEY_PREF_PARTICIPATE_SERVER;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Process;
import android.os.StrictMode;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.concurrent.TimeUnit;

import co.epitre.aelf_lectures.components.NetworkStatusMonitor;
import co.epitre.aelf_lectures.components.webviewpool.WebViewPoolManager;
import co.epitre.aelf_lectures.lectures.data.LecturesController;
import co.epitre.aelf_lectures.lectures.data.Validator;
import co.epitre.aelf_lectures.settings.SettingsActivity;
import co.epitre.aelf_lectures.sync.SyncManager;
import co.epitre.aelf_lectures.utils.HardwareDetection;


// Attempt to fix crash on Android 4.4 when upgrading app
// http://stackoverflow.com/questions/40069273/unable-to-get-provider-rarely-crash-on-kitkat
public class LecturesApplication extends Application {
    // General configuration
    private static final String TAG = "LecturesApplication";
    private static final int INITIAL_WEB_VIEWS = 4; // Typical observed, when swiping
    private static final int MAX_WEB_VIEW = 6; // Maximum observed, when jumping in the Bible

    // Resources
    private static LecturesApplication instance;
    private SharedPreferences settings;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "app start...");
        checkAppReplacingState();

        // Load Sqlite lib
        System.loadLibrary("sqliteX");

        // Get global manager instances
        instance = this;
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        // Boot application
        HardwareDetection.getGuessedPerformanceClass(this);
        WebViewPoolManager.Initialize(this, INITIAL_WEB_VIEWS, MAX_WEB_VIEW);
        initNetworkStatusMonitor();
        isValidServer();
        initializeSync();
        LecturesController.getInstance(this);

        // Enable strict mode, after startup, unless release
        maybeEnableStrictMode();
    }

    //
    // Accessors
    //

    public static LecturesApplication getInstance() {
        return instance;
    }

    //
    // Internals
    //

    private void checkAppReplacingState() {
        if (getResources() == null) {
            Log.w(TAG, "app is replacing...kill");
            Process.killProcess(Process.myPid());
        }
    }

    private void initNetworkStatusMonitor() {
        NetworkStatusMonitor.getInstance().register(this.getApplicationContext());
    }

    private void isValidServer() {
        // It is possible that the server URL stored in the preference is invalid, especially after
        // upgrading. And we know, we have at least one user putting an email address in this field
        // which obviously breaks the sync...

        // Get current value
        String server = settings.getString(KEY_PREF_PARTICIPATE_SERVER, "");

        // Validate it
        if (server.isEmpty() || Validator.isValidUrl(server)) {
            return;
        }

        // Force it back to default
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY_PREF_PARTICIPATE_SERVER, "");
        editor.apply();
    }

    private void initializeSync() {
        // Ensure the region is configured
        if (settings.getString(SettingsActivity.KEY_PREF_REGION, "").isEmpty()) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(SettingsActivity.KEY_PREF_REGION, getDefaultRegionFromLocale());
            editor.apply();
        }

        // Boot the sync
        SyncManager syncManager = SyncManager.getInstance(this);

        // If the sync has not run for more than 2 days, expedite
        long lastSyncAge = syncManager.getLastSyncSuccessAgeHours();
        if (lastSyncAge > 7*24) {
            Log.w(TAG, "Sync has not run for more than a week ("+lastSyncAge+" hours). Scheduling with low delay.");
            syncManager.triggerSync(1, TimeUnit.MINUTES);
        } else if (lastSyncAge > 48) {
            Log.w(TAG, "Sync has not run for "+lastSyncAge+" hours. Scheduling.");
            syncManager.triggerSync();
        }
    }

    private String getDefaultRegionFromLocale() {
        String locale = getResources().getConfiguration().locale.getCountry();

        // Make a reasonable region guess
        return switch (locale) {
            case "FR" -> "france";
            case "BE" -> "belgique";
            case "LU" -> "luxembourg";
            case "CA" -> "canada";
            case "CH" -> "suisse";
            case "DZ", "AO", "AC", "BJ", "BW", "BF", "BI", "CM", "CV", "CF", "TD", "KM", "CG", "CD",
                 "CI", "DG", "DJ", "EG", "GQ", "ER", "ET", "FK", "GA", "GH", "GI", "GN", "GW", "KE",
                 "LS", "LR", "LY", "MG", "MW", "ML", "MR", "MU", "YT", "MA", "MZ", "NA", "NE", "NG",
                 "RE", "RW", "SH", "ST", "SN", "SC", "SL", "SO", "ZA", "SD", "SZ", "TZ", "GM", "TG",
                 "TA", "TN", "UG", "EH", "ZM", "ZW" -> "afrique";
            default -> "romain";
        };
    }

    private void maybeEnableStrictMode() {
        // Only in debug mode
        if (!BuildConfig.DEBUG) {
            return;
        }

        // Enable strict mode, unless in production mode
        Log.w(TAG, "Enabling strict mode");
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyFlashScreen()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
        super.onCreate();
    }
}