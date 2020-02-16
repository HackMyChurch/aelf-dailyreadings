package co.epitre.aelf_lectures.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.content.FileProvider;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import java.io.File;
import java.io.IOException;

import co.epitre.aelf_lectures.R;

public class MainPrefFragment extends BasePrefFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.app_settings, rootKey);

        // Init font size summary
        onSharedPreferenceChanged(null, SettingsActivity.KEY_PREF_DISP_FONT_SIZE);

        // Send mail + logs to dev
        Preference contactDevPref = findPreference(SettingsActivity.KEY_CONTACT_DEV);
        contactDevPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                sendMailDev();
                return true;
            }
        });
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

    private void sendMailDev(){
        Context context = getContext();
        if (context == null) {
            return;
        }

        // Grab the logs
        File outputFile = new File(context.getExternalCacheDir(), "logcat.txt");
        try {
            Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Generate mail with attached logs
        String to[] = {getString(R.string.app_support)};
        String subject = "Application " + getString(R.string.app_name) + " (version: " + getString(R.string.app_version) + ")";
        String packageName = context.getApplicationContext().getPackageName();
        Uri logcatURI = FileProvider.getUriForFile(context, packageName + ".fileprovider", outputFile);

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("vnd.android.cursor.dir/email");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_STREAM, logcatURI);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(emailIntent , getString(R.string.mailto_dev)));
    }
}
