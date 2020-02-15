package co.epitre.aelf_lectures.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.SeekBarPreference;

import co.epitre.aelf_lectures.R;

public class MainPrefFragment extends BasePrefFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.app_settings, rootKey);

        // hacky hack, but does the job --> init summaries
        onSharedPreferenceChanged(null, SettingsActivity.KEY_PREF_DISP_FONT_SIZE);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Set summary
        if (key.equals(SettingsActivity.KEY_PREF_DISP_FONT_SIZE)) {
            SeekBarPreference pref = findPreference(key);
            pref.setSummary("Agrandissement du texte: " + pref.getValue() + "%");
        }

        super.onSharedPreferenceChanged(sharedPreferences, key);
    }
}
