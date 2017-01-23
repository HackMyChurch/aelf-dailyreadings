package co.epitre.aelf_lectures;

import android.app.Application;
import android.util.Log;
import android.os.Process;

// Attempt to fix crash on Android 4.4 when upgrading app
// http://stackoverflow.com/questions/40069273/unable-to-get-provider-rarely-crash-on-kitkat
public class LecturesApplication extends Application {
    private static final String TAG = "DevToolsApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "app start...");
        checkAppReplacingState();
    }

    private void checkAppReplacingState() {
        if (getResources() == null) {
            Log.w(TAG, "app is replacing...kill");
            Process.killProcess(Process.myPid());
        }
    }
}