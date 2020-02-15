package co.epitre.aelf_lectures;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import co.epitre.aelf_lectures.data.AelfDate;
import co.epitre.aelf_lectures.data.LectureItem;
import co.epitre.aelf_lectures.data.LecturesController;
import co.epitre.aelf_lectures.data.WhatWhen;
import co.epitre.aelf_lectures.settings.SettingsActivity;

/**
 * Created by jean-tiare on 22/05/17.
 */

enum LectureLoadProgress {
    LOAD_START,
    LOAD_FAIL,
    LOAD_DONE;
}

interface LectureLoadProgressListener {
    void onLectureLoadProgress(LectureLoadProgress progress);
    void onLectureLoaded(List<LectureItem> lectures, boolean isSuccess);
}

/* Async loader
 *
 * Cancel are unreliable using URLConnection class in the controller. What we do instead to manage
 * cancels is:
 * - track current load task in a "future", in a thread pool
 * - set a flag
 * - cancel current load future
 * - on flag change, remove loading screen if any
 * - if the flag is true, ignore any result
 * Timeouts *should* limit the impact of threads / connections stacking. Should...
 */
class DownloadXmlTask extends AsyncTask<Void, Void, List<LectureItem>> {
    private Context ctx;
    private LectureLoadProgressListener lectureLoadProgressListener;
    private LecturesController lecturesCtrl = null;

    private WhatWhen ww = null;

    public static final String TAG = "DownloadXmlTask";

    /**
     * Error messages
     */
    private static final String noNetworkErrorMessage = ""+
            "<h3>Aucune connexion Internet n’est disponible</h3>" +
            "<p>L’office que vous demandez n’est pas disponible en mode hors-connexion. Pour le consulter, assurez-vous de disposer d’une connexion Internet de bonne qualité, de préférence en WiFi, puis ré-essayez.</p>" +
            "<p><strong>Astuce&nbsp;:</strong> Une fois chargé avec succès, cet office sera automatiquement disponible en mode hors-connexion&nbsp;!</p>";
    private static final String connectionErrorMessage = ""+
            "<h3>Une erreur s'est glissée lors du chargement des lectures</h3>" +
            "<p>Saviez-vous que cette application est développée entièrement bénévolement&nbsp;? Elle est construite en lien et avec le soutien de l'AELF, mais elle reste un projet indépendant, soutenue par <em>votre</em> prière&nbsp!</p>\n" +
            "<div class=\"app-office-navigation\"><a href=\"aelf://app.epitre.co/action/refresh\">Ré-essayer</a></div>";
    private static final String emptyOfficeErrorMessage = "" +
            "<h3>Il n'y a pas encore de lectures pour cet office</h3>" +
            "<p>Le saviez-vous ? <a href=\"http://aelf.org/\">AELF</a> ajoute les nouveaux offices quotidiennement, jusqu'à un mois à l'avance&nbsp;! Celui-ci devrait bientôt arriver.</p>";
    private static final String subOptimalSettingsErrorMessage = ""+
            "<h3>Erreur de synchronisation</h3>" +
            "<p>Cette lecture devrait être disponible hors connexion. Malheureusement, nous ne l'avons pas trouvée.</p>" +
            "<p>Pour fonctionner de manière optimale, la synchronisation doit être activée sur votre téléphone et l'application devrait être configurée pour pré-charger les lectures du mois dès qu'une connexion est disponible.</p>" +
            "<div class=\"app-office-navigation\"><a href=\"aelf://app.epitre.co/action/apply-optimal-sync-settings\">Appliquer ces paramètres</a></div>";


    public DownloadXmlTask(Context ctx, WhatWhen whatwhen, LectureLoadProgressListener lectureLoadProgressListener) {
        this.ctx = ctx;
        this.lecturesCtrl = LecturesController.getInstance(ctx);
        this.lectureLoadProgressListener = lectureLoadProgressListener;
        this.ww = whatwhen.copy();
    }

    private void runOnUIThread(Runnable code) {
        new Handler(Looper.getMainLooper()).post(code);
    }

    private void onLectureLoadProgress(final LectureLoadProgress progress) {
        if (lectureLoadProgressListener != null) {
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    lectureLoadProgressListener.onLectureLoadProgress(progress);
                }
            });
        }
    }

    @Override
    protected List<LectureItem> doInBackground(Void... voids) {

        try {
            // TODO: start only after a delay
            // TODO: use a nicer lecture swap animation ?
            onLectureLoadProgress(LectureLoadProgress.LOAD_START);
            List<LectureItem> lectures =  lecturesCtrl.loadLectures(ww.what, ww.when, ww.useCache);
            onLectureLoadProgress(LectureLoadProgress.LOAD_DONE);
            return lectures;
        } catch (IOException e) {
            Log.e(TAG, "I/O error while loading. AELF servers down ?");
            onLectureLoadProgress(LectureLoadProgress.LOAD_FAIL);
            return null;
        }
    }

    @Override
    protected void onCancelled(List<LectureItem> lectureItems) {
        super.onCancelled(lectureItems);
    }

    private List<LectureItem> buildErrorMessage(String message) {
        List<LectureItem> error = new ArrayList<>(1);

        // Build and return error
        error.add(new LectureItem("error", "Erreur", message, null));
        return error;
    }

    private boolean detectSubOptimalSettings() {
        // Never consider sub optimal settings as the cause of the problems if the reading is more than 20 days ahead
        if(ww.when.dayBetween(new AelfDate()) > 20) {
            return false;
        }

        // Always consider as sub-optimal is global sync is disabled
        if(!ContentResolver.getMasterSyncAutomatically()) {
            return true;
        }

        // Starting from there, we'll need settings
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        Resources res = ctx.getResources();

        // Is there a custom server ? Beta and no cache are non critical
        if(!preference.getString(SettingsActivity.KEY_PREF_PARTICIPATE_SERVER, "").equals("")) {
            return true;
        }

        // Are we syncing for a month ?
        if(!preference.getString(SettingsActivity.KEY_PREF_SYNC_DUREE, res.getString(R.string.pref_duree_def)).equals("mois")) {
            return true;
        }

        // Are we trying to load an office but only pre-load mass ?
        if(
                ww.what != LecturesController.WHAT.MESSE && ww.what != LecturesController.WHAT.INFORMATIONS &&
                !preference.getString(SettingsActivity.KEY_PREF_SYNC_LECTURES, res.getString(R.string.pref_lectures_def)).equals("messe-offices")
        ) {
            return true;
        }

        // Other settings could influence, but not as much

        return false;
    }

    @Override
    protected void onPostExecute(final List<LectureItem> lectures) {
        final List<LectureItem> pager_data;
        boolean isSuccess = false;

        // Failed to load
        if (lectures == null) {
            if(detectSubOptimalSettings()) {
                pager_data = buildErrorMessage(subOptimalSettingsErrorMessage);
            } else if (NetworkStatusMonitor.getInstance().isNetworkAvailable()) {
                pager_data = buildErrorMessage(connectionErrorMessage);
            } else {
                pager_data = buildErrorMessage(noNetworkErrorMessage);
            }
        } else if (lectures.isEmpty()) {
            pager_data = buildErrorMessage(emptyOfficeErrorMessage);
        } else {
            isSuccess = true;
            pager_data = lectures;
        }

        if(lectureLoadProgressListener != null) {
            lectureLoadProgressListener.onLectureLoaded(pager_data, isSuccess);
        }
    }
}
