package co.epitre.aelf_lectures.settings;

import android.os.Bundle;

import co.epitre.aelf_lectures.R;

public class MainPrefFragment extends BasePrefFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.app_settings, rootKey);
    }
}
