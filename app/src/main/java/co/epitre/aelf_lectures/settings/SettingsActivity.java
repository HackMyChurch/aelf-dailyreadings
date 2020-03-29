package co.epitre.aelf_lectures.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import co.epitre.aelf_lectures.R;

public class SettingsActivity extends AppCompatActivity {
    public static final String KEY_PREF_REGION = "pref_region";
    public static final String KEY_PREF_DISP_FONT_SIZE = "pref_disp_font_size";
    public static final String KEY_PREF_DISP_PSALM_UNDERLINE = "pref_disp_psalm_underline";
    public static final String KEY_PREF_DISP_FULLSCREEN = "pref_disp_fullscreen";
    public static final String KEY_PREF_DISP_NIGHT_MODE = "pref_disp_night_mode";
    public static final String KEY_PREF_SYNC_LECTURES = "pref_sync_lectures";
    public static final String KEY_PREF_SYNC_DUREE = "pref_sync_duree";
    public static final String KEY_PREF_SYNC_CONSERV = "pref_sync_conserv";
    public static final String KEY_PREF_SYNC_WIFI_ONLY = "pref_sync_wifi_only";
    public static final String KEY_PREF_SYNC_BATTERY = "pref_sync_battery";
    public static final String KEY_PREF_SYNC_DROP_CACHE = "pref_sync_drop_cache";
    public static final String KEY_PREF_PARTICIPATE_BETA = "pref_participate_beta";
    public static final String KEY_PREF_PARTICIPATE_NOCACHE = "pref_participate_nocache";
    public static final String KEY_PREF_PARTICIPATE_SERVER = "pref_participate_server";
    public static final String KEY_CONTACT_DEV = "contact_dev";
    public static final String KEY_APP_PREVIOUS_VERSION = "previous_version";
    public static final String KEY_APP_SYNC_LAST_STOP = "app_sync_last_stop";
    public static final String KEY_APP_SYNC_LAST_ATTEMPT = "app_sync_last_attempt";
    public static final String KEY_APP_SYNC_LAST_SUCCESS = "app_sync_last_success";
    public static final String KEY_APP_CACHE_MIN_VERSION = "min_cache_version";
    public static final String KEY_APP_CACHE_MIN_DATE = "min_cache_date";
    public static final String KEY_APP_VERSION = "version";
    public static final String KEY_BIBLE_LAST_PAGE = "bible_last_page";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Install theme before anything else
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean nightMode = settings.getBoolean(SettingsActivity.KEY_PREF_DISP_NIGHT_MODE, false);
        this.setTheme(nightMode ? R.style.AelfAppThemeDark : R.style.AelfAppThemeLight);

        // Call parent
        super.onCreate(savedInstanceState);

        // Register settings fragment
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preference_container, new MainPrefFragment())
                .commit();
    }

}
