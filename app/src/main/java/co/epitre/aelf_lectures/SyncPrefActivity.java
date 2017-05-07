package co.epitre.aelf_lectures;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.extra.PiwikApplication;

public class SyncPrefActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    public static final String KEY_PREF_DISP_FONT_SIZE = "pref_disp_font_size";
    public static final String KEY_PREF_SYNC_LECTURES = "pref_sync_lectures";
    public static final String KEY_PREF_SYNC_DUREE = "pref_sync_duree";
    public static final String KEY_PREF_SYNC_CONSERV = "pref_sync_conserv";
    public static final String KEY_PREF_PARTICIPATE_BETA = "pref_participate_beta";
    public static final String KEY_PREF_PARTICIPATE_NOCACHE = "pref_participate_nocache";
    public static final String KEY_PREF_PARTICIPATE_SERVER = "pref_participate_server";
    public static final String KEY_PREF_PARTICIPATE_STATISTICS = "pref_participate_statistics";
    public static final String KEY_APP_PREVIOUS_VERSION = "app_previous_version";
    public static final String KEY_APP_SYNC_LAST_ATTEMPT = "app_sync_last_attempt";
    public static final String KEY_APP_SYNC_LAST_SUCCESS= "app_sync_last_success";

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

        // hacky hack, but does the job --> init summaries
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

    @SuppressWarnings("deprecation")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // set summary
        if (key.equals(KEY_PREF_SYNC_LECTURES) ||
            key.equals(KEY_PREF_SYNC_DUREE) ||
            key.equals(KEY_PREF_SYNC_CONSERV)) {
            ListPreference pref = (ListPreference)findPreference(key);
            pref.setSummary(pref.getEntry());
        } else if (key.equals(KEY_PREF_PARTICIPATE_SERVER)) {
            EditTextPreference pref = (EditTextPreference)findPreference(key);
            String server = pref.getText();
            if (server.isEmpty()) {
                pref.setSummary("Serveur par défaut (recommandé)");
            } else {
                pref.setSummary("L'application fonctionne avec le serveur de test: "+server+". En cas de doute, vous pouvez effacer cette valeur sans danger.");
            }
        }
        
        // called with null from the constructor
        if(sharedPreferences != null) {
            // Apply changes so that sync engines takes them into account
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.commit(); // commit to file so that sync service is able to load it from disk
        }
    }

}
