package co.epitre.aelf_lectures.data;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import co.epitre.aelf_lectures.NetworkStatusMonitor;
import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.SyncPrefActivity;

/**
 * Public data controller --> load either from cache, either from network
 */

public final class LecturesController {

    /**
     * "What to sync" constants
     */

    public enum WHAT {
        MESSE   (0, R.id.nav_mass,        "lectures_messe"),
        LECTURES(1, R.id.nav_lectures,    "lectures_lectures"),
        LAUDES  (2, R.id.nav_laudes,      "lectures_laudes"),
        TIERCE  (3, R.id.nav_tierce,      "lectures_tierce"),
        SEXTE   (4, R.id.nav_sexte,       "lectures_sexte"),
        NONE    (5, R.id.nav_none,        "lectures_none"),
        VEPRES  (6, R.id.nav_vepres,      "lectures_vepres"),
        COMPLIES(7, R.id.nav_complies,    "lectures_complies"),
        METAS   (8, R.id.nav_information, "lectures_metas");

        private String name = "";
        private int position = 0; // FIXME: remove field
        private int menu_id;

        WHAT(int position, int menu_id, String name) {
            this.menu_id = menu_id;
            this.position = position;
            this.name = name;
        }

        public static WHAT fromMenuId(int menu_id) {
            for (WHAT what : WHAT.values()) {
                if(what.menu_id == menu_id) {
                    return what;
                }
            }
            return null;
        }

        public String urlName() {
            if(this.position == 0) {
                return "messes";
            }
            if(this.position == 8) {
                return "informations";
            }
            return this.name.split("_")[1];
        }

        public String aelfUrlName() {
            // There is no specific page for the informations, link to the mass
            if(this.position == 8) {
                return "messe";
            }
            return this.name.split("_")[1];
        }

        public String actionBarName() {
            if(this.position == 6) {
                return "Vêpres";
            }
            if(this.position == 8) {
                return "Informations";
            }

            String name = this.name.split("_")[1];
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
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

        // FIXME: remove
        public int getPosition(){
            return position;
        }

        public int getMenuId(){
            return menu_id;
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
    private EpitreApi api = null;
    Context ctx;

    private LecturesController(Context c) {
        super();

        ctx = c;
        api = EpitreApi.getInstance(c);
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
            return false;
        }
    }

    public List<LectureItem> loadLecturesFromCache(WHAT what, AelfDate when, boolean allowColdCache) throws IOException {
        List<LectureItem> lectures;
        AelfDate minLoadDate = null;
        long minLoadVersion = -1;

        if (!allowColdCache) {
            minLoadVersion = preference.getInt(SyncPrefActivity.KEY_APP_CACHE_MIN_VERSION, -1);
            minLoadDate = new AelfDate(preference.getLong(SyncPrefActivity.KEY_APP_CACHE_MIN_DATE, 0));
        }

        try {
            lectures = cache.load(what, when, minLoadDate, minLoadVersion);
        } catch (RuntimeException e) {
            // gracefully recover when DB stream outdated/corrupted by refreshing
            Log.e(TAG, "Loading lecture from cache crashed ! Recovery by refreshing...", e);
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

    public List<LectureItem> loadLecturesFromNetwork(WHAT what, AelfDate when) throws IOException {
        // Load lectures
        List<LectureItem> lectures = api.getOffice(what.urlName(), when.toIsoString());

        // Cache lectures
        if(!looksLikeError(lectures)) {
            try {
                cache.store(what.urlName(), when.toIsoString(), lectures);
            } catch (IOException e) {
                Log.e(TAG, "Failed to store lecture in cache", e);
            }
        }

        return lectures;
    }

    public List<LectureItem> loadLectures(WHAT what, AelfDate when, boolean useCache) throws IOException {
        List<LectureItem> lectures = null;
        boolean isNetworkAvailable = NetworkStatusMonitor.getInstance().isNetworkAvailable();

        // When the network is not available, always try to load from cache, even if outdated.
        if (useCache || !isNetworkAvailable) {
            boolean allowColdCache = !isNetworkAvailable;

            // attempt to load from cache: skip loading indicator (avoids flickering)
            // if the cache consider the lecture as outdated, do not return it: we'll try to reload it
            lectures = loadLecturesFromCache(what, when, allowColdCache);
            if (lectures != null) {
                return lectures;
            }
        }

        lectures = loadLecturesFromNetwork(what, when);

        // Fallback: cold cache
        if (lectures == null) {
            // Failed to load lectures from network AND we were asked to refresh so attempt
            // a fallback on the cache to avoid the big error message but still display a notification
            // If the cache considers the lecture as outdated, still return it. We are in error recovery now
            lectures = loadLecturesFromCache(what, when, true);
        }

        return lectures;
    }

    // re-export cleanup helper
    public void truncateBefore(GregorianCalendar when) {
        WHAT[] whatValues = WHAT.values();

        try {
            for (WHAT whatValue : whatValues) {
                cache.truncateBefore(whatValue, when);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to truncate lecture from cache", e);
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
