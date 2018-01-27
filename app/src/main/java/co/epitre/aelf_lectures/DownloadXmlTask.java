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
import java.util.concurrent.ExecutionException;

import co.epitre.aelf_lectures.data.AelfDate;
import co.epitre.aelf_lectures.data.LectureFuture;
import co.epitre.aelf_lectures.data.LectureItem;
import co.epitre.aelf_lectures.data.LecturesController;
import co.epitre.aelf_lectures.data.WhatWhen;

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

    LectureFuture future;
    private WhatWhen ww = null;
    String statLectureSource = "unknown";

    public static final String TAG = "DownloadXmlTask";

    /**
     * Error messages
     */
    private static final String noNetworkErrorMessage = ""+
            "<h3>Aucune connexion Internet n’est disponible</h3>" +
            "<p>L’office que vous ne demandez n’est pas disponible en mode hors-connexion. Pour le consulter, assurez-vous de disposer d’une connexion Internet de bonne qualité, de préférence en WiFi, puis ré-essayez.</p>" +
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
            List<LectureItem> lectures = null;
            boolean isNetworkAvailable = NetworkStatusMonitor.getInstance().isNetworkAvailable();

            // When the network is not available, always try to load from cache, even if outdated.
            if (ww.useCache || !isNetworkAvailable) {
                boolean allowColdCache = !isNetworkAvailable;

                // attempt to load from cache: skip loading indicator (avoids flickering)
                // if the cache consider the lecture as outdated, do not return it: we'll try to reload it
                lectures = lecturesCtrl.getLecturesFromCache(ww.what, ww.when, allowColdCache);
                if (lectures != null) {
                    statLectureSource = "cache";
                    return lectures;
                }
            }

            // attempts to load from network, with loading indicator
            onLectureLoadProgress(LectureLoadProgress.LOAD_START);
            future = lecturesCtrl.getLecturesFromNetwork(ww.what, ww.when);

            // When cancel is called, we first mark as cancelled then check for future
            // but future may be created in the mean time, so recheck here to avoid race
            if (isCancelled()) {
                future.cancel(true);
            }

            try {
                lectures = future.get();
                statLectureSource = "network";
            } catch (InterruptedException e) {
                // Do not report: this is requested by the user
            } catch (ExecutionException e) {
                // Do not report: already done
            }

            // If cancel has been called while loading, we'll only catch it here
            if (isCancelled()) {
                return null;
            }

            // Fallback: cold cache
            if (lectures == null) {
                // Failed to load lectures from network AND we were asked to refresh so attempt
                // a fallback on the cache to avoid the big error message but still display a notification
                // If the cache considers the lecture as outdated, still return it. We are in error recovery now
                lectures = lecturesCtrl.getLecturesFromCache(ww.what, ww.when, true);
                statLectureSource = "cache";
                onLectureLoadProgress(LectureLoadProgress.LOAD_FAIL);
            }

            // Fallback: static asset
            if (lectures == null) {
                lectures = lecturesCtrl.loadLecturesFromAssets(ww.what, ww.when);
                statLectureSource = "asset";
            }

            return lectures;
        } catch (IOException e) {
            // Error already propagated to Sentry. Do not propagate twice !
            Log.e(TAG, "I/O error while loading. AELF servers down ?");
            onLectureLoadProgress(LectureLoadProgress.LOAD_DONE);
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
        if(!preference.getString(SyncPrefActivity.KEY_PREF_PARTICIPATE_SERVER, "").equals("")) {
            return true;
        }

        // Are we syncing for a month ?
        if(!preference.getString(SyncPrefActivity.KEY_PREF_SYNC_DUREE, res.getString(R.string.pref_duree_def)).equals("mois")) {
            return true;
        }

        // Are we trying to load an office but only pre-load mass ?
        if(
                ww.what != LecturesController.WHAT.MESSE && ww.what != LecturesController.WHAT.METAS &&
                !preference.getString(SyncPrefActivity.KEY_PREF_SYNC_LECTURES, res.getString(R.string.pref_lectures_def)).equals("messe-offices")
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
