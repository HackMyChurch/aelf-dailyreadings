package co.epitre.aelf_lectures.bible.data;

import androidx.annotation.NonNull;

public class BibleVerse {
    private String mRef;
    private String mText;

    public BibleVerse(String verseRef, @NonNull String text) {
        this.mRef = verseRef;
        this.mText = text;
    }

    public String getRef() {
        return this.mRef;
    }

    public String getText() {
        return this.mText;
    }
}

