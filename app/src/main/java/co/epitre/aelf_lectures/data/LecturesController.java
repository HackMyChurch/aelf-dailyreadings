package co.epitre.aelf_lectures.data;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.extra.PiwikApplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.getsentry.raven.android.Raven;

/**
 * Public data controller --> load either from cache, either from network
 */

public final class LecturesController {

    /**
     * Statistics
     */
    Tracker tracker;

    /**
     * "What to sync" constants
     */

    public enum WHAT {
        MESSE   (0, "lectures_messe",    "/%s/"+Credentials.API_KEY_MESSE),
        LECTURES(1, "lectures_lectures", "/%s/"+Credentials.API_KEY_LECTURES),
        LAUDES  (2, "lectures_laudes",   "/%s/"+Credentials.API_KEY_LAUDES),
        TIERCE  (3, "lectures_tierce",   "/%s/"+Credentials.API_KEY_TIERCE),
        SEXTE   (4, "lectures_sexte",    "/%s/"+Credentials.API_KEY_SEXTE),
        NONE    (5, "lectures_none",     "/%s/"+Credentials.API_KEY_NONE),
        VEPRES  (6, "lectures_vepres",   "/%s/"+Credentials.API_KEY_VEPRES),
        COMPLIES(7, "lectures_complies", "/%s/"+Credentials.API_KEY_COMPLIES),
        METAS   (8, "lectures_metas",    "/%s/"+Credentials.API_KEY_METAS);

        private String name = "";
        private String url = "";
        private int position = 0;

        WHAT(int position, String name, String url) {
            this.position = position;
            this.name = name;
            this.url = url;
        }

        public String getRelativeUrl() {
            return url;
        }

        public String urlName() {
            return this.name.split("_")[1];
        }

        public String prettyName() {
            if (this.position == 0) {
                return "de la Messe";
            }

            String name = this.urlName();

            if (name.charAt(name.length()-1) == 's') {
                return "de l'office des "+name;
            } else {
                return "de l'office de "+name;
            }
        }

        public int getPosition(){
            return position;
        }

        public String toString(){
            return name;
        }
    }


    /**
     * This class is a manager --> Singleton
     */
    private static final String TAG = "LectureController";
    private SharedPreferences preference = null;
    private static volatile LecturesController instance = null;
    private AelfCacheHelper cache = null;
    Context ctx;

    private LecturesController(Context c) {
        super();

        ctx = c;
        tracker = ((PiwikApplication) c.getApplicationContext()).getTracker();
        cache = new AelfCacheHelper(c);
        preference = PreferenceManager.getDefaultSharedPreferences(c);
    }
    public static LecturesController getInstance(Context c) {
        if (LecturesController.instance == null) {
            synchronized(LecturesController.class) {
               if (LecturesController.instance == null) {
                 LecturesController.instance = new LecturesController(c);
             }
           }
        }
        return LecturesController.instance;
    }

    public boolean isLecturesInCache(WHAT what, AelfDate when, boolean allowColdCache) {
        GregorianCalendar minLoadDate = null;
        long minLoadVersion = allowColdCache ? -1 : preference.getInt("min_cache_version", -1);

        try {
            return cache.has(what, when, minLoadDate, minLoadVersion);
        } catch (Exception e) {
            Log.e(TAG, "Failed to check if lecture is in cache", e);
            Raven.capture(e);
            return false;
        }
    }

    public List<LectureItem> getLecturesFromCache(WHAT what, AelfDate when, boolean allowColdCache) throws IOException {
        List<LectureItem> lectures;
        GregorianCalendar minLoadDate = null;
        long minLoadVersion = allowColdCache ? -1 : preference.getInt("min_cache_version", -1);

        try {
            lectures = cache.load(what, when, minLoadDate, minLoadVersion);
        } catch (RuntimeException e) {
            // gracefully recover when DB stream outdated/corrupted by refreshing
            Log.e(TAG, "Loading lecture from cache crashed ! Recovery by refreshing...", e);
            Raven.capture(e);
            return null;
        }

        // on error or if cached value looks like an error (not yet in AELF
        // calendar for instance), force reload of live data.
        // Need this heuristic after a cache load as previous versions erroneously cached
        // these.
        if(lectures != null && !looksLikeError(lectures)) {
            return lectures;
        }
        
        return null;
    }

    public LectureFuture getLecturesFromNetwork(WHAT what, AelfDate when) throws IOException {
        LectureFuture future = new LectureFuture(ctx, what, when, new LectureFutureProgressListener() {
            @Override
            public void onLectureLoaded(WHAT what, AelfDate when, List<LectureItem> lectures) {
                // does it look like an error message ? Only simple stupid heuristic for now.
                if(!looksLikeError(lectures)) {
                    cache.store(what, when, lectures);
                }
            }
        });

        return future;
    }

    // re-export cleanup helper
    public void truncateBefore(GregorianCalendar when) {
        WHAT[] whatValues = WHAT.values();

        for (WHAT whatValue : whatValues) {
            cache.truncateBefore(whatValue, when);
        }
    }

    /**
     * Helpers
     */

    private boolean looksLikeError(List<LectureItem> lectures) {
        // does it look like an error message ? Only simple stupid heuristic for now.
        if(lectures.size() > 1) {
            return false;
        }

        if(lectures.size() == 1 && !lectures.get(0).longTitle.contains("pas dans notre calendrier")) {
            return false;
        }

        return true;
    }

}
