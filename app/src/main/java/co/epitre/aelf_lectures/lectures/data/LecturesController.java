package co.epitre.aelf_lectures.lectures.data;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import co.epitre.aelf_lectures.components.NetworkStatusMonitor;
import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.lectures.data.api.EpitreApi;
import co.epitre.aelf_lectures.lectures.data.api.OfficeResponse;
import co.epitre.aelf_lectures.lectures.data.cache.Cache;
import co.epitre.aelf_lectures.lectures.data.cache.CacheEntry;
import co.epitre.aelf_lectures.lectures.data.cache.CacheEntries;
import co.epitre.aelf_lectures.lectures.data.office.Office;
import co.epitre.aelf_lectures.lectures.data.office.OfficesChecksums;

/**
 * Public data controller --> load either from cache, either from network
 */

public final class LecturesController {

    /**
     * This class is a manager --> Singleton
     */
    private static final String TAG = "LectureController";
    private int apiVersion;
    private static volatile LecturesController instance = null;
    private Cache cache = null;
    private EpitreApi api = null;
    Context ctx;

    private LecturesController(Context c) {
        super();

        ctx = c;
        api = EpitreApi.getInstance(c);
        cache = new Cache(c);
        apiVersion = c.getResources().getInteger(R.integer.api_version);
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

    public CacheEntries listCachedEntries(AelfDate since) {
        return cache.listCachedEntries(since, apiVersion);
    }

    public OfficesChecksums loadOfficesChecksums(AelfDate since, int days) throws IOException {
        return api.getOfficesChecksums(since, days, apiVersion);
    }

    public Office loadLecturesFromCache(OfficeTypes what, AelfDate when, boolean allowColdCache) throws IOException {
        long minLoadVersion = -1;

        if (!allowColdCache) {
            minLoadVersion = apiVersion;
        }

        try {
            CacheEntry cacheEntry = cache.load(what, when, minLoadVersion);
            if (cacheEntry == null) {
                return null;
            }
            return cacheEntry.office();
        } catch (RuntimeException e) {
            // gracefully recover when DB stream outdated/corrupted by refreshing
            Log.e(TAG, "Loading lecture from cache crashed ! Recovery by refreshing...", e);
            return null;
        }
    }

    public Office loadLecturesFromNetwork(OfficeTypes what, AelfDate when) throws IOException {
        // Load lectures
        OfficeResponse response = api.getOffice(what.apiName(), when.toIsoString(), apiVersion);

        // Cache lectures
        try {
            cache.store(what, when, response.office(), response.checksum(), response.generationDate(), apiVersion);
        } catch (IOException e) {
            Log.e(TAG, "Failed to store lecture in cache", e);
        }

        return response.office();
    }

    public Office loadLectures(OfficeTypes what, AelfDate when, boolean useCache) throws IOException {
        Office office = null;
        boolean isNetworkAvailable = NetworkStatusMonitor.getInstance().isNetworkAvailable();

        // When the network is not available, always try to load from cache, even if outdated.
        if (useCache || !isNetworkAvailable) {
            boolean allowColdCache = !isNetworkAvailable;

            // attempt to load from cache: skip loading indicator (avoids flickering)
            // if the cache consider the lecture as outdated, do not return it: we'll try to reload it
            office = loadLecturesFromCache(what, when, allowColdCache);
            if (office != null) {
                return office;
            }
        }

        office = loadLecturesFromNetwork(what, when);

        // Fallback: cold cache
        if (office == null) {
            // Failed to load lectures from network AND we were asked to refresh so attempt
            // a fallback on the cache to avoid the big error message but still display a notification
            // If the cache considers the lecture as outdated, still return it. We are in error recovery now
            office = loadLecturesFromCache(what, when, true);
        }

        return office;
    }

    // re-export cleanup helper
    public void truncateBefore(AelfDate when) {
        try {
            cache.truncateBefore(when);
        } catch (IOException e) {
            Log.e(TAG, "Failed to truncate lecture from cache", e);
        }
    }

    public long getDatabaseSize() {
        return cache.getDatabaseSize();
    }

    public void dropDatabase() {
        cache.dropDatabase();
    }
}
