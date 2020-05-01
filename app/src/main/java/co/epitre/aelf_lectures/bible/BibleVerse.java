package co.epitre.aelf_lectures.bible;

import androidx.annotation.NonNull;

public class BibleVerse {
    private int mRef;
    private String mText;

    BibleVerse(int verseRef, @NonNull String text) {
        this.mRef = verseRef;
        this.mText = text;
    }

    public int getRef() {
        return this.mRef;
    }

    public String getText() {
        return this.mText;
    }
}
