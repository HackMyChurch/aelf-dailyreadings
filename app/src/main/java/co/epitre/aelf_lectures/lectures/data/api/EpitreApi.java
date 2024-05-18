package co.epitre.aelf_lectures.lectures.data.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import co.epitre.aelf_lectures.lectures.data.AelfDate;
import co.epitre.aelf_lectures.lectures.data.office.Office;
import co.epitre.aelf_lectures.lectures.data.office.OfficesChecksums;
import co.epitre.aelf_lectures.settings.SettingsActivity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.tls.HandshakeCertificates;
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
     * TLS trust store
     */

    private static final String ISGRootX1PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw\n" +
            "TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh\n" +
            "cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4\n" +
            "WhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu\n" +
            "ZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBY\n" +
            "MTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rVygc\n" +
            "h77ct984kIxuPOZXoHj3dcKi/vVqbvYATyjb3miGbESTtrFj/RQSa78f0uoxmyF+\n" +
            "0TM8ukj13Xnfs7j/EvEhmkvBioZxaUpmZmyPfjxwv60pIgbz5MDmgK7iS4+3mX6U\n" +
            "A5/TR5d8mUgjU+g4rk8Kb4Mu0UlXjIB0ttov0DiNewNwIRt18jA8+o+u3dpjq+sW\n" +
            "T8KOEUt+zwvo/7V3LvSye0rgTBIlDHCNAymg4VMk7BPZ7hm/ELNKjD+Jo2FR3qyH\n" +
            "B5T0Y3HsLuJvW5iB4YlcNHlsdu87kGJ55tukmi8mxdAQ4Q7e2RCOFvu396j3x+UC\n" +
            "B5iPNgiV5+I3lg02dZ77DnKxHZu8A/lJBdiB3QW0KtZB6awBdpUKD9jf1b0SHzUv\n" +
            "KBds0pjBqAlkd25HN7rOrFleaJ1/ctaJxQZBKT5ZPt0m9STJEadao0xAH0ahmbWn\n" +
            "OlFuhjuefXKnEgV4We0+UXgVCwOPjdAvBbI+e0ocS3MFEvzG6uBQE3xDk3SzynTn\n" +
            "jh8BCNAw1FtxNrQHusEwMFxIt4I7mKZ9YIqioymCzLq9gwQbooMDQaHWBfEbwrbw\n" +
            "qHyGO0aoSCqI3Haadr8faqU9GY/rOPNk3sgrDQoo//fb4hVC1CLQJ13hef4Y53CI\n" +
            "rU7m2Ys6xt0nUW7/vGT1M0NPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNV\n" +
            "HRMBAf8EBTADAQH/MB0GA1UdDgQWBBR5tFnme7bl5AFzgAiIyBpY9umbbjANBgkq\n" +
            "hkiG9w0BAQsFAAOCAgEAVR9YqbyyqFDQDLHYGmkgJykIrGF1XIpu+ILlaS/V9lZL\n" +
            "ubhzEFnTIZd+50xx+7LSYK05qAvqFyFWhfFQDlnrzuBZ6brJFe+GnY+EgPbk6ZGQ\n" +
            "3BebYhtF8GaV0nxvwuo77x/Py9auJ/GpsMiu/X1+mvoiBOv/2X/qkSsisRcOj/KK\n" +
            "NFtY2PwByVS5uCbMiogziUwthDyC3+6WVwW6LLv3xLfHTjuCvjHIInNzktHCgKQ5\n" +
            "ORAzI4JMPJ+GslWYHb4phowim57iaztXOoJwTdwJx4nLCgdNbOhdjsnvzqvHu7Ur\n" +
            "TkXWStAmzOVyyghqpZXjFaH3pO3JLF+l+/+sKAIuvtd7u+Nxe5AW0wdeRlN8NwdC\n" +
            "jNPElpzVmbUq4JUagEiuTDkHzsxHpFKVK7q4+63SM1N95R1NbdWhscdCb+ZAJzVc\n" +
            "oyi3B43njTOQ5yOf+1CceWxG1bQVs5ZufpsMljq4Ui0/1lvh+wjChP4kqKOJ2qxq\n" +
            "4RgqsahDYVvTH9w7jXbyLeiNdd8XM2w9U/t7y0Ff/9yi0GE44Za4rF2LN9d11TPA\n" +
            "mRGunUHBcnWEvgJBQl9nJEiU0Zsnvgc/ubhPgXRR4Xq37Z0j4r7g1SgEEzwxA57d\n" +
            "emyPxgcYxn/eR44/KJ4EBs+lVDR3veyJm+kXQ99b21/+jh5Xos1AnX5iItreGCc=\n" +
            "-----END CERTIFICATE-----";

    /**
     * HTTP Client
     */
    private OkHttpClient client;
    private final Moshi moshi = new Moshi.Builder()
            .add(new LectureVariantsJsonAdapter())
            .add(new OfficesChecksumsJsonAdapter())
            .build();
    final JsonAdapter<Office> officeJsonAdapter = moshi.adapter(Office.class);
    final JsonAdapter<OfficesChecksums> officesChecksumsJsonAdapter = moshi.adapter(OfficesChecksums.class);

    /**
     * Singleton
     */

    EpitreApi(Context c) {
        super();

        // Get a handle on the preference manager
        // TODO: stop depending on the context / preferences and instead user a wrapper / anti-corruption class so that
        // this class becomes easier to test.
        if (c != null) {
            preference = PreferenceManager.getDefaultSharedPreferences(c);
        }

        // Build the HTTP client
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // Inject Let's Encrypt root certificates in the trust store. This is needed for
        // obsolete phones where even the trust store did not receive updates. Ex: 6.0.1
        // see https://stackoverflow.com/questions/64844311/certpathvalidatorexception-connecting-to-a-lets-encrypt-host-on-android-m-or-ea
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate isgCertificate = cf.generateCertificate(new ByteArrayInputStream(ISGRootX1PEM.getBytes("UTF-8")));

            HandshakeCertificates certificates = new HandshakeCertificates.Builder()
                    .addTrustedCertificate((X509Certificate) isgCertificate)
                    .addPlatformTrustedCertificates()
                    .build();

            builder.sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager());
        } catch (Exception e) {
            Log.w(TAG, "Failed to configure custom CA. Ignoring: " + e);
        }

        // Configure client timeouts
        builder.connectTimeout(30, TimeUnit.SECONDS) // Was 60 seconds
               .writeTimeout  (60, TimeUnit.SECONDS) // Was 10 minutes
               .readTimeout   (60, TimeUnit.SECONDS)
               .retryOnConnectionFailure(true);

        // Instanciate the HTTP client
        this.client = builder.build();
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

    private String extractResponseEtag(Response response) {
        // In the application, we use the Etag to validate cache entry freshness.
        // Etags are defined by RFC 7232 section 2.3 and look like W/"ACTUAL-ETAG"
        // where the W/ prefix is optional. An Etag without a pair of double quotes
        // is assumed to be a raw value and returned as-is.
        String raw_etag = response.header("etag");
        if (raw_etag == null) {
            return "";
        }

        int quote_start = raw_etag.indexOf("\"");
        int quote_end = raw_etag.lastIndexOf("\"");

        if (quote_start == -1 || quote_end == -1) {
            return raw_etag;
        }

        return raw_etag.substring(quote_start+1, quote_end);
    }

    /**
     * Public API
     */

    public OfficesChecksums getOfficesChecksums(AelfDate since, int days, int apiVersion) throws IOException {
        // Build URL
        String path = "/%d/office/checksums/%s/%sd?region=%s";
        String region = preference.getString(SettingsActivity.KEY_PREF_REGION, "romain");
        path = String.format(Locale.US, path, apiVersion, since.toIsoString(), days, region);

        BufferedSource source = null;
        try {
            // Issue request
            Response response = InternalGet(path);

            // Grab response
            source = Objects.requireNonNull(response.body()).source();

            // De-Serialize
            OfficesChecksums officesChecksums = officesChecksumsJsonAdapter.fromJson(source);

            // Return
            return officesChecksums;
        } catch (IOException e) {
            Log.w(TAG, "Failed to load office checksums from network: " + e);
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if(source != null) {
                source.close();
            }
        }
    }

    public OfficeResponse getOffice(String officeName, String date, int apiVersion) throws IOException {
        // Build URL
        String path = "/%d/office/%s/%s.json?region=%s";
        String region = preference.getString(SettingsActivity.KEY_PREF_REGION, "romain");
        path = String.format(Locale.US, path, apiVersion, officeName, date, region);

        BufferedSource source = null;
        try {
            // Issue request
            Response response = InternalGet(path);

            // Grab response
            source = Objects.requireNonNull(response.body()).source();
            Office office = officeJsonAdapter.fromJson(source);

            // Grab checksum, as an opaque blob
            String checksum = extractResponseEtag(response);

            // Return
            return new OfficeResponse(office, checksum);
        } catch (IOException e) {
            Log.w(TAG, "Failed to load lectures from network: " + e);
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if(source != null) {
                source.close();
            }
        }
    }
}
