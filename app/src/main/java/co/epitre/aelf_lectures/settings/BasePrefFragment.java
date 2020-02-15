package co.epitre.aelf_lectures.settings;

import androidx.preference.PreferenceFragmentCompat;

import android.content.SharedPreferences;

public abstract class BasePrefFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Stop here is called with null preference pointer from the constructor
        if (sharedPreferences == null) {
            return;
        }

        // Apply changes so that sync engines takes them into account
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.apply(); // commit to file so that sync service is able to load it from disk
    }
}
