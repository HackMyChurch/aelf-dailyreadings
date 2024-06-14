package co.epitre.aelf_lectures.bible;

import android.os.Bundle;

import co.epitre.aelf_lectures.components.ReadingFragment;


public class BibleChapterFragment extends ReadingFragment {
    private static final String TAG = "BibleChapterFragment";

    /**
     * The fragment arguments
     */
    public static final String ARG_TEXT_HTML = "chapter_text";
    public static final String ARG_HIGHLIGHT = "highlight";
    public static final String ARG_REFERENCE = "reference";
    public static final String ARG_BOOK_REF = "book_ref";
    public static final String ARG_CHAPTER = "chapter";

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
            body = args.getString(ARG_TEXT_HTML);
            highlight = args.getString(ARG_HIGHLIGHT, "");
            reference = args.getString(ARG_REFERENCE, "");
            book_ref = args.getString(ARG_BOOK_REF, "");
            chapter = args.getString(ARG_CHAPTER, "");
        }

        htmlString.append("<!DOCTYPE html><html><head>");
        htmlString.append("<link href=\"css/common.css\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<link href=\"");
        htmlString.append(getThemeCss());
        htmlString.append("\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<script src=\"js/mark.8.11.1.min.js\" charset=\"utf-8\"></script>\n");
        htmlString.append("</head><body>");
        htmlString.append(body);
        htmlString.append("<script>");
        htmlString.append("var highlight='"+highlight.replace("'", "")+"';\n");
        htmlString.append("var reference='"+reference.replace("'", "")+"';\n");
        htmlString.append("var current_chapter='"+chapter.replace("'", "")+"';\n");
        htmlString.append("</script>");
        htmlString.append("<script src=\"js/chapter.js\" charset=\"utf-8\"></script>\n");
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
