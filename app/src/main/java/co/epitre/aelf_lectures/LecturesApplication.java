package co.epitre.aelf_lectures;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.os.Process;

import com.getsentry.raven.android.AndroidRavenFactory;
import com.getsentry.raven.android.Raven;
import com.getsentry.raven.android.event.helper.AndroidEventBuilderHelper;
import com.getsentry.raven.dsn.Dsn;
import com.getsentry.raven.event.EventBuilder;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.TrackerConfig;
import org.piwik.sdk.extra.DownloadTracker;
import org.piwik.sdk.extra.PiwikApplication;
import org.piwik.sdk.extra.TrackHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import co.epitre.aelf_lectures.data.Credentials;
import co.epitre.aelf_lectures.data.Validator;
import co.epitre.aelf_lectures.sync.SyncAdapter;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static co.epitre.aelf_lectures.SyncPrefActivity.KEY_PREF_PARTICIPATE_SERVER;
import static co.epitre.aelf_lectures.SyncPrefActivity.KEY_PREF_PARTICIPATE_STATISTICS;

/**
 * Sentry event helper. Will allow us to track our own debug data
 */
class AelfEventBuilderHelper extends AndroidEventBuilderHelper {
    private Context ctx;
    private SharedPreferences settings;
    private String piwikUserId;

    public AelfEventBuilderHelper(Context ctx, String piwikUserId) {
        super(ctx);
        this.ctx = ctx;
        this.piwikUserId = piwikUserId;
        this.settings = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    @Override
    public void helpBuildingEvent(EventBuilder eventBuilder) {
        super.helpBuildingEvent(eventBuilder);
        Map<String, Map<String, Object>> contexts = eventBuilder.getEvent().getContexts();
        eventBuilder.withContexts(extendContexts(contexts));
        eventBuilder.withTag("piwik", piwikUserId);
    }

    private String getLastUpdateDate() {
        long lastUpdateTime;
        try {
            lastUpdateTime = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        return df.format(new Date(lastUpdateTime));
    }

    private Map<String, Map<String, Object>> extendContexts(Map<String, Map<String, Object>> contexts) {
        Resources res = ctx.getResources();
        Map<String, Object> syncMap    = new HashMap<>();
        Map<String, Object> networkMap = new HashMap<>();
        Map<String, Object> appMap     = contexts.get("app");
        contexts.put("sync",    syncMap);
        contexts.put("network", networkMap);


        // Application
        appMap.put("app_previous_build", settings.getInt(SyncPrefActivity.KEY_APP_PREVIOUS_VERSION, -1));
        appMap.put("app_upgrade_date", getLastUpdateDate());
        appMap.put("user", piwikUserId);

        // Sync
        syncMap.put("last_attempt", SyncAdapter.getLastSyncAttemptAgeHours(ctx));
        syncMap.put("last_success", SyncAdapter.getLastSyncSuccessAgeHours(ctx));
        syncMap.put("cache_bypass", settings.getBoolean(SyncPrefActivity.KEY_PREF_PARTICIPATE_NOCACHE, false));
        syncMap.put("beta",         settings.getBoolean(SyncPrefActivity.KEY_PREF_PARTICIPATE_BETA, false));
        syncMap.put("server",       settings.getString(SyncPrefActivity.KEY_PREF_PARTICIPATE_SERVER, "default"));
        syncMap.put("what",         settings.getString(SyncPrefActivity.KEY_PREF_SYNC_LECTURES, res.getString(R.string.pref_lectures_def)));
        syncMap.put("duration",     settings.getString(SyncPrefActivity.KEY_PREF_SYNC_DUREE,    res.getString(R.string.pref_duree_def)));
        syncMap.put("conservation", settings.getString(SyncPrefActivity.KEY_PREF_SYNC_CONSERV,  res.getString(R.string.pref_conserv_def)));

        // Network
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = connManager.getActiveNetworkInfo();
        if (netinfo != null) {
            networkMap.put("available", true);
            networkMap.put("reason", netinfo.getReason());
            networkMap.put("type", netinfo.getTypeName());
            networkMap.put("subtype", netinfo.getSubtypeName());
            networkMap.put("state", netinfo.getDetailedState().name());
            networkMap.put("available", netinfo.isAvailable());
            networkMap.put("connected", netinfo.isConnected());
            networkMap.put("roaming", netinfo.isRoaming());
        } else {
            networkMap.put("available", false);
        }

        return contexts;
    }
}

/**
 * Sentry client builder. We need it to add our own event builder to track some AELF specific debug
 * data. I hate java...
 */
class AelfRavenFactory extends AndroidRavenFactory {
    private Context ctx;
    private String piwikUserId;
    SharedPreferences settings;

    public AelfRavenFactory(Context ctx, String piwikUserId) {
        super(ctx);
        this.piwikUserId = piwikUserId;
        this.ctx = ctx;
        this.settings = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    @Override
    public com.getsentry.raven.Raven createRavenInstance(Dsn dsn) {
        // If the user has opted-out, send events to /dev/null. This is hack to bypass
        // the protocol validation stage in the Android client
        if (!settings.getBoolean(KEY_PREF_PARTICIPATE_STATISTICS, true)) {
            Log.i("RAVEN_FACTORY",  "User has opted out, using noop DSN");
            dsn = new Dsn();
        }
        com.getsentry.raven.Raven ravenInstance = super.createRavenInstance(dsn);
        ravenInstance.addBuilderHelper(new AelfEventBuilderHelper(ctx, piwikUserId));
        return ravenInstance;
    }
}

// Attempt to fix crash on Android 4.4 when upgrading app
// http://stackoverflow.com/questions/40069273/unable-to-get-provider-rarely-crash-on-kitkat
public class LecturesApplication extends PiwikApplication implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "LecturesApplication";
    private SharedPreferences settings;
    public static final int STATS_DIM_SOURCE = 1;

    public static final int STATS_DIM_STATUS = 2;
    public static final int STATS_DIM_DAY_DELTA = 3;
    public static final int STATS_DIM_DAY_NAME = 4;

    public static final int NOTIFICATION_SYNC_PROGRESS = 1;
    public static final int NOTIFICATION_START_ERROR = 2;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "app start...");
        checkAppReplacingState();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(this);

        // Boot application
        initPiwik();
        initSentry();
        isValidServer();
    }

    private void checkAppReplacingState() {
        if (getResources() == null) {
            Log.w(TAG, "app is replacing...kill");
            Process.killProcess(Process.myPid());
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
        if(key.equals(KEY_PREF_PARTICIPATE_STATISTICS)) {
            Tracker tracker = getTracker();
            tracker.setOptOut(!settings.getBoolean(KEY_PREF_PARTICIPATE_STATISTICS, true));
            this.initSentry();
            Log.i(TAG, "Piwik/Sentry OptOut status changed to: "+tracker.isOptOut());
        }
    }

    @Override
    public TrackerConfig onCreateTrackerConfig() {
        return TrackerConfig.createDefault(Credentials.PIWIK_URI, Credentials.PIWIK_APP);
    }

    // see https://github.com/piwik/piwik-sdk-android/blob/master/exampleapp/src/main/java/com/piwik/demo/DemoApp.java
    private void initPiwik() {
        // Track this app install, this will only trigger once per app version.
        Tracker tracker = getTracker();
        TrackHelper.track().download().identifier(new DownloadTracker.Extra.ApkChecksum(this)).with(tracker);
        tracker.setOptOut(!settings.getBoolean(KEY_PREF_PARTICIPATE_STATISTICS, true));
        // tracker.setDispatchInterval(1);

        Log.i(TAG, "Piwik setup complete. OptOut status: "+tracker.isOptOut());
    }

    // see https://sentry.app.epitre.co/sentry/aelf-application/getting-started/java-android/
    private void initSentry() {
        Context ctx = this.getApplicationContext();
        String server = "https://"+Credentials.SENTRY_DSN_PUBLIC_KEY+":"+Credentials.SENTRY_DSN_SECRET_KEY+"@sentry.app.epitre.co/2";
        Raven.clearStoredRaven();
        Raven.init(ctx, server, new AelfRavenFactory(ctx, getTracker().getUserId()));
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
        editor.commit();

        // Notify user
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this.getApplicationContext())
                .setContentTitle("Adresse du serveur corrigée")
                .setContentText("Vous devriez à nouveau bénéficier des lectures !")
                .setSmallIcon(android.R.drawable.ic_dialog_info);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_START_ERROR, mBuilder.build());
    }
}