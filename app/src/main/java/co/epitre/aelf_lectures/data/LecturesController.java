package co.epitre.aelf_lectures.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.SyncPrefActivity;

/**
 * Public data controller --> load either from cache, either from network
 */

public final class LecturesController implements LectureFutureProgressListener {

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
        METAS   (8, R.id.nav_information, "lectures_informations");

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
    Context ctx;

    private LecturesController(Context c) {
        super();

        ctx = c;
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

    /**
     * Build request path from a lecture type and and date. The path starts with a '/' and does not
     * include the domain name. It is intended to be used both as a cache key and the actual request
     * path, hence his place in the controller.
     * @param what
     * @param when
     */
    public String buildPath(WHAT what, AelfDate when) {
        String path = "/%d/office/%s/%s.rss?region=%s";

        // Fill placeholders
        String region = preference.getString(SyncPrefActivity.KEY_PREF_REGION, "romain");
        path = String.format(Locale.US, path,
                preference.getInt("version", -1),
                what.urlName(),
                when.toIsoString(),
                region
        );

        return path;
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

    public List<LectureItem> getLecturesFromCache(WHAT what, AelfDate when, boolean allowColdCache) throws IOException {
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

    // Last resort: attempt to load the lecture from the static / built-in asset folder
    public List<LectureItem> loadLecturesFromAssets(WHAT what, AelfDate when) {
        String filename = "preloaded-reading/"+what.urlName()+"_"+when.toIsoString()+".rss";
        InputStream in = null;

        try {
            in = ctx.getAssets().open(filename);
            return AelfRssParser.parse(in);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public LectureFuture getLecturesFromNetwork(WHAT what, AelfDate when) throws IOException {
        // Load a lecture. When the lecture is ready, call this.onLectureLoaded to cache it
        return new LectureFuture(ctx, buildPath(what, when), this);
    }

    @Override
    public void onLectureLoaded(String path, List<LectureItem> lectures) {
        // does it look like an error message ? Only simple stupid heuristic for now.
        if(!looksLikeError(lectures)) {
            try {
                // HACK: the cache will be rewritten to use the path itself. In the mean time, we need
                // to rebuild the expected what and when as strings
                String[] chunks = path.split("/");
                String what_str = chunks[3];
                String[] when_chunks = chunks[4].split("\\.", 2);
                String when_str = when_chunks[0];

                if (what_str.equals("messes")) {
                    what_str = "messe";
                }
                what_str = "lectures_" + what_str;

                cache.store(what_str, when_str, lectures);
            } catch (IOException e) {
                Log.e(TAG, "Failed to store lecture in cache", e);
            }
        }
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
