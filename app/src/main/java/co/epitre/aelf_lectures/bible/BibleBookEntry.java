package co.epitre.aelf_lectures.bible;

import androidx.annotation.NonNull;

enum BibleBookEntryType {
    SECTION(0),
    BOOK(1),
    ;

    private int mValue;

    BibleBookEntryType(int value) {
        mValue = value;
    }

    public static BibleBookEntryType fromValue(int value) {
        for (BibleBookEntryType bookListEntryType : BibleBookEntryType.values()) {
            if(bookListEntryType.mValue == value) {
                return bookListEntryType;
            }
        }
        return null;
    }

    public int getValue() {
        return mValue;
    }
}

public class BibleBookEntry {
    private BibleBookEntryType mType;
    private String mName;

    public BibleBookEntry(BibleBookEntryType type, @NonNull String name) {
        this.mType = type;
        this.mName = name;
    }

    public BibleBookEntryType getType() {
        return mType;
    }

    public String getName() {
        return this.mName;
    }
}