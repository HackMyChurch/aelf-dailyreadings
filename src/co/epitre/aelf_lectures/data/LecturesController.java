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
import android.provider.UserDictionary.Words;
import android.support.v4.text.TextUtilsCompat;
import android.transition.Slide;
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
	
	private String sanitizeTitleCapitalization(String input) {
		// sanitize capitalization
		if(input.length() != 0) {
			// *keep* capitals only when first letter of a word
			// set capital on first *letter*
			char[] chars = input.toCharArray();
			boolean isFirstChar = true;
			boolean handledCapital = false;
			for (int i = 0; i < chars.length; i++) {
				if (isFirstChar && Character.isLetter(chars[i])) {
					// First letter of sentence: always capitalize
					if(!handledCapital) {
						chars[i] = Character.toUpperCase(chars[i]);
						handledCapital = true;
					}
					isFirstChar = false;
				} else if (!handledCapital && Character.isDigit(chars[i])) {
					// If first alnum char is number, cancel 1st letter capitalization
					handledCapital = true;
				} else if (!isFirstChar && Character.isLetter(chars[i])) {
					// We are inside a word: never keep capitals
					chars[i] = Character.toLowerCase(chars[i]);
				} else if (!Character.isLetterOrDigit(chars[i])) {
					isFirstChar = true;
				}
			}
			input = String.valueOf(chars);

			// force lower case on determinant and preposition
			// except if 1st word
			String[] words = input.split("\\s+");
			input = "";
			boolean isFirstWord = true;
			for (int i = 0; i < words.length; i++) {
				if(words[i].startsWith("L'Évangile")) {
					// keep capitals
				} else if(!isFirstWord && (
					words[i].startsWith("D'") ||
					words[i].equals("De") ||
					words[i].equals("Des") ||
					words[i].startsWith("L'") ||
					words[i].equals("Le") ||
					words[i].equals("La") ||
					words[i].equals("Les") ||
					words[i].equals("Sur") ||
					words[i].equals("Sur") ||
					words[i].equals("En") ||
					words[i].startsWith("C'") ||
					words[i].equals("Ce") ||
					words[i].equals("Ces") ||
					words[i].equals("Celui") ||
					words[i].equals("Ça") ||
					words[i].equals("Sa") ||
					words[i].equals("Son") ||
					words[i].equals("Ses") ||
					words[i].equals("Dans") ||
					words[i].equals("Pour") ||
					words[i].equals("Contre") ||
					words[i].equals("Avec")
				)) {
					words[i] = words[i].toLowerCase();
				}
				input += " "+words[i];

				// Toggle once 1st word has been handled
				if(Character.isLetterOrDigit(words[i].charAt(0))) {
					isFirstWord = false;
				}
			}
		}
		return input;
	}

	private String commonTextSanitizer(String input) {
		input = input
				// drop F** MS Word Meta
				.replaceAll("(?s)<!--.*-->", "")
				// remove inline paragraph styling
				.replaceAll("<p.*?>", "<p>")
				// fun with line feed at the beginning of §
				.replaceAll("<p><br\\s*/>", "<p>")
				// fix ugly typo in error message
				.replace("n\\est", "n'est")
				// R/ formating
				.replace("</p></font></p>", "</font></p>\n")
				.replace("R/ <p>", "<strong>R/&nbsp;</strong>")
				// spacing fixes
				.replaceAll("\\s*-\\s*", "-")
    			.replaceAll(":\\s+(\\s+)", "")
    			.replaceAll("\\s*\\(", " (") // FIXME: move this, ensure space before '('
				// ensure punctuation has required spaces
				.replaceAll("\\s*([:?!])\\s*", "\u00A0$1 ")
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
				// Evangile fixes
				.replace("<br /><blockquote>", "<blockquote>")
				.replaceAll("<b>Acclamation\\s*:\\s*</b>", "")
				// clean references
				.replaceAll("<small><i>\\s*\\((cf.)?\\s*", "<small><i>— ")
				.replaceAll("\\s*\\)</i></small>", "</i></small>")
				// grrrr
				.replaceAll("<strong><font\\s*color=\"#[a-zA-Z0-9]*\"><br\\s*/></font></strong>", "")
				// ensure quotes have required spaces
				.replaceAll("\\s*(»|&raquo;)", "\u00A0»")
				.replaceAll("(«|&laquo;)\\s*", "«\u00A0");

		// Recently, lectures started to use 1§ / line && 1 empty § between parts. This result is UGLY. Fiw this
		if(input.contains("<p>&nbsp;</p>")) {
			input = input
					.replace("<p><p>", "<p>")
					.replace("</p></p>", "</p>")
					.replace("<p>&nbsp;</p>", " ")
					.replace("</p><p>", "<br/>");
		}
		return input;
	}

    private List<LectureItem> PostProcessLectures(List<LectureItem> lectures) {
    	List<LectureItem> cleaned = new ArrayList<LectureItem>();

    	postProcessState previousState = postProcessState.Empty;
    	String bufferCategory = "";
    	String bufferTitle = "";
    	String pagerTitle = "";
    	String lectureTitle = "";
    	String lectureReference = "";
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
    		currentTitle = currentDescription = commonTextSanitizer(currentTitle)
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
    					pagerTitle = words[0];
    					if(words.length > 1) {
    						lectureTitle = words[1];
    					} else {
    						lectureTitle = words[0];
    					}
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
    		
    		// Sanitize titles capitalization
    		pagerTitle = sanitizeTitleCapitalization(pagerTitle);
    		lectureTitle = sanitizeTitleCapitalization(lectureTitle);
    		
    		// FIXME: compat
    		bufferTitle = pagerTitle + " : " + lectureTitle;
    		
    		currentDescription = commonTextSanitizer(lectureIn.description);
    		
    		// prepare reference, if any
    		if(lectureReference != "") {
    			lectureReference = "<small><i>— "+lectureReference+"</i></small>";
    		}
    		
    		// accumulate into buffer, depending on current state
    		switch(currentState) {
			case Empty:
				// TODO: exception ?
				break;
			case Pericope:
				if(lectureIn.shortTitle.equalsIgnoreCase("pericope")) {
					bufferDescription = "<h3>" + lectureTitle + lectureReference + "</h3><div style=\"clear: both;\"></div>" + currentDescription;
					bufferCategory = lectureIn.category;
				} else {
					bufferDescription += "<blockquote>" + currentDescription + "</blockquote>";
					bufferDescription = bufferDescription
							.replace("</blockquote><blockquote>", "")
							.replaceAll("(<br\\s*/>){2,}", "<br/><br/>");
				}
				break;
			case Psaume:
				if(lectureIn.longTitle.equalsIgnoreCase("antienne")) {
					currentDescription = currentDescription.replaceAll("^\\s*<p>", "");
					bufferDescription = "<blockquote><p><b>Antienne&nbsp;:</b> "+currentDescription+"</blockquote>";
				} else { // Psaume|Cantique
					bufferDescription = "<h3>" + lectureTitle + lectureReference + "</h3><div style=\"clear: both;\"></div>" + bufferDescription + currentDescription;
					bufferCategory = lectureIn.category;
					// force commit && buffer flush --> avoid to merge consecutives psaumes
					currentState = postProcessState.NeedFlush;
				}
				break;
			case Regular:
				bufferCategory = lectureIn.category;
				//bufferTitle = currentTitle;
				bufferDescription = "<h3>" + lectureTitle + lectureReference + "</h3><div style=\"clear: both;\"></div>" + currentDescription;
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
