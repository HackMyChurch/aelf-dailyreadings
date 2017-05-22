package co.epitre.aelf_lectures;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.getsentry.raven.android.Raven;
import com.getsentry.raven.android.event.helper.AndroidEventBuilderHelper;
import com.getsentry.raven.event.BreadcrumbBuilder;
import com.getsentry.raven.event.Breadcrumbs;
import com.getsentry.raven.event.EventBuilder;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.extra.PiwikApplication;
import org.piwik.sdk.extra.TrackHelper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
class DownloadXmlTask extends AsyncTask<WhatWhen, Void, List<LectureItem>> {
    private Context ctx;
    private LectureLoadProgressListener lectureLoadProgressListener;
    private LecturesController lecturesCtrl = null;
    private static final ExecutorService executor = Executors.newCachedThreadPool(Executors.defaultThreadFactory());

    Future<List<LectureItem>> future;
    private WhatWhen statWhatWhen = null;
    boolean statIsFromCache = false; // True is the data came from the cache

    public static final String TAG = "DownloadXmlTask";

    /**
     * Statistics
     */
    Tracker tracker;

    /**
     * Error messages
     */
    private static final String networkErrorMessage = ""+
            "<h3>Oups... Une erreur s'est glissée lors du chargement des lectures</h3>" +
            "<p>Saviez-vous que cette application est développée entièrement bénévolement&nbsp;? Elle est construite en lien et avec le soutien de l'AELF, mais elle reste un projet indépendant, soutenue par <em>votre</em> prière&nbsp!</p>\n" +
            "<p>Si vous pensez qu'il s'agit d'une erreur, vous pouvez envoyer un mail à <a href=\"mailto:cathogeek@epitre.co?subject=Report:%20Network%20error%20loading%20##OFFICE##%20Office%20(version:%20##VERSION##)&body=##REPORT##\">cathogeek@epitre.co</a>.<p>";
    private static final String emptyOfficeErrorMessage = "" +
            "<h3>Oups... Cet office ne contient pas de lectures</h3>" +
            "<p>Cet office ne semble pas contenir de lecture. Si vous pensez qu'il s'agit d'un erreur, vous pouver essayer de \"Rafraîchir\" cet office.</p>" +
            "<p>Saviez-vous que cette application est développée entièrement bénévolement&nbsp;? Elle est construite en lien et avec le soutien de l'AELF, mais elle reste un projet indépendant, soutenue par <em>votre</em> prière&nbsp!</p>\n" +
            "<p>Si vous pensez qu'il s'agit d'une erreur, vous pouvez envoyer un mail à <a href=\"mailto:cathogeek@epitre.co?subject=Report:%20Empty%20%##OFFICE##20Office%20(version:%20##VERSION##)&body=##REPORT##\">cathogeek@epitre.co</a>.<p>";


    public DownloadXmlTask(Context ctx, LectureLoadProgressListener lectureLoadProgressListener) {
        this.ctx = ctx;
        this.tracker = ((PiwikApplication) ctx.getApplicationContext()).getTracker();
        this.lecturesCtrl = LecturesController.getInstance(ctx);
        this.lectureLoadProgressListener = lectureLoadProgressListener;
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
    protected List<LectureItem> doInBackground(WhatWhen... whatwhen) {
        final WhatWhen ww = whatwhen[0];
        statWhatWhen = ww;

        try {
            List<LectureItem> lectures = null;
            if (ww.useCache) {
                // attempt to load from cache: skip loading indicator (avoids flickering)
                // if the cache consider the lecture as outdated, do not return it: we'll try to reload it
                lectures = lecturesCtrl.getLecturesFromCache(ww.what, ww.when, false);
                if (lectures != null) {
                    statIsFromCache = true;
                    return lectures;
                }
            }

            // attempts to load from network, with loading indicator
            onLectureLoadProgress(LectureLoadProgress.LOAD_START);
            future = executor.submit(new Callable<List<LectureItem>>() {
                @Override
                public List<LectureItem> call() {
                    try {
                        return lecturesCtrl.getLecturesFromNetwork(ww.what, ww.when);
                    } catch (IOException e) {
                        // Do nothing: the error has already been reported, if it makes sense
                        return null;
                    }
                }
            });

            // When cancel is called, we first mark as cancelled then check for future
            // but future may be created in the mean time, so recheck here to avoid race
            if (isCancelled()) {
                future.cancel(true);
            }

            // attempt to read the result
            try {
                lectures = future.get();
            } catch (InterruptedException e) {
                // Do not report: this is requested by the user
            } catch (ExecutionException e) {
                Raven.capture(e);
            }

            // If cancel has been called while loading, we'll only catch it here
            if (isCancelled()) {
                return null;
            }

            if (lectures == null) {
                // Failed to load lectures from network AND we were asked to refresh so attempt
                // a fallback on the cache to avoid the big error message but still display a notification
                // If the cache considers the lecture as outdated, still return it. We are in error recovery now
                lectures = lecturesCtrl.getLecturesFromCache(ww.what, ww.when, true);
                statIsFromCache = true;
                onLectureLoadProgress(LectureLoadProgress.LOAD_FAIL);
            }
            return lectures;
        } catch (IOException e) {
            // Error alredy propagated to Sentry. Do not propagate twice !
            Log.e(TAG, "I/O error while loading. AELF servers down ?");
            onLectureLoadProgress(LectureLoadProgress.LOAD_DONE);
            return null;
        }
    }

    private void trackView(String status) {
        long dayDelta = statWhatWhen.when.dayBetween(new GregorianCalendar());

        TrackHelper.track()
                .screen("/office/" + statWhatWhen.what.urlName())
                .title("/office/" + statWhatWhen.what.urlName())
                .dimension(LecturesApplication.STATS_DIM_SOURCE, statIsFromCache ? "cache" : "network")
                .dimension(LecturesApplication.STATS_DIM_STATUS, status)
                .dimension(LecturesApplication.STATS_DIM_DAY_DELTA, Integer.toString((int) dayDelta))
                .dimension(LecturesApplication.STATS_DIM_DAY_NAME, statWhatWhen.when.dayName())
                .with(tracker);
    }

    @Override
    protected void onCancelled(List<LectureItem> lectureItems) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.onCancelled(lectureItems);
        }
        trackView("cancelled");
    }

    private List<LectureItem> buildErrorMessage(String message) {
        List<LectureItem> error = new ArrayList<>(1);

        // Get version name
        String versionName = "";
        try {
            versionName = "v" + ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // Only drawback here is no version displayed in about. Minor anoyance
        }
        message = message.replace("##VERSION##", versionName);

        // Get office name / date
        message = message.replace("##OFFICE##", statWhatWhen.toUrlName());

        // Build detailed report, using data from AelfEventBuilderHelper
        EventBuilder eventBuilder = new EventBuilder();
        new AndroidEventBuilderHelper(ctx).helpBuildingEvent(eventBuilder);
        new AelfEventBuilderHelper(ctx, tracker.getUserId()).helpBuildingEvent(eventBuilder);
        Map<String, Map<String, Object>> contexts = eventBuilder.getEvent().getContexts();

        String report = "";
        report += "Bonjour !\n\n" +
                "Merci d'avoir pris le temps d'envoyer un message pour signaler une erreur !\n\n" +
                "Ce message a été pré-rempli avec les informations dont j'ai habituellement besoin pour diagnostiquer les erreurs. " +
                "Si vous le souhaitez, vous pouvez prendre le temps de les relire ou même les supprimer. Mais cela m'aidera beaucoup si vous les conservez.\n\n" +
                "VOUS POUVEZ AJOUTER UN MESSAGE ICI\n\n";
        report += "Debug informations:\n";
        report += "===================\n";

        for (Map.Entry<String, Map<String, Object>> context : contexts.entrySet()) {
            String key = context.getKey();
            report += "\n" + key + "\n" + new String(new char[key.length()]).replace("\0", "-") + "\n";

            for (Map.Entry<String, Object> entry : context.getValue().entrySet()) {
                Object value = entry.getValue();
                if (value != null) {
                    report += entry.getKey() + "=" + value.toString() + "\n";
                } else {
                    report += entry.getKey() + "=null\n";
                }
            }
        }

        try {
            message = message.replace("##REPORT##", URLEncoder.encode(report, "utf-8").replace("+", "%20"));
        } catch (UnsupportedEncodingException e) {
            // That's exactly the same informations as we would have sent, except that the user has no chance to give us extra info
            Breadcrumbs.record(new BreadcrumbBuilder().setMessage("Building error report for " + statWhatWhen.toUrlName()).build());
            Raven.capture(e);
        }

        // Build and return error
        error.add(new LectureItem("error", "Oups...", message, null));
        return error;
    }

    @Override
    protected void onPostExecute(final List<LectureItem> lectures) {
        final List<LectureItem> pager_data;
        boolean isSuccess = false;

        // Failed to load
        if (lectures == null) {
            trackView("error");
            pager_data = buildErrorMessage(networkErrorMessage);
        } else if (lectures.isEmpty()) {
            trackView("empty");
            pager_data = buildErrorMessage(emptyOfficeErrorMessage);
            ;
        } else {
            trackView("success");
            isSuccess = true;
            pager_data = lectures;
        }

        if(lectureLoadProgressListener != null) {
            lectureLoadProgressListener.onLectureLoaded(pager_data, isSuccess);
        }
    }
}