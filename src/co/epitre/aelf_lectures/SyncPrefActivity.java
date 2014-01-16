package co.epitre.aelf_lectures;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class SyncPrefActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    public static final String KEY_PREF_SYNC_LECTURES = "pref_sync_lectures";
    public static final String KEY_PREF_SYNC_DUREE = "pref_sync_duree";
    public static final String KEY_PREF_SYNC_CONSERV = "pref_sync_conserv";

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sync_preferences);

        // hacky hack, but does the job --> init summaries
        onSharedPreferenceChanged(null, KEY_PREF_SYNC_LECTURES);
        onSharedPreferenceChanged(null, KEY_PREF_SYNC_DUREE);
        onSharedPreferenceChanged(null, KEY_PREF_SYNC_CONSERV);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressLint("NewApi") // check
    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        if(android.os.Build.VERSION.SDK_INT >= 9) {
            // need this to make sure Sync sees the new preferences...
        	SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
        	editor.apply();
        }
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
        }
    }

}
