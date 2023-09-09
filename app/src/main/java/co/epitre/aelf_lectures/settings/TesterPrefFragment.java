package co.epitre.aelf_lectures.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.lectures.data.Validator;

public class TesterPrefFragment extends BasePrefFragment implements
        Preference.OnPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.tester_settings, rootKey);

        // Register validator for server to prevent users from using a mail address...
        Preference serverPreference = getPreferenceScreen().findPreference(SettingsActivity.KEY_PREF_PARTICIPATE_SERVER);
        assert serverPreference != null;
        serverPreference.setOnPreferenceChangeListener(this);

        // hacky hack, but does the job --> init summaries
        onSharedPreferenceChanged(null, SettingsActivity.KEY_PREF_PARTICIPATE_SERVER);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null) {
            return;
        }

        // set summary
        if (key.equals(SettingsActivity.KEY_PREF_PARTICIPATE_SERVER)) {
            EditTextPreference pref = findPreference(key);
            String server = null;
            if (pref != null) {
                server = pref.getText();

                if (server == null || server.isEmpty()) {
                    pref.setSummary("Serveur par défaut (recommandé)");
                } else {
                    pref.setSummary("L'application fonctionne avec le serveur de test: " + server + ". En cas de doute, vous pouvez effacer cette valeur sans danger.");
                }
            }
        }

        super.onSharedPreferenceChanged(sharedPreferences, key);
    }

    // Called BEFORE a change, return false to block the change
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case SettingsActivity.KEY_PREF_PARTICIPATE_SERVER:
                String candidate = newValue.toString();
                return candidate.isEmpty() || Validator.isValidUrl(candidate);
        }

        // Consider as valid by default
        return true;
    }
}
