package co.epitre.aelf_lectures;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.os.Process;

import co.epitre.aelf_lectures.data.Validator;

import static co.epitre.aelf_lectures.SyncPrefActivity.KEY_PREF_PARTICIPATE_SERVER;


// Attempt to fix crash on Android 4.4 when upgrading app
// http://stackoverflow.com/questions/40069273/unable-to-get-provider-rarely-crash-on-kitkat
public class LecturesApplication extends Application {
    private static final String TAG = "LecturesApplication";
    private SharedPreferences settings;

    public static final int NOTIFICATION_SYNC_PROGRESS = 1;
    public static final int NOTIFICATION_START_ERROR = 2;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "app start...");
        checkAppReplacingState();

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        // Boot application
        initNetworkStatusMonitor();
        isValidServer();
    }

    private void checkAppReplacingState() {
        if (getResources() == null) {
            Log.w(TAG, "app is replacing...kill");
            Process.killProcess(Process.myPid());
        }
    }

    private void initNetworkStatusMonitor() {
        NetworkStatusMonitor.getInstance().register(this.getApplicationContext());
    }

    private void isValidServer() {
        // It is possible that the server URL stored in the preference is invalid, especially after
        // upgrading. And we know, we have at least one user putting an email address in this field
        // which obviously breaks the sync...

        // Get current value
        String server = settings.getString(KEY_PREF_PARTICIPATE_SERVER, "");

        // Validate it
        if (server.isEmpty() || Validator.isValidUrl(server)) {
            return;
        }

        // Force it back to default
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY_PREF_PARTICIPATE_SERVER, "");
        editor.commit();

        // Notify user
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this.getApplicationContext())
                .setContentTitle("Adresse du serveur corrigée")
                .setContentText("Vous devriez à nouveau bénéficier des lectures !")
                .setSmallIcon(android.R.drawable.ic_dialog_info);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_START_ERROR, mBuilder.build());
    }
}