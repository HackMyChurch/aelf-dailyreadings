package co.epitre.aelf_lectures.bible;

import co.epitre.aelf_lectures.components.ReadingFragment;


public class BibleChapterFragment extends ReadingFragment {
    private static final String TAG = "BibleChapterFragment";

    @Override
    protected void loadText() {
        // Build content
        StringBuilder htmlString = new StringBuilder();
        String body = getArguments().getString(ARG_TEXT_HTML);
        String highlight = getArguments().getString(ARG_HIGHLIGHT, "");

        htmlString.append("<!DOCTYPE html><html><head>");
        htmlString.append("<link href=\"css/common.css\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<link href=\"css/theme.css\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<script src=\"js/mark.8.11.1.min.js\" charset=\"utf-8\"></script>\n");
        htmlString.append("</head><body>");
        htmlString.append(body);
        htmlString.append("<script>var highlight='"+highlight.replace("'", "")+"';</script>\n");
        htmlString.append("<script src=\"js/chapter.js\" charset=\"utf-8\"></script>\n");
        htmlString.append("</body></html>");

        // Load content
        mWebView.loadDataWithBaseURL("file:///android_asset/", htmlString.toString(), "text/html", "utf-8", null);
        mWebView.setBackgroundColor(0x00000000);
    }
}
