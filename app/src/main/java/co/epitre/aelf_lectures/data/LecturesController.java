package co.epitre.aelf_lectures.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.extra.PiwikApplication;
import org.piwik.sdk.extra.TrackHelper;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Xml;

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

    private LecturesController(Context c) {
        super();

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
            Raven.capture(e);
            throw e;
        } catch (Exception e) {
            errorName = "error."+e.getClass().getName();
            Raven.capture(e);
            throw e;
        } finally {
            // Push event
            float deltaTime = (System.nanoTime() - startTime) / 1000;
            long dayDelta = when.dayBetween(new GregorianCalendar());

            TrackHelper.track().event("Office", "download."+errorName).name(what.urlName()+"."+dayDelta).value(deltaTime).with(tracker);
        }

        lectures = PostProcessLectures(lectures);

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

    private enum postProcessState {
        Empty,    // Initial. Do not commit
        Regular,  // post-process && commit
        Psaume,   // Merge Antienne + Psaume/Cantique/...
        Pericope, // aka "Parole de Dieu"
        Repons,   // usually comes right after the pericope, we'll want to merge them
        Verse,    // verse needs to be inserted in preceding psaume, if any
        Antienne, // This comes before the psaume, this will need to be buffered
        Oraison,
        Benediction,
        NeedFlush, // Force flush when item is known to be last of sequence
    }

    private int count_match(String input, String search) {
        int matches = 0;
        Pattern pattern = Pattern.compile(search);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) matches++;

        return matches;
    }

    private List<LectureItem> PostProcessLectures(List<LectureItem> lectures) {
        List<LectureItem> cleaned = new ArrayList<>();

        postProcessState previousState = postProcessState.Empty;
        String bufferKey = null;
        String bufferReference = "";
        String bufferTitle = "";
        String pagerTitle;
        String lectureTitle;
        String lectureReference;
        String bufferDescription = "";

        for(LectureItem lectureIn: lectures) {
            postProcessState currentState;
            String currentTitle;
            String currentDescription;
            String currentKey = "";

            // compute new state
            if(	lectureIn.shortTitle.equalsIgnoreCase("pericope") ||
                lectureIn.shortTitle.equalsIgnoreCase("lecture") ||
                lectureIn.shortTitle.equalsIgnoreCase("lecture patristique") ||
                lectureIn.shortTitle.equalsIgnoreCase("parole de dieu")) {
                currentState = postProcessState.Pericope;
            } else if ( lectureIn.shortTitle.equalsIgnoreCase("repons") ||
                        lectureIn.shortTitle.equalsIgnoreCase("répons")) {
                currentState = postProcessState.Repons;
            } else if ( lectureIn.shortTitle.equalsIgnoreCase("verset")) {
                currentState = postProcessState.Verse;
            } else if ( lectureIn.shortTitle.equalsIgnoreCase("antienne")) {
                currentState = postProcessState.Antienne;
            } else if ( lectureIn.shortTitle.equalsIgnoreCase("oraison")) {
                currentState = postProcessState.Oraison;
            } else if ( lectureIn.shortTitle.equalsIgnoreCase("bénédiction")) {
                currentState = postProcessState.Benediction;
            } else if ( lectureIn.longTitle.toLowerCase().startsWith("psaume") ||
                        lectureIn.longTitle.toLowerCase().startsWith("cantique")) {
                currentState = postProcessState.Psaume;
            } else {
                currentState = postProcessState.Regular;
            }

            // Do we need to flush ?
            boolean needFlush = (previousState != postProcessState.Empty); // Unless first slide, flush by default

            if (previousState == postProcessState.Regular) {
                // Regular text --> flush
                needFlush = true;
            } else if (previousState == postProcessState.NeedFlush) {
                // Explicit request (will be removed)
                needFlush = true;
            } else if (previousState == postProcessState.Antienne && currentState == postProcessState.Psaume) {
                // Antienne can be merged in psaume --> explicit false
                needFlush = false;
            } else if (previousState == postProcessState.Pericope && currentState == postProcessState.Repons) {
                // Repons can be merged in Pericope --> explicit false
                needFlush = false;
            } else if (previousState == postProcessState.Pericope && currentState == postProcessState.Verse) {
                // Verset can be merged in Pericope if after --> explicit false
                needFlush = false;
            } else if (previousState == postProcessState.Repons && currentState == postProcessState.Verse) {
                // Verset can be merged in Repons if after (cf pericope --> repons --> verset) --> explicit false
                needFlush = false;
            } else if (previousState == postProcessState.Psaume && currentState == postProcessState.Verse) {
                // Verse can be merged in psaume --> explicit false
                needFlush = false;
            } else if (previousState == postProcessState.Oraison && currentState == postProcessState.Benediction) {
                // Benediction can be merged in Oraison
                needFlush = false;
            }

            if(	needFlush) {
                cleaned.add(new LectureItem(
                        bufferKey,
                        bufferTitle,
                        bufferDescription,
                        bufferReference));
                bufferTitle = bufferDescription = "";
                bufferKey = null;
            }

            /*
             * Titres
             * ======
             *
             * 1/ FILTRE péricope, ...
             *
             * 2/ split sur ':'
             *  - SI une seule partie: short = long, FIN
             *  - SI commence par "'«, short = [0], long = [1], FIN
             *  - SI [1] commence par Cantique|... short = [1].1ermot, long = [1], [0],[1] = [0].split(' ')
             *    SINON short = [0], long = [1]
             *  - SI [1] commence par chiffre ET (1 seul mot OU mot suivant commence par chiffre), short += 1er mot, long = [1]
             *
             */
            currentTitle = lectureIn.longTitle;
            lectureReference = "";

            // trim HTML
            currentTitle = android.text.Html.fromHtml(currentTitle).toString();

            // Extract reference
            String parts[] = currentTitle.split("\\(", 2);
            currentTitle = parts[0].trim();
            if(parts.length > 1) {
                int p = parts[1].lastIndexOf(")");
                lectureReference = parts[1].substring(0, p).trim();
            }

            // filter title && content
            currentTitle = currentTitle
                    // title specific fixes
                    .replaceAll("CANTIQUE", "Cantique")
                    .replaceFirst("(?i)pericope", "Parole de Dieu")
                    .replaceAll("(Hymne|Psaume)\\s+(?!:)", "$1 : ");

            // compute long and short titles
            String[] titleChunks = currentTitle.trim().split("\\s*:\\s*", 2);
            if(titleChunks.length == 1 || titleChunks[1].length() == 0) {
                pagerTitle = lectureTitle = titleChunks[0];

                // if the short title is loooooong, heuristic: keep only the first, this a comment for Lectures Office
                if(pagerTitle.length() > 30) {
                    pagerTitle = pagerTitle.split("\\s+")[0];
                }
            } else {
                char fc = titleChunks[1].charAt(0);
                if(fc == '"' || fc == '\'' || fc == '«') {
                    pagerTitle = titleChunks[0];
                    lectureTitle = titleChunks[1];
                } else {
                    String[] words = titleChunks[1].split("\\s+", 2);
                    if(
                        words[0].equalsIgnoreCase("cantique") ||
                        words[0].equalsIgnoreCase("hymne") ||
                        words[0].equalsIgnoreCase("psaume")
                    ){
                        pagerTitle = titleChunks[1];
                        lectureTitle = titleChunks[1];
                    } else {
                        pagerTitle = titleChunks[0];
                        lectureTitle = titleChunks[1];
                    }

                    // FIXME: hard-coded until real ref parser
                    if(currentState == postProcessState.Psaume && Character.isDigit(lectureTitle.charAt(0))) {
                        lectureTitle = pagerTitle + " " + lectureTitle;
                        pagerTitle += " "+words[0];
                    }
                }

                // finally, make sure we have more than just a reference
                if(lectureTitle.charAt(0) == '(') {
                    lectureTitle = pagerTitle + " " + lectureTitle;
                }
            }

            currentDescription = lectureIn.description;

            // Insert note if the lecture is not available (AELF bug...)
            if(lectureIn.description.trim().equals("")) {
                String name = null;

                // What kind of reading is that ?
                if(currentState == postProcessState.Pericope) {
                    name = "cette lecture";
                } else if(currentState == postProcessState.Psaume) {
                    name = "ce psaume";
                } else {
                    currentState = postProcessState.Empty;
                }

                // If it is a major reading, recover
                if (currentState != postProcessState.Empty) {
                    if (!lectureReference.equals("")) {
                        String link = "http://epitre.co/" + lectureReference.replace(" ", "");
                        currentDescription = "<p>Oups... Il semble que nous n\'ayons pas réussis à afficher "+name+"... Peut être aurez vous plus de chance sur <a href=\"" + link + "\">epitre.co</a> (Traduction liturgiqe, AELF)&nbsp;? Nous ferons mieux la prochaine fois, promis&nbsp;!</p>";
                    } else {
                        currentDescription = "<p>Oups... Il semble que nous n\'ayons pas réussis à afficher "+name+"... Nous ferons mieux la prochaine fois, promis&nbsp;!</p>";
                    }
                }
            }


            // Prepare reference, if any
            if (lectureIn.reference != null && !lectureIn.reference.isEmpty()) {
                lectureReference = lectureIn.reference;
            }
            if(!lectureReference.equals("")) {
                lectureReference = "<small><i>— "+lectureReference+"</i></small>";
            }

            // Clean key
            if (lectureIn.key != null && !lectureIn.key.equals("")) {
                currentKey = lectureIn.key;
            }

            // accumulate into buffer, depending on current state
            switch(currentState) {
            case Empty:
                break;
            case Pericope:
                bufferDescription = "<h3>" + lectureTitle + lectureReference + "</h3><div style=\"clear: both;\"></div>" + currentDescription;
                bufferReference = lectureIn.reference;
                bufferTitle = pagerTitle + " : " + lectureTitle;
                bufferKey = currentKey;
                break;
            case Repons:
            case Verse:
                bufferDescription += "<blockquote>" + currentDescription + "</blockquote>";
                bufferDescription = bufferDescription
                        .replace("</blockquote><blockquote>", "")
                        .replaceAll("(<br\\s*/>){2,}", "<br/><br/>");
                if(bufferTitle.equals("")) {
                    bufferTitle = pagerTitle + " : " + lectureTitle;
                }
                break;
            case Antienne:
                bufferDescription = "<blockquote><p><b>Antienne&nbsp;:</b> " + currentDescription + "</blockquote>";
                if(bufferTitle.equals("")) {
                    bufferTitle = pagerTitle + " : " + lectureTitle;
                }
                break;
            case Psaume:
                bufferDescription = "<h3>" + lectureTitle + lectureReference + "</h3><div style=\"clear: both;\"></div>" + bufferDescription + currentDescription;
                bufferReference = lectureIn.reference;
                bufferTitle = pagerTitle + " : " + lectureTitle;
                bufferKey = currentKey;
                break;
            case Oraison:
                bufferDescription = "<h3>Oraison</h3><div style=\"clear: both;\"></div>" + currentDescription;
                bufferReference = lectureIn.reference;
                bufferTitle = "Oraison";
                bufferKey = currentKey;
                break;
            case Benediction:
                bufferDescription = bufferDescription.replace("<h3>Oraison</h3>", "<h3>Oraison et bénédiction</h3>");
                bufferDescription += currentDescription;
                bufferReference = lectureIn.reference;
                bufferTitle = "Oraison et bénédiction";
                bufferKey = currentKey;
                break;
            case Regular:
                bufferReference = lectureIn.reference;
                bufferTitle = pagerTitle + " : " + lectureTitle;
                bufferDescription = "<h3>" + lectureTitle + lectureReference + "</h3><div style=\"clear: both;\"></div>" + currentDescription;
                bufferKey = currentKey;
                break;
            default:
                break;
            }

            // save state
            previousState = currentState;
        }

        // Not empty ? --> do last commit
        if(	previousState != postProcessState.Empty) {
            cleaned.add(new LectureItem(
                    bufferKey,
                    bufferTitle,
                    bufferDescription,
                    bufferReference));
        }

        return cleaned;
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
    // throws IOException to allow for auto retry. Aelf servers are not that stable...
    private List<LectureItem> loadFromNetwork(WHAT what, AelfDate when) throws IOException, DownloadException {
        InputStream in = null;
        URL feedUrl;

        List<LectureItem> lectures = new ArrayList<>();

        // Build feed URL
        int version = preference.getInt("version", -1);
        boolean pref_nocache = preference.getBoolean("pref_participate_nocache", false);

        try {
            String url = String.format(Locale.US, getUrl(what)+"?version=%d", formater.format(when.getTime()), version);
            Log.d(TAG, "Getting "+url);
            feedUrl = new URL(url);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Failed to parse URL", e);
            Raven.capture(e);
            throw new DownloadException("url");
        }

        // Attempts to load and parse the feed
        HttpURLConnection urlConnection = (HttpURLConnection) feedUrl.openConnection();
        urlConnection.setConnectTimeout(60*1000); // 60 seconds
        urlConnection.setReadTimeout(600*1000);   // 10 minutes

        if (pref_nocache) {
            urlConnection.setRequestProperty("x-aelf-nocache", "1");
        }

        try {
            in = urlConnection.getInputStream();
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            readFeed(parser, lectures);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Failed to parse API result", e);
            Raven.capture(e);
            throw new DownloadException("parse");
        } finally {
            try {
                urlConnection.disconnect();
                if(in!=null) in.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close API connection", e);
                Raven.capture(e);
            }
        }

        return lectures;
    }

    // main parser
    private void readFeed(XmlPullParser parser, List<LectureItem> lectures) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "rss");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("channel")) {
                readChannel(parser, lectures);
            } else {
                skip(parser);
            }
        }
    }

    private void readChannel (XmlPullParser parser, List<LectureItem> lectures) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "channel");

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("item")) {
                lectures.add(readEntry(parser));
            } else {
                skip(parser);
            }
        }
    }

    // item parser
    private LectureItem readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "item");
        String key = null;
        String title = null;
        String description = null;
        String reference = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "title":
                    parser.require(XmlPullParser.START_TAG, null, name);
                    title = readText(parser);
                    parser.require(XmlPullParser.END_TAG, null, name);
                    break;
                case "description":
                    parser.require(XmlPullParser.START_TAG, null, name);
                    description = readText(parser);
                    parser.require(XmlPullParser.END_TAG, null, name);
                    break;
                case "key":
                    parser.require(XmlPullParser.START_TAG, null, name);
                    key = readText(parser).replace("\n", "").trim();
                    parser.require(XmlPullParser.END_TAG, null, name);
                    break;
                case "reference":
                    parser.require(XmlPullParser.START_TAG, null, name);
                    reference = readText(parser);
                    parser.require(XmlPullParser.END_TAG, null, name);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return new LectureItem(key, title, description, reference);
    }

    // Extract text from the feed
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            result = result.replace("\n", "").trim();
            parser.nextTag();
        }
        return result;
    }

    // skip tags I do not need
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                depth--;
                break;
            case XmlPullParser.START_TAG:
                depth++;
                break;
            }
        }
     }
}
