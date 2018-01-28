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

    //
    // Entry-Point
    //

    public static List<LectureItem> parse(InputStream in) throws IOException, XmlPullParserException {
        return readFeed(in);
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
