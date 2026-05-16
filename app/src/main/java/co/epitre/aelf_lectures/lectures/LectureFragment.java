package co.epitre.aelf_lectures.lectures;

import android.os.Bundle;

import co.epitre.aelf_lectures.components.ReadingFragment;
import co.epitre.aelf_lectures.lectures.data.office.Lecture;
import co.epitre.aelf_lectures.lectures.data.office.LectureVariants;

/**
 * "Lecture" renderer
 */
public class LectureFragment extends ReadingFragment {
    private static final String TAG = "LectureFragment";

    /**
     * The fragment arguments
     */
    public static final String ARG_POSITION = "position";
    public static final String ARG_VARIANT = "variant";
    public static final String ARG_WHAT = "office";
    public static final String ARG_WHEN = "date";

    @Override
    protected void loadText() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }

        // Get the lecture
        int position = args.getInt(ARG_POSITION);
        int variant = args.getInt(ARG_VARIANT);
        String officeName = args.getString(ARG_WHAT, "");
        String officeDate = args.getString(ARG_WHEN, "");

        SectionLecturesFragment parent = (SectionLecturesFragment)getParentFragment();
        if (parent == null) {
            return;
        }

        LectureVariants lectureVariants = parent.getLectureVariants(position);
        if (lectureVariants == null) {
            return;
        }

        Lecture lecture = lectureVariants.get(variant);

        // Build HTML
        StringBuilder htmlString = new StringBuilder();
        htmlString.append("<!DOCTYPE html><html><head>");
        htmlString.append("<link href=\"css/common.css\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<link href=\"");
        htmlString.append(getThemeCss());
        htmlString.append("\" type=\"text/css\" rel=\"stylesheet\" media=\"screen\" />");
        htmlString.append("<style>");
        htmlString.append(getAccessibilityCss());
        htmlString.append("</style>");
        htmlString.append("</head>");
        htmlString.append("<body>");
        htmlString.append(lecture.toHtml());
        htmlString.append("<script src=\"js/lecture.js\" charset=\"utf-8\"></script>\n");
        htmlString.append("</body></html>");

        String reading = htmlString.toString();

        // Build history URL
        StringBuilder UrlString = new StringBuilder();
        if (!officeName.isEmpty()) {
            UrlString.append("aelf:office/");
            UrlString.append(officeDate);
            UrlString.append("/");
            UrlString.append("/");
            UrlString.append(officeName);
            UrlString.append("/");
            UrlString.append(position);
            UrlString.append("#variant=");
            UrlString.append(variant);
        }

        // load content
        this.setWebViewContent(reading, UrlString.toString());
    }
}
