package co.epitre.aelf_lectures.bible.data;

public enum BibleBookEntryType {
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
