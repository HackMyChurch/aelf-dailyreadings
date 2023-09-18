package co.epitre.aelf_lectures.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.base.BaseActivity;

public class SettingsActivity extends BaseActivity {
    public static final String KEY_PREF_REGION = "pref_region";
    public static final String KEY_PREF_DISP_FONT_SIZE = "pref_disp_font_size";
    public static final String KEY_PREF_DISP_PINCH_TO_ZOOM = "pref_disp_pinch_to_zoom";
    public static final String KEY_PREF_DISP_PSALM_UNDERLINE = "pref_disp_psalm_underline";
    public static final String KEY_PREF_DISP_NIGHT_MODE = "pref_disp_night_mode";
    public static final String KEY_PREF_DISP_NIGHT_MODE_V2 = "pref_disp_night_mode_v2";
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
    public static final String KEY_APP_ABOUT = "app_about";
    public static final String KEY_APP_NEWS = "app_news";
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
        // Call parent (handles night mode)
        super.onCreate(savedInstanceState);

        // Register settings fragment
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preference_container, new MainPrefFragment())
                .commit();
    }

    // Intercept crashes when opening intents from settings
    @Override
    public void startActivity(Intent intent, Bundle options) {
        try {
            super.startActivity(intent, options);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Failed to launch intent "+intent.toString()+": "+e);
            Toast.makeText(this, "Impossible de lancer l'action demandée", Toast.LENGTH_SHORT).show();
        }
    }
}
