package co.epitre.aelf_lectures.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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

// Load lecture from network. Bring cancel and timeout support
public class LectureFuture implements Future<List<LectureItem>> {
    public static final String API_ENDPOINT = "https://api.app.epitre.co";
    private static final String TAG = "LectureFuture";

    /**
     * Internal state
     */
    private SharedPreferences preference = null;
    private String path;

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

    public LectureFuture(Context ctx, String path) throws IOException {
        this.path = path;

        // Grab preferences
        preference = PreferenceManager.getDefaultSharedPreferences(ctx);

        // Mark work start
        startTime = System.nanoTime();
        try {
            Work.acquire();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }

        // Build and start request
        startRequest();
    }

    private void startRequest()  {
        // Re-Init state
        this.pendingLectures = null;
        this.pendingIoException = null;

        // Build feed URL
        boolean pref_nocache = preference.getBoolean("pref_participate_nocache", false);
        String Url = buildUrl();
        Log.d(TAG, "Getting "+Url);

        // Build request + headers
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(Url);
        if (pref_nocache) {
            requestBuilder.addHeader("x-aelf-nocache", "1");
        }
        Request request = requestBuilder.build();

        // Build and enqueue the call
        call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                pendingIoException = e;
                Work.release();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                IOException err = null;
                try {
                    onHttpResponse(response);
                } catch (IOException e) {
                    // Ignore this exception IF we are going to retry
                    err = e;
                } catch (XmlPullParserException e) {
                    // Parser exceptions tends to occur on connection error while parsing
                    err = new IOException(e);
                } finally {
                    Work.release();
                    if (err != null) {
                        throw err;
                    }
                }
            }
        });
    }

    //
    // Accessors
    //

    public String getPath() {
        return this.path;
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
        cancelled = true;
        call.cancel();
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

    private void onHttpResponse(Response response) throws IOException, XmlPullParserException {
        // Grab response
        InputStream in = null;
        try {
            in = response.body().byteStream();
            pendingLectures = AelfRssParser.parse(in);
            if (pendingLectures == null) {
                Log.w(TAG, "Failed to load lectures from network");
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Failed to parse API result", e);
            pendingIoException = new IOException(e);
        } catch (IOException e) {
            Log.w(TAG, "Failed to load lectures from network");
            pendingIoException = e;
        } catch (Exception e) {
            pendingIoException = new IOException(e);
        } finally {
            if(in != null) {
                in.close();
            }
        }

        if (pendingIoException != null) {
            throw pendingIoException;
        }
    }

    //
    // Helpers
    //

    private String buildUrl() {
        boolean pref_beta = preference.getBoolean("pref_participate_beta", false);
        String endpoint = preference.getString("pref_participate_server", "");

        // If the URL was not overloaded, build it
        if (endpoint.equals("")) {
            endpoint = API_ENDPOINT;

            // If applicable, switch to beta
            if (pref_beta) {
                endpoint = endpoint.replaceAll("^(https?://)", "$1beta.");
            }
        }

        return endpoint + this.path;
    }
}
