package co.epitre.aelf_lectures.lectures.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import co.epitre.aelf_lectures.settings.SettingsActivity;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;

/**
 * Created by jean-tiare on 16/03/18.
 */

public final class EpitreApi {
    public static final String API_ENDPOINT = "https://api.app.epitre.co";
    private static final String TAG = "EpitreApi";

    /**
     * Internal state
     */
    private static volatile EpitreApi instance = null;
    private SharedPreferences preference = null;

    /**
     * HTTP Client
     */
    private Call call = null;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Was 60 seconds
            .writeTimeout  (60, TimeUnit.SECONDS) // Was 10 minutes
            .readTimeout   (60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<Office> officeJsonAdapter = moshi.adapter(Office.class);

    /**
     * Singleton
     */

    private EpitreApi(Context c) {
        super();

        preference = PreferenceManager.getDefaultSharedPreferences(c);
    }

    public static EpitreApi getInstance(Context c) {
        if (EpitreApi.instance == null) {
            synchronized(EpitreApi.class) {
                if (EpitreApi.instance == null) {
                    EpitreApi.instance = new EpitreApi(c);
                }
            }
        }
        return EpitreApi.instance;
    }

    /**
     * Requests internals
     */

    private Response InternalGet(String path) throws IOException {
        // Load request engine configuration
        boolean pref_nocache = preference.getBoolean("pref_participate_nocache", false);
        boolean pref_beta = preference.getBoolean("pref_participate_beta", false);
        String endpoint = preference.getString("pref_participate_server", "");

        // Build url
        if (endpoint.equals("")) {
            endpoint = API_ENDPOINT;

            // If applicable, switch to beta
            if (pref_beta) {
                endpoint = endpoint.replaceAll("^(https?://)", "$1beta.");
            }
        }

        String Url = endpoint + path;
        Log.d(TAG, "Getting "+Url);

        // Build request
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(Url);
        if (pref_nocache) {
            requestBuilder.addHeader("x-aelf-nocache", "1");
        }
        Request request = requestBuilder.build();

        return client.newCall(request).execute();
    }

    /**
     * Public API
     */

    public List<LectureItem> getOffice(String officeName, String date) throws IOException {
        // Load configuration
        String path = "/%d/office/%s/%s.json?region=%s";
        int version = preference.getInt("version", -1);

        // Build URL
        String region = preference.getString(SettingsActivity.KEY_PREF_REGION, "romain");
        path = String.format(Locale.US, path, version, officeName, date, region);

        // Issue request
        Response response = InternalGet(path);

        // Grab response
        BufferedSource source = null;
        Office office = null;
        try {
            source = response.body().source();
            office = officeJsonAdapter.fromJson(response.body().source());
        } catch (IOException e) {
            Log.w(TAG, "Failed to load lectures from network");
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if(source != null) {
                source.close();
            }
        }

        // Compat: concert office to legacy List<LectureItem>
        List<LectureItem> legacyLectures = new ArrayList<>();
        for (OfficeVariant officeVariant: office.variants) {
            for (List<Lecture> lectureVariant: officeVariant.lectures) {
                Lecture lecture = lectureVariant.get(0);
                
                LectureItem legacyLecture = new LectureItem(
                        lecture.getKey(),
                        lecture.getShortTitle(),
                        lecture.toHtml(),
                        lecture.getReference()
                );
                legacyLectures.add(legacyLecture);
            }
        }
        return legacyLectures;
    }
}
