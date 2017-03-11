package co.epitre.aelf_lectures.data;

import java.io.Serializable;

public class LectureItem implements Serializable {
    /**
     * internal data structure version ID
     */
    private static final long serialVersionUID = 1L;

    public final String longTitle;  // Full, raw title, including ":"
    public final String shortTitle; // Title or part of the title before the ":"
    public final String title;      // Part of the title after the ":" or null
    public final String description;
    public final String reference;
    public final String key;

    public LectureItem(String key, String title, String description, String reference) {
        String[] titleChunks = title.split(":", 2);
        String shortTitle = titleChunks[0].replace("\u00a0", " :").trim().replace("&nbsp;", " ");
        String longTitle = title.trim();

        if (titleChunks.length > 1) {
            title = titleChunks[1].replace("\u00a0", " :").replace("&nbsp;", " ").trim();
            if (title.equalsIgnoreCase(shortTitle)) {
                title = null;
            } else if (title.equalsIgnoreCase(reference)) {
                title = null;
            }
        } else {
            title = null;
        }

        this.key = key;
        this.shortTitle = shortTitle;
        this.longTitle = longTitle;
        this.title = title;
        this.description = description;
        this.reference = reference;
    }
}
