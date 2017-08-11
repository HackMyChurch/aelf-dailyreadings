package co.epitre.aelf_lectures.data;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jean-tiare on 22/05/17.
 */

public final class AelfRssParser {

    /**
     * Post-processor state-machine
     */
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

    //
    // Entry-Point
    //

    public static List<LectureItem> parse(InputStream in) throws IOException, XmlPullParserException {
        List<LectureItem> lectures = readFeed(in);
        return PostProcessLectures(lectures);
    }

    //
    // High level Lecture post-processing
    //

    private static String capitalize(final String line) {
        if (line == null) {
            return line;
        }
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

    private static List<LectureItem> PostProcessLectures(List<LectureItem> lectures) {
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
                } else if (titleChunks[1].equals(lectureIn.reference)) {
                    pagerTitle = titleChunks[0];
                    lectureTitle = titleChunks[0];
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
                    bufferDescription = "<h3>" + capitalize(lectureTitle) + lectureReference + "</h3><div style=\"clear: both;\"></div>" + currentDescription;
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
                    bufferDescription = "<h3>" + capitalize(lectureTitle) + lectureReference + "</h3><div style=\"clear: both;\"></div>" + bufferDescription + currentDescription;
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
                    bufferDescription = "<h3>" + capitalize(lectureTitle) + lectureReference + "</h3><div style=\"clear: both;\"></div>" + currentDescription;
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

    //
    // Low level RSS Parser
    //

    private static List<LectureItem> readFeed(InputStream in) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(in, null);
        parser.nextTag();

        List<LectureItem> lectures = new ArrayList<>();
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

        return lectures;
    }

    private static void readChannel (XmlPullParser parser, List<LectureItem> lectures) throws XmlPullParserException, IOException {
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
    private static LectureItem readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
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
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            result = result.replace("\n", "").trim();
            parser.nextTag();
        }
        return result;
    }

    // skip tags I do not need
    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
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
