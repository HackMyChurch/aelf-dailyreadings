package co.epitre.aelf_lectures.bible;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import org.json.JSONArray;

import java.util.List;

import co.epitre.aelf_lectures.bible.data.bookmark.BibleBookmark;
import co.epitre.aelf_lectures.bible.data.bookmark.BibleBookmarkRepository;
import co.epitre.aelf_lectures.bible.data.bookmark.BibleBookmarkWebBridge;
import co.epitre.aelf_lectures.bible.data.bible.BibleBook;
import co.epitre.aelf_lectures.bible.data.bible.BibleBookChapter;
import co.epitre.aelf_lectures.components.ReadingFragment;


public class BibleChapterFragment extends ReadingFragment {
    private static final String TAG = "BibleChapterFragment";

    /**
     * The fragment arguments
     */
    public static final String ARG_HIGHLIGHT = "highlight";
    public static final String ARG_REFERENCE = "reference";
    public static final String ARG_BOOK_REF = "book_ref";
    public static final String ARG_CHAPTER = "chapter";

    @Override
    protected void onWebViewCreated(@NonNull WebView webView) {
        super.onWebViewCreated(webView);
        if (getContext() != null) {
            webView.addJavascriptInterface(
                    new BibleBookmarkWebBridge(getContext()),
                    BibleBookmarkWebBridge.INTERFACE_NAME
            );
        }
    }

    @Override
    protected void onWebViewDestroyed(@NonNull WebView webView) {
        super.onWebViewDestroyed(webView);
        webView.removeJavascriptInterface(BibleBookmarkWebBridge.INTERFACE_NAME);
    }

    @Override
    protected void loadText() {
        // Get arguments
        Bundle args = getArguments();

        // Build content
        StringBuilder htmlString = new StringBuilder();
        String body = "";
        String highlight = "";
        String reference = "";
        String book_ref = "";
        String chapter = "";
        if (args != null) {
            highlight = args.getString(ARG_HIGHLIGHT, "");
            reference = args.getString(ARG_REFERENCE, "");
            book_ref = args.getString(ARG_BOOK_REF, "");
            chapter = args.getString(ARG_CHAPTER, "");
        }
        if (highlight == null) highlight = "";
        if (reference == null) reference = "";
        if (book_ref == null) book_ref = "";
        if (chapter == null) chapter = "";

        if (!book_ref.isEmpty() && !chapter.isEmpty()) {
            BibleBook book = BibleBook.getBook(book_ref);
            try {
                int pos = book.getChapterPosition(chapter);
                BibleBookChapter chapterObj = book.getChapter(pos);
                if (chapterObj != null) {
                    body = chapterObj.getContent();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load chapter content for " + book_ref + " " + chapter, e);
            }
        }

        if (body == null) {
            body = "";
        }

        // Fetch bookmarks for this chapter
        JSONArray bookmarksJsonArray = new JSONArray();
        if (getContext() != null && !book_ref.isEmpty() && !chapter.isEmpty()) {
            List<BibleBookmark> bookmarks = BibleBookmarkRepository.getInstance(getContext())
                    .getBookmarksForChapter(book_ref, chapter);
            for (BibleBookmark bm : bookmarks) {
                bookmarksJsonArray.put(bm.toJsonObject());
            }
        }

        htmlString.append("<!DOCTYPE html><html><head>");
        htmlString.append("<link href=\"css/common.css\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<link href=\"");
        htmlString.append(getThemeCss());
        htmlString.append("\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<style>");
        htmlString.append(getAccessibilityCss());
        htmlString.append("</style>");
        htmlString.append("</head><body>");
        htmlString.append(body);
        htmlString.append("<script>");
        htmlString.append("var highlight='").append(highlight.replace("'", "")).append("';\n");
        htmlString.append("var reference='").append(reference.replace("'", "")).append("';\n");
        htmlString.append("var current_chapter='").append(chapter.replace("'", "")).append("';\n");
        htmlString.append("var current_book='").append(book_ref.replace("'", "")).append("';\n");
        htmlString.append("var current_bookmarks=").append(bookmarksJsonArray).append(";\n");
        htmlString.append("</script>");
        htmlString.append("<script src=\"js/mark.8.11.1.min.js\" charset=\"utf-8\"></script>\n");
        htmlString.append("<script src=\"js/chapter.js\" charset=\"utf-8\"></script>\n");
        htmlString.append("<script src=\"js/bookmarks.js\" charset=\"utf-8\"></script>\n");
        htmlString.append("</body></html>");

        // Build history URL
        StringBuilder UrlString = new StringBuilder();
        if (!book_ref.isEmpty()) {
            UrlString.append("aelf:bible/");
            UrlString.append(book_ref);
            UrlString.append("/");
            UrlString.append(chapter);
        }

        if (!reference.isEmpty()) {
            UrlString.append("#ref=");
            UrlString.append(reference);
        }

        // Load content
        this.setWebViewContent(htmlString.toString(), UrlString.toString());
    }
}
