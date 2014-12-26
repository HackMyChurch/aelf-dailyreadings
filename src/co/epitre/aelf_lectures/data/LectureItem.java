package co.epitre.aelf_lectures.data;

import java.io.Serializable;

public class LectureItem implements Serializable {
    /**
     * internal data structure version ID
     */
    private static final long serialVersionUID = 1L;

    public final String longTitle;
    public final String shortTitle;
    public final String description;
    public final String category;

    public LectureItem(String title, String description, String category) {
    	String[] titleChunks = title.split(":");
    	String shortTitle = titleChunks[0].trim();
    	String longTitle = title.trim();

    	this.shortTitle = shortTitle;
    	this.longTitle = longTitle;
    	this.description = description;
    	this.category = category;
    }
}
