package co.epitre.aelf_lectures.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

/**
 * Public data controller --> load either from cache, either from network
 */

public final class LecturesController {
    /**
     * "What to sync" constants
     */

    public enum WHAT {
		MESSE   (0, "lectures_messe",    "http://rss.aelf.org/%s/[redacted]"),
		LECTURES(1, "lectures_lectures", "http://rss.aelf.org/%s/[redacted]"),
		LAUDES  (2, "lectures_laudes",   "http://rss.aelf.org/%s/[redacted]"),
		TIERCE  (3, "lectures_tierce",   "http://rss.aelf.org/%s/[redacted]"),
		SEXTE   (4, "lectures_sexte",    "http://rss.aelf.org/%s/[redacted]"),
		NONE    (5, "lectures_none",     "http://rss.aelf.org/%s/[redacted]"),
		VEPRES  (6, "lectures_vepres",   "http://rss.aelf.org/%s/[redacted]"),
		COMPLIES(7, "lectures_complies", "http://rss.aelf.org/%s/[redacted]"),
		METAS   (8, "lectures_metas",    "http://rss.aelf.org/%s/[redacted]");

    	private String name = "";
    	private String url = "";
    	private int position = 0;

    	WHAT(int position, String name, String url) {
    		this.position = position;
    		this.name = name;
    		this.url = url;
    	}

    	public String getUrl(){
    		return url;
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
    private static volatile LecturesController instance = null;
    private AelfCacheHelper cache = null;
    private LecturesController(Context c) {
    	super();
    	cache = new AelfCacheHelper(c);
    }
    public final static LecturesController getInstance(Context c) {
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
     * API
     * @throws IOException to allow for auto retry. Aelf servers are not that stable...
     */

    public List<LectureItem> getLectures(WHAT what, GregorianCalendar when, boolean restrictToCache) throws IOException {
        List<LectureItem> lectures = null;

        // attempts load from cache
        try {
        	lectures = cache.load(what, when);
    	} catch (RuntimeException e) {
    		// gracefully recover when DB stream outdated/corrupted by refreshing
    		Log.e(TAG, "Loading lecture from cache crashed ! Recovery by refreshing...");
    		lectures = null;
    	}
        
        // on error or if cached value looks like an error (not yet in AELF
        // calendar for instance), force reload of live data.
        // Need this heuristic after a cache load as previous versions erroneously cached
        // these.
        if(lectures != null && !looksLikeError(lectures)) {
        	return lectures;
        }

    	// are we allowed to go any further ?
        if(restrictToCache) return null;

        // fallback to network load
        lectures = loadFromNetwork(what, when);
    	if(lectures == null) {
    		// big fail TODO: Log
    		return null;
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

        for(int i = 0; i < whatValues.length; i++) {
        	cache.truncateBefore(whatValues[i], when);
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
    	
    	return true;
    }

    private List<LectureItem> PostProcessLectures(List<LectureItem> lectures) {
    	List<LectureItem> cleaned = new ArrayList<LectureItem>();

    	String descriptionBuffer = "";
    	String titleBuffer = "";
    	boolean inPericope = false;

    	for(LectureItem lectureIn: lectures) {
    		boolean isEmpty = lectureIn.description.trim().equals("");
    		boolean isAntienne = lectureIn.longTitle.equals("Antienne");
    		
    		if(!inPericope) {
    			// transition in ?
    			if(lectureIn.shortTitle.equals("Pericope")) {
    				inPericope = true;
    				titleBuffer = lectureIn.longTitle;
    			}
    		} else {
    			// still in or transitioned out ?
    			inPericope = lectureIn.shortTitle.equals("Repons") || lectureIn.shortTitle.equals("Verset");
    		}

    		// if the content is empty, just ignore this chunk
    		if(isEmpty) continue;

    		if(!inPericope)
	    		titleBuffer = lectureIn.longTitle
	    				.replace("n\\est", "n'est");
    		
    		if(!isAntienne) {
    			// prepend title
    			descriptionBuffer = "<h3>" + titleBuffer + "</h3>" + descriptionBuffer;
    		} else {
    			// enter blockquote
    			descriptionBuffer += "<blockquote><b>Antienne&nbsp;:</b> ";
    		}

    		// add &nbsp; where needed
    		descriptionBuffer += lectureIn.description
    				// fix ugly typo in error message
    				.replace("n\\est", "n'est")
    				// ensure punctuation has required spaces
    				.replace(" :", "&nbsp;:")
    				.replace(" !", "&nbsp;!")
    				.replace(" ?", "&nbsp;?")
    				// fix suddenly smaller text in readings
    				.replace("size=\"2\"", "")
    				.replace("face=\"Tahoma\"", "")
    				// ensure quotes have required spaces
    				.replace(" &raquo;", "&nbsp;&raquo;")
    				.replace("&laquo; ", "&laquo;&nbsp;");

    		// if `longTitle` is "Antienne" --> done
    		if(isAntienne) {
    			descriptionBuffer += "</blockquote>";
    			continue; // FIXME: add "not last iteration condition"
    		}

    		// append to the output list
    		cleaned.add(new LectureItem(
    				titleBuffer,
    				descriptionBuffer,
    				lectureIn.category,
    				lectureIn.guid));

    		// reset buffer
    		descriptionBuffer = "";
    		titleBuffer = "";
    	}

    	return cleaned;
    }

    // real work internal var
    private static final SimpleDateFormat formater = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

    // Attempts to load from network
    // throws IOException to allow for auto retry. Aelf servers are not that stable...
    private List<LectureItem> loadFromNetwork(WHAT what, GregorianCalendar when) throws IOException {
    	InputStream in;
    	URL feedUrl;

    	List<LectureItem> lectures = new ArrayList<LectureItem>();

    	// Attempts to load the feed
    	try {
    		feedUrl = new URL(String.format(what.getUrl(), formater.format(when.getTime())));
    		in = feedUrl.openStream();
    	} catch (MalformedURLException e) {
    		throw new RuntimeException(e);
    		// FIXME: error recovery
    	}

    	// Attempts to parse the feed
    	try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            readFeed(parser, lectures);
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
            // FIXME: error recovery
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    		// FIXME: error recovery
    	} finally {
            try {
    			in.close();
    		} catch (IOException e) {
    			throw new RuntimeException(e);
    			// FIXME: error recovery
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
        String title = null;
        String description = null;
        String category = null;
        int guid = -1;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
                parser.require(XmlPullParser.START_TAG, null, name);
                title = readText(parser);
                parser.require(XmlPullParser.END_TAG, null, name);
            } else if (name.equals("description")) {
                parser.require(XmlPullParser.START_TAG, null, name);
                description = readText(parser);
                parser.require(XmlPullParser.END_TAG, null, name);
            } else if (name.equals("category")) {
            	parser.require(XmlPullParser.START_TAG, null, name);
                category = readText(parser);
                parser.require(XmlPullParser.END_TAG, null, name);
            } else if (name.equals("guid")) {
            	parser.require(XmlPullParser.START_TAG, null, name);
                guid = readInt(parser);
                parser.require(XmlPullParser.END_TAG, null, name);
            } else {
                skip(parser);
            }
        }
        return new LectureItem(title, description, category, guid);
    }

    // Extract text from the feed
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Extract int from the feed
    private int readInt(XmlPullParser parser) throws IOException, XmlPullParserException {
        int result = -1;
        if (parser.next() == XmlPullParser.TEXT) {
            result = Integer.parseInt(parser.getText());
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
