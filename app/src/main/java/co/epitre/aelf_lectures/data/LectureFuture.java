package co.epitre.aelf_lectures.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.getsentry.raven.android.Raven;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.extra.PiwikApplication;
import org.piwik.sdk.extra.TrackHelper;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by jean-tiare on 22/05/17.
 */

interface LectureFutureProgressListener {
    void onLectureLoaded(LecturesController.WHAT what, AelfDate when, List<LectureItem> lectures);
}

// Load lecture from network. Bring cancel and timeout support
public class LectureFuture implements Future<List<LectureItem>> {
    private static final String TAG = "LectureFuture";

    /**
     * Internal state
     */
    private long retryBudget = 3;
    private Context ctx;
    private SharedPreferences preference = null;
    public LecturesController.WHAT what;
    public AelfDate when;

    /**
     * Statistics
     */
    Tracker tracker;

    /**
     * HTTP Client
     */
    private long startTime;
    private Call call = null;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Was 60 seconds
            .writeTimeout  (60, TimeUnit.SECONDS) // Was 10 minutes
            .readTimeout   (60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    /**
     * Pending result
     */
    private boolean cancelled = false;
    private Semaphore Work = new Semaphore(1);
    private IOException pendingIoException = null;
    private List<LectureItem> pendingLectures = null;
    private LectureFutureProgressListener listener;

    public LectureFuture(Context ctx, LecturesController.WHAT what, AelfDate when, LectureFutureProgressListener listener) throws IOException {
        this.listener = listener;
        this.what = what;
        this.when = when;
        this.ctx = ctx;

        // Grab preferences
        preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        tracker = ((PiwikApplication) ctx.getApplicationContext()).getTracker();

        // Build feed URL
        boolean pref_nocache = preference.getBoolean("pref_participate_nocache", false);
        String Url = buildUrl(what, when);
        Log.d(TAG, "Getting "+Url);

        // Build request + headers
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(Url);
        if (pref_nocache) {
            requestBuilder.addHeader("x-aelf-nocache", "1");
        }
        Request request = requestBuilder.build();

        // Mark work start
        startTime = System.nanoTime();
        try {
            Work.acquire();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }

        // Build and enqueue the call
        call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                pendingIoException = e;
                Work.release();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    onHttpResponse(call, response);
                } finally {
                    Work.release();
                }
            }
        });
    }

    public LectureFuture createRetry() {
        if (!isDone()) {
            throw new RuntimeException("Can not retry a pending task");
        }

        if (retryBudget <= 0) {
            throw new RuntimeException("Too many retries");
        }

        if (!isNetworkAvailable()) {
            throw new RuntimeException("Network is not available. Retry is pointless !");
        }

        LectureFuture future;
        try {
            future = new LectureFuture(ctx, what, when, listener);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        future.retryBudget = retryBudget--;
        return future;
    }

    //
    // Future Interface
    //

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone()) {
            return false;
        }
        Log.i(TAG, "Cancelling future");
        call.cancel();
        cancelled = true;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return cancelled || Work.availablePermits() > 0;
    }

    @Override
    public List<LectureItem> get() throws InterruptedException, ExecutionException {
        if (cancelled) {
            Log.i(TAG, "Getting cancelled result");
            throw new CancellationException();
        }

        // Wait for work to complete
        Work.acquire();
        Work.release();

        // Check for pending exception
        if (pendingIoException != null) {
            Log.i(TAG, "Getting exception result");
            throw new ExecutionException(pendingIoException);
        }

        // Return result
        Log.i(TAG, "Getting result");
        return pendingLectures;
    }

    @Override
    public List<LectureItem> get(long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (cancelled) {
            throw new CancellationException();
        }

        // Wait for work to complete
        if (!Work.tryAcquire(timeout, unit)) {
            throw  new TimeoutException();
        }
        Work.release();

        // Check for pending exception
        if (pendingIoException != null) {
            throw new ExecutionException(pendingIoException);
        }

        // Return result
        return pendingLectures;
    }

    //
    // HTTP Request callbacks
    //

    private void onHttpResponse(Call call, Response response) throws IOException {
        // Grab response
        InputStream in = null;
        String errorName = "unknown";
        try {
            in = response.body().byteStream();
            pendingLectures = AelfRssParser.parse(in);
            if (pendingLectures == null) {
                errorName = "error.generic";
                Log.w(TAG, "Failed to load lectures from network");
            } else {
                errorName = "success";
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Failed to parse API result", e);
            errorName = "error.parse";
            Raven.capture(e);
            pendingIoException = new IOException(e);
        } catch (IOException e) {
            errorName = "error.io";
            Log.w(TAG, "Failed to load lectures from network");
            if (isNetworkAvailable()) {
                Raven.capture(e);
            }
            pendingIoException = e;
        } catch (Exception e) {
            errorName = "error."+e.getClass().getName();
            Raven.capture(e);
            pendingIoException = new IOException(e);
        } finally {
            if(in != null) {
                in.close();
            }
            trackDownloadEvent(errorName);
        }

        if (pendingIoException != null) {
            throw pendingIoException;
        }

        if (listener != null) {
            listener.onLectureLoaded(what, when, pendingLectures);
        }
    }

    //
    // Helpers
    //

    private String buildUrl(LecturesController.WHAT what, AelfDate when) {
        boolean pref_beta = preference.getBoolean("pref_participate_beta", false);
        String Url = preference.getString("pref_participate_server", "");

        // If the URL was not overloaded, build it
        if (Url.equals("")) {
            Url = Credentials.API_ENDPOINT;

            // If applicable, switch to beta
            if (pref_beta) {
                Url = Url.replaceAll("^(https?://)", "$1beta.");
            }
        }

        // Append path + date placeholder
        Url += what.getRelativeUrl();

        // Append version placeholder
        Url += "?version=%d";

        // Fill placeholders
        int version = preference.getInt("version", -1);
        Url = String.format(Locale.US, Url, when.toUrlString(), version);

        return Url;
    }

    private void trackDownloadEvent(String errorName) {
        // Push event
        float deltaTime = (System.nanoTime() - startTime) / 1000;
        long dayDelta = when.dayBetween(new GregorianCalendar());

        // Disable success reporting, this is too noisy
        if (!errorName.equals("success")) {
            TrackHelper.track().event("Office", "download." + errorName).name(what.urlName() + "." + dayDelta).value(deltaTime).with(tracker);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
