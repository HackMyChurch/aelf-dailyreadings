package co.epitre.aelf_lectures;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.extra.PiwikApplication;
import org.piwik.sdk.extra.TrackHelper;

import co.epitre.aelf_lectures.data.Validator;

public class SyncPrefActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {
    public static final String KEY_PREF_REGION = "pref_region";
    public static final String KEY_PREF_DISP_FONT_SIZE = "pref_disp_font_size";
    public static final String KEY_PREF_DISP_FULLSCREEN = "pref_disp_fullscreen";
    public static final String KEY_PREF_DISP_PULL_TO_REFRESH = "pref_disp_pull_to_refresh";
    public static final String KEY_PREF_SYNC_LECTURES = "pref_sync_lectures";
    public static final String KEY_PREF_SYNC_DUREE = "pref_sync_duree";
    public static final String KEY_PREF_SYNC_CONSERV = "pref_sync_conserv";
    public static final String KEY_PREF_SYNC_WIFI_ONLY = "pref_sync_wifi_only";
    public static final String KEY_PREF_PARTICIPATE_BETA = "pref_participate_beta";
    public static final String KEY_PREF_PARTICIPATE_NOCACHE = "pref_participate_nocache";
    public static final String KEY_PREF_PARTICIPATE_SERVER = "pref_participate_server";
    public static final String KEY_PREF_PARTICIPATE_STATISTICS = "pref_participate_statistics";
    public static final String KEY_APP_PREVIOUS_VERSION = "previous_version";
    public static final String KEY_APP_SYNC_LAST_ATTEMPT = "app_sync_last_attempt";
    public static final String KEY_APP_SYNC_LAST_SUCCESS= "app_sync_last_success";
    public static final String KEY_APP_CACHE_MIN_VERSION= "min_cache_version";
    public static final String KEY_APP_CACHE_MIN_DATE = "min_cache_date";
    public static final String KEY_APP_VERSION = "version";

    /**
     * Statistics
     */
    Tracker tracker;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load Tracker
        tracker = ((PiwikApplication) getApplication()).getTracker();

        addPreferencesFromResource(R.xml.sync_preferences);

        // Register validator for server to prevent users from using a mail address...
        getPreferenceScreen().findPreference(KEY_PREF_PARTICIPATE_SERVER).setOnPreferenceChangeListener(this);

        // hacky hack, but does the job --> init summaries
        onSharedPreferenceChanged(null, KEY_PREF_REGION);
        onSharedPreferenceChanged(null, KEY_PREF_SYNC_LECTURES);
        onSharedPreferenceChanged(null, KEY_PREF_SYNC_DUREE);
        onSharedPreferenceChanged(null, KEY_PREF_SYNC_CONSERV);
        onSharedPreferenceChanged(null, KEY_PREF_PARTICIPATE_SERVER);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    // Called AFTER a change
    @SuppressWarnings("deprecation")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // set summary
        if (key.equals(KEY_PREF_SYNC_LECTURES) ||
            key.equals(KEY_PREF_REGION) ||
            key.equals(KEY_PREF_SYNC_DUREE) ||
            key.equals(KEY_PREF_SYNC_CONSERV)) {
            ListPreference pref = (ListPreference)findPreference(key);
            pref.setSummary(pref.getEntry());
        } else if (key.equals(KEY_PREF_PARTICIPATE_SERVER)) {
            EditTextPreference pref = (EditTextPreference)findPreference(key);
            String server = pref.getText();
            if (server == null || server.isEmpty()) {
                pref.setSummary("Serveur par défaut (recommandé)");
            } else {
                pref.setSummary("L'application fonctionne avec le serveur de test: "+server+". En cas de doute, vous pouvez effacer cette valeur sans danger.");
            }
        }

        // Stop here is called with null preference pointer from the constructor
        if (sharedPreferences == null) {
            return;
        }

        // Statistics
        boolean enabled;
        switch (key) {
            case KEY_PREF_DISP_FULLSCREEN:
                enabled = sharedPreferences.getBoolean(KEY_PREF_DISP_FULLSCREEN, false);
                TrackHelper.track().event("OfficePreferences", "display.fullscreen").name(enabled?"enable":"disable").value(1f).with(tracker);
                break;
            case KEY_PREF_PARTICIPATE_BETA:
                enabled = sharedPreferences.getBoolean(KEY_PREF_PARTICIPATE_BETA, false);
                TrackHelper.track().event("OfficePreferences", "participate.beta").name(enabled?"enable":"disable").value(1f).with(tracker);
                break;
            case KEY_PREF_PARTICIPATE_NOCACHE:
                enabled = sharedPreferences.getBoolean(KEY_PREF_PARTICIPATE_NOCACHE, false);
                TrackHelper.track().event("OfficePreferences", "participate.cache").name(enabled?"disable":"enable").value(1f).with(tracker);
                break;
            case KEY_PREF_REGION:
                String region = sharedPreferences.getString(KEY_PREF_REGION, "romain");
                TrackHelper.track().event("OfficePreferences", "lectures.region").name(region).value(1f).with(tracker);
                break;
        }
        
        // Apply changes so that sync engines takes them into account
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.commit(); // commit to file so that sync service is able to load it from disk
    }

    // Called BEFORE a change, return false to block the change
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case KEY_PREF_PARTICIPATE_SERVER:
                String candidate = newValue.toString();
                return candidate.isEmpty() || Validator.isValidUrl(candidate);
        }

        // Consider as valid by default
        return true;
    }
}
