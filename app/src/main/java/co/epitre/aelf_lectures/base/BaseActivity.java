package co.epitre.aelf_lectures.base;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.settings.SettingsActivity;

/**
 * Implement the common features between all activities:
 * - Handling night mode
 */
public class BaseActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "BaseActivity";

    private boolean nightMode;
    protected SharedPreferences settings;

    /*
     * Shared: Activity initialization
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Get the preferences
        this.settings = PreferenceManager.getDefaultSharedPreferences(this);

        // Install theme before anything else
        nightMode = this.shouldNightMode(this.getDesiredDisplayMode());
        this.setTheme(nightMode ? R.style.AelfAppThemeDark : R.style.AelfAppThemeLight);

        // Register preference listener
        this.settings.registerOnSharedPreferenceChangeListener(this);

        // Call parent
        super.onCreate(savedInstanceState);
    }

    /*
     * Shared: (un-)Register listeners
     */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, this.getClass().getName()+".onDestroy");
        this.settings.unregisterOnSharedPreferenceChangeListener(this);
    }

    /*
     * Shared: Phone configuration change
     */

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Night mode change handling
        int newNightConfig = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean newNightMode = newNightConfig == Configuration.UI_MODE_NIGHT_YES;
        String prefNightMode = this.getDesiredDisplayMode();
        if (newNightConfig != Configuration.UI_MODE_NIGHT_UNDEFINED && newNightMode != nightMode && prefNightMode.equals("auto")) {
            recreate();
        }
    }

    /*
     * Shared: Watch for preference change
     */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SettingsActivity.KEY_PREF_DISP_NIGHT_MODE_V2)) {
            recreate();
        }
    }

    /*
     * Night mode
     */

    public boolean getNightMode(){
        return this.nightMode;
    }
    private String getDesiredDisplayMode() {
        String defaultMode = getResources().getString(R.string.pref_disp_night_mode_v2_def);
        String desiredDisplayMode = settings.getString(
                SettingsActivity.KEY_PREF_DISP_NIGHT_MODE_V2,
                defaultMode);
        return desiredDisplayMode;
    }

    private boolean shouldNightMode(String desiredDisplayMode) {
        if (desiredDisplayMode.equals("day")) {
            return false;
        }
        if (desiredDisplayMode.equals("night")) {
            return true;
        }

        Configuration systemConfig = getResources().getConfiguration();
        int systemNightModeConfig = systemConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return systemNightModeConfig == Configuration.UI_MODE_NIGHT_YES;
    }
}
