package co.epitre.aelf_lectures.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import java.io.File;
import java.io.IOException;

import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.lectures.data.AelfCacheHelper;
import co.epitre.aelf_lectures.sync.SyncAdapter;

public class MainPrefFragment extends BasePrefFragment {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.app_settings, rootKey);

        // Init summaries
        onSharedPreferenceChanged(null, SettingsActivity.KEY_PREF_DISP_FONT_SIZE);
        onSharedPreferenceChanged(null, SettingsActivity.KEY_PREF_SYNC_BATTERY);
        onSharedPreferenceChanged(null, SettingsActivity.KEY_PREF_SYNC_DROP_CACHE);

        // Request adding app to doze mode whitelist
        Preference batterySyncPref = findPreference(SettingsActivity.KEY_PREF_SYNC_BATTERY);
        batterySyncPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(@NonNull Preference preference) {
                requestDozeModeExemption();
                return true;
            }
        });

        // Send mail + logs to dev
        Preference contactDevPref = findPreference(SettingsActivity.KEY_CONTACT_DEV);
        contactDevPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(@NonNull Preference preference) {
                sendMailDev();
                return true;
            }
        });

        // Drop the cache
        Preference dropCachePref = findPreference(SettingsActivity.KEY_PREF_SYNC_DROP_CACHE);
        dropCachePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(@NonNull Preference preference) {
                dropCache();
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update doze mode whitelist when coming back from the intent
        onSharedPreferenceChanged(null, SettingsActivity.KEY_PREF_SYNC_BATTERY);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        // Set summary
        if (key.equals(SettingsActivity.KEY_PREF_DISP_FONT_SIZE)) {
            SeekBarPreference pref = findPreference(key);
            pref.setSummary("Agrandissement du texte: " + pref.getValue() + "%");
        } else if (key.equals(SettingsActivity.KEY_PREF_SYNC_BATTERY)) {
            Preference batterySyncPref = findPreference(SettingsActivity.KEY_PREF_SYNC_BATTERY);
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm.isIgnoringBatteryOptimizations(context.getApplicationContext().getPackageName())) {
                batterySyncPref.setSummary("La synchronisation fonctionnera même sur batterie !");
            } else {
                batterySyncPref.setSummary("Attention: La synchronisation risque de ne pas fonctionner sur batterie...");
            }
        } else if (key.equals(SettingsActivity.KEY_PREF_SYNC_DROP_CACHE)) {
            long dbSize = AelfCacheHelper.getDatabaseSize(context);
            Preference dropCachePref = findPreference(SettingsActivity.KEY_PREF_SYNC_DROP_CACHE);
            String summary = "Taille actuelle du cache: "+android.text.format.Formatter.formatFileSize(context, dbSize)+".";
            dropCachePref.setSummary(summary);
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
            Runtime.getRuntime().exec("logcat -t 500 -f " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Generate mail with attached logs
        String to[] = {getString(R.string.app_support)};
        String subject = "Application " + getString(R.string.app_name) + " (version: " + getString(R.string.app_version) + ")";
        String packageName = context.getApplicationContext().getPackageName();
        Uri logcatURI = null;
        try {
            logcatURI = FileProvider.getUriForFile(context, packageName + ".fileprovider", outputFile);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("vnd.android.cursor.dir/email");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        if (logcatURI != null) {
            emailIntent.putExtra(Intent.EXTRA_STREAM, logcatURI);
        }
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(emailIntent , getString(R.string.mailto_dev)));
    }

    private void requestDozeModeExemption() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("Synchronisation sur batterie")
                .setMessage(Html.fromHtml(
                        "Depuis la version 6.0, Android peut bloquer la synchronisation lorsque le téléphone est sur batterie pour économiser de l'énergie." +
                                "<br/>" +
                                "<br/>Si cela pose problème sur votre téléphone, suivez la procédure suivante dans l'écran qui va s'afficher:" +
                                "<br/>" +
                                "<br/>1. Affichez toutes les applications</li>" +
                                "<br/>2. Cherchez l'application AELF</li>" +
                                "<br/>3. Désactivez les 'optimisation de batterie'</li>"
                ))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                        } catch (Exception e) {
                            // EMPTY
                        }
                        onSharedPreferenceChanged(null, SettingsActivity.KEY_PREF_SYNC_BATTERY);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void dropCache() {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.pref_sync_drop_cache_title))
                .setMessage(Html.fromHtml(
                         "Si la synchronisation ne fonctionne pas, vous pouvez essayer de purger le cache." +
                                "<br/>" +
                                "<br>Purger le cache supprimera toutes les lectures « hors connexion »." +
                                " Une nouvelle synchronisation sera automatiquement démarrée mais peut prendre du temps suivant votre connexion Internet."
                ))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Kill any running sync
                        SyncAdapter.killPendingSyncs(context);

                        // Drop the database
                        try {
                            AelfCacheHelper.dropDatabase(context);
                        } catch (Exception e) {}

                        // Refresh the size
                        onSharedPreferenceChanged(null, SettingsActivity.KEY_PREF_SYNC_DROP_CACHE);

                        // Start a new background sync
                        SyncAdapter.triggerSync(context);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}
