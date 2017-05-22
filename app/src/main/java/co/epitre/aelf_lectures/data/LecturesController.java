package co.epitre.aelf_lectures.data;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.extra.PiwikApplication;
import org.piwik.sdk.extra.TrackHelper;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.getsentry.raven.android.Raven;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Public data controller --> load either from cache, either from network
 */

public final class LecturesController {
    /**
     * HTTP Client
     */
    private final OkHttpClient client;

    /**
     * Statistics
     */
    Tracker tracker;

    /**
     * Simple exception class to propagate error names to statistics handler
     */
    class DownloadException extends Exception {
        public String name;
        DownloadException(String name) {
            super();
            this.name = name;
        }
    }

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

        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // Was 60 seconds
                .writeTimeout  (60, TimeUnit.SECONDS) // Was 10 minutes
                .readTimeout   (60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

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

    public List<LectureItem> getLecturesFromCache(WHAT what, GregorianCalendar when, boolean allowColdCache) throws IOException {
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

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public List<LectureItem> getLecturesFromNetwork(WHAT what, AelfDate when) throws IOException {
        List<LectureItem> lectures;

        // fallback to network load
        long startTime = System.nanoTime();
        String errorName = "success";
        try {
            lectures = loadFromNetwork(what, when);
            if (lectures == null) {
                errorName = "error.generic";
                Log.w(TAG, "Failed to load lectures from network");
                return null;
            }
        } catch (DownloadException e) {
            errorName = "error."+e.name;
            Log.w(TAG, "Failed to load lectures from network");
            // No capture here, already done in callee
            return null;
        } catch (IOException e) {
            errorName = "error.io";
            Log.w(TAG, "Failed to load lectures from network");
            if (isNetworkAvailable()) {
                Raven.capture(e);
            }
            throw e;
        } catch (Exception e) {
            errorName = "error."+e.getClass().getName();
            Raven.capture(e);
            throw e;
        } finally {
            // Push event
            float deltaTime = (System.nanoTime() - startTime) / 1000;
            long dayDelta = when.dayBetween(new GregorianCalendar());

            // Disable success reporting, this is too noisy
            if (!errorName.equals("success")) {
                TrackHelper.track().event("Office", "download." + errorName).name(what.urlName() + "." + dayDelta).value(deltaTime).with(tracker);
            }
        }

        // does it look like an error message ? Only simple stupid heuristic for now.
        if(!looksLikeError(lectures)) {
            cache.store(what, when, lectures);
        }

        return lectures;
    }

    // re-export cleanup helper
    public void truncateBefore(GregorianCalendar when) {
        WHAT[] whatValues = WHAT.values();

        for (WHAT whatValue : whatValues) {
            cache.truncateBefore(whatValue, when);
        }
    }

    /**
     * Real Work
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

    // real work internal var
    private static final SimpleDateFormat formater = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

    // Get Server URL. Supports developer mode where you want to run a server locally
    private String getBasedUrl() {
        boolean pref_beta = preference.getBoolean("pref_participate_beta", false);
        String baseUrl = preference.getString("pref_participate_server", "");

        // Manual overload: stop here
        if (!baseUrl.equals("")) {
            return baseUrl;
        }

        // Load default URL
        baseUrl = Credentials.API_ENDPOINT;

        // If applicable, switch to beta
        if (pref_beta) {
            baseUrl = baseUrl.replaceAll("^(https?://)", "$1beta.");
        }
        return baseUrl;
    }

    // Build final URL for an office
    private String getUrl(WHAT what) {
        return getBasedUrl()+what.getRelativeUrl();
    }

    // Attempts to load from network
    // throws IOException to allow for auto retry.
    private List<LectureItem> loadFromNetwork(WHAT what, AelfDate when) throws IOException, DownloadException {
        List<LectureItem> lectures = null;

        // Build feed URL
        int version = preference.getInt("version", -1);
        boolean pref_nocache = preference.getBoolean("pref_participate_nocache", false);

        String url = String.format(Locale.US, getUrl(what)+"?version=%d", formater.format(when.getTime()), version);
        Log.d(TAG, "Getting "+url);

        // Build request + headers
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);
        if (pref_nocache) {
            requestBuilder.addHeader("x-aelf-nocache", "1");
        }
        Request request = requestBuilder.build();

        // Grab response
        InputStream in = null;
        Call call = null;
        Response response = null;
        try {
            // Grab response
            call = client.newCall(request);
            response = call.execute();
            in = response.body().byteStream();

            // Parse response
            lectures = AelfRssParser.parse(in);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Failed to parse API result", e);
            Raven.capture(e);
            throw new DownloadException("parse");
        } finally {
            try {
                if(call     != null) call.cancel();
                if(response != null) response.close();
                if(in       != null) in.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close API connection", e);
            }
        }

        return lectures;
    }

}
