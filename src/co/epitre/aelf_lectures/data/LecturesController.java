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
	private static final String API_ENDPOINT = Credentials.API_ENDPOINT;

    public enum WHAT {
		MESSE   (0, "lectures_messe",    API_ENDPOINT+"/%s/"+Credentials.API_KEY_MESSE),
		LECTURES(1, "lectures_lectures", API_ENDPOINT+"/%s/"+Credentials.API_KEY_LECTURES),
		LAUDES  (2, "lectures_laudes",   API_ENDPOINT+"/%s/"+Credentials.API_KEY_LAUDES),
		TIERCE  (3, "lectures_tierce",   API_ENDPOINT+"/%s/"+Credentials.API_KEY_TIERCE),
		SEXTE   (4, "lectures_sexte",    API_ENDPOINT+"/%s/"+Credentials.API_KEY_SEXTE),
		NONE    (5, "lectures_none",     API_ENDPOINT+"/%s/"+Credentials.API_KEY_NONE),
		VEPRES  (6, "lectures_vepres",   API_ENDPOINT+"/%s/"+Credentials.API_KEY_VEPRES),
		COMPLIES(7, "lectures_complies", API_ENDPOINT+"/%s/"+Credentials.API_KEY_COMPLIES),
		METAS   (8, "lectures_metas",    API_ENDPOINT+"/%s/"+Credentials.API_KEY_METAS);

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

    public List<LectureItem> getLecturesFromCache(WHAT what, GregorianCalendar when) throws IOException {
    	List<LectureItem> lectures = null;
    	
    	try {
        	lectures = cache.load(what, when);
    	} catch (RuntimeException e) {
    		// gracefully recover when DB stream outdated/corrupted by refreshing
    		Log.e(TAG, "Loading lecture from cache crashed ! Recovery by refreshing...");
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
    
    public List<LectureItem> getLecturesFromNetwork(WHAT what, GregorianCalendar when) throws IOException {
    	List<LectureItem> lectures = null;
    	
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

	private enum postProcessState {
		Empty,    // Initial. Do not commit
		Regular,  // post-process && commit
		Psaume,   // Merge Antienne + Psaume/Cantique/...
		Pericope, // Merge Pericope + Repos + Verset
		NeedFlush, // Force flush when item is known to be last of sequence
	}

    private List<LectureItem> PostProcessLectures(List<LectureItem> lectures) {
    	List<LectureItem> cleaned = new ArrayList<LectureItem>();

    	postProcessState previousState = postProcessState.Empty;
    	String bufferCategory = "";
    	String bufferTitle = ""; 
    	String bufferDescription = "";

    	for(LectureItem lectureIn: lectures) {
    		postProcessState currentState = postProcessState.Empty;
    		String currentTitle = "";
    		String currentDescription = "";
    		
    		// ignore buggy/empty chunks
    		if(lectureIn.description.trim().equals("")) continue;
    		
    		// compute new state
    		if(	lectureIn.shortTitle.equalsIgnoreCase("pericope") ||
    			lectureIn.shortTitle.equalsIgnoreCase("repons") || 
    			lectureIn.shortTitle.equalsIgnoreCase("verset")) {
    			currentState = postProcessState.Pericope;
    		} else if(	lectureIn.longTitle.equalsIgnoreCase("antienne") || 
    					lectureIn.longTitle.toLowerCase().startsWith("psaume") ||
    					lectureIn.longTitle.toLowerCase().startsWith("cantique")) {
    			currentState = postProcessState.Psaume;
    		} else {
    			currentState = postProcessState.Regular;
    		}
    		
    		// state changed or Regular or NeedFlush ? Commit previous.
    		if(	previousState != postProcessState.Empty && (
    			previousState != currentState || 
    			previousState == postProcessState.NeedFlush ||
    			previousState == postProcessState.Regular)) {
        		cleaned.add(new LectureItem(
        				bufferTitle,
        				bufferDescription,
        				bufferCategory));
        		bufferCategory = bufferTitle = bufferDescription = "";
    		}
    		
    		// filter title && content
    		currentTitle = lectureIn.longTitle
    				// WTF fixes
    				.replace("n\\est", "n'est")
    				.replaceAll(":\\s+(\\s+)", "")
    				.replaceAll("(Hymne|Psaume)\\s+(?!:)", "$1 : ");
    		
    		currentDescription = lectureIn.description
    				// fix ugly typo in error message
    				.replace("n\\est", "n'est")
    				// ensure punctuation has required spaces
    				.replaceAll("\\s*([:?!])\\s*", "&nbsp;$1 ")
    				// non adjacent semicolon
    				.replaceAll("\\s+;\\s*", "&#x202f;; ")
    				// adjacent semicolon NOT from entities
    				.replaceAll("\\b(?<!&)(?<!&#)(\\w+);\\s*", "$1&#x202f;; ")
    				// fix suddenly smaller text in readings
    				.replace("size=\"2\"", "")
    				.replaceAll("face=\".*?\"", "")
    				.replaceAll("(font-size|font-family).*?(?=[;\"])", "")
    				// HTML entities bugs
    				.replace("&#156;", "œ")
    				// ensure quotes have required spaces
    				.replace(" &raquo;", "&nbsp;&raquo;")
    				.replace("&laquo; ", "&laquo;&nbsp;");
    		
    		// accumulate into buffer, depending on current state
    		switch(currentState) {
			case Empty:
				// TODO: exception ?
				break;
			case Pericope:
				if(lectureIn.shortTitle.equalsIgnoreCase("pericope")) {
					bufferTitle = currentTitle.replaceFirst("(?i)pericope", "Parole de Dieu");
					bufferDescription = "<h3>" + bufferTitle + "</h3>" + currentDescription;
					bufferCategory = lectureIn.category;
				} else {
					bufferDescription += "<blockquote>" + currentDescription + "</blockquote>";
				}
				break;
			case Psaume:
				if(lectureIn.longTitle.equalsIgnoreCase("antienne")) {
					bufferTitle = currentTitle;
					bufferDescription = "<blockquote><b>Antienne&nbsp;:</b> "+currentDescription+"</blockquote>";
				} else { // Psaume|Cantique
					bufferTitle = currentTitle;
					bufferDescription = "<h3>" + currentTitle + "</h3>" + bufferDescription + currentDescription;
					bufferCategory = lectureIn.category;
					// force commit && buffer flush --> avoid to merge consecutives psaumes
					currentState = postProcessState.NeedFlush;
				}
				break;
			case Regular:
				bufferCategory = lectureIn.category;
				bufferTitle = currentTitle;
				bufferDescription = "<h3>" + currentTitle + "</h3>" + currentDescription;
				break;
			default:
				// TODO: exception ?
				break;
    		}
    		
    		// save state
    		previousState = currentState;
    	}

    	// Not empty ? --> do last commit
		if(	previousState != postProcessState.Empty) {
    		cleaned.add(new LectureItem(
    				bufferTitle,
    				bufferDescription,
    				bufferCategory));
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
    		return null;
    	}

    	// Attempts to parse the feed
    	try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            readFeed(parser, lectures);
        } catch (XmlPullParserException e) {
        	return null;
    	} catch (IOException e) {
    		return null;
    	} finally {
            try {
    			in.close();
    		} catch (IOException e) {
    			return null;
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
            } else {
                skip(parser);
            }
        }
        return new LectureItem(title, description, category);
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
