package co.epitre.aelf_lectures.data;

import java.io.Serializable;

import android.R.bool;

public class LectureItem implements Serializable {
    /**
     * internal data structure version ID
     */
    private static final long serialVersionUID = 1L;

    public final String longTitle;
    public final String shortTitle;
    public final String description;
    public final String category;
    public final int guid; // FIXME: remove ?

    /**
     * @param title
     * @param description
     * @param category
     * @param guid
     */
    public LectureItem(String title, String description, String category, int guid) {
    	String[] titleChunks = title.split(":");
    	String shortTitle = titleChunks[0].trim();
    	String longTitle = title.trim();

    	if(shortTitle.equalsIgnoreCase("psaume") && longTitle.length() < 18) {
    		// use a title of the form "Psaume 94" instead of Psaume : 94
    		longTitle = shortTitle = longTitle.replaceFirst(" *: *", " ");
    	}

    	this.shortTitle = shortTitle;
    	this.longTitle = longTitle;
    	this.description = description;
    	this.category = category;
    	this.guid = guid;
    }
}
