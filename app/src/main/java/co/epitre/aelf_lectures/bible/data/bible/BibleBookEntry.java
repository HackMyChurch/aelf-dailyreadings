package co.epitre.aelf_lectures.bible.data.bible;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class BibleBookEntry {
    private final BibleBookEntryType mType;
    private final String mBookName;
    private final String mShortBookName;
    private final String mEntryName;
    private final String mBookRef;
    private final String mChapterRef;
    private final List<String> mAliases;
    private BibleBook mBook;

    // Section title entry constructor
    public BibleBookEntry(@NonNull BibleBookEntryType type, @NonNull String bookName) {
        this(type, bookName, null, bookName, null, null, null);
    }

    // Book entry constructor without shortBookName or aliases
    public BibleBookEntry(@NonNull BibleBookEntryType type, @NonNull String bookName, String bookRef) {
        this(type, bookName, null, bookName, bookRef, null, null);
    }

    // Book entry constructor with explicit shortBookName and bookRef
    public BibleBookEntry(@NonNull BibleBookEntryType type, @NonNull String bookName, String shortBookName, String bookRef) {
        this(type, bookName, shortBookName, bookName, bookRef, null, null);
    }

    // Book entry constructor with explicit shortBookName, bookRef, and aliases
    public BibleBookEntry(@NonNull BibleBookEntryType type, @NonNull String bookName, String shortBookName, String bookRef, String[] aliases) {
        this(type, bookName, shortBookName, bookName, bookRef, null, aliases);
    }

    // Book entry constructor with explicit aliases array
    public BibleBookEntry(@NonNull BibleBookEntryType type, @NonNull String bookName, String bookRef, String[] aliases) {
        this(type, bookName, null, bookName, bookRef, null, aliases);
    }

    // Chapter entry constructor
    public BibleBookEntry(@NonNull BibleBookEntryType type, @NonNull String bookName, String entryName, String bookRef, String chapterRef) {
        this(type, bookName, null, entryName, bookRef, chapterRef, null);
    }

    // Master constructor
    public BibleBookEntry(@NonNull BibleBookEntryType type, @NonNull String bookName, String shortBookName, String entryName, String bookRef, String chapterRef, String[] aliases) {
        this.mType = type;
        this.mBookName = bookName;
        this.mShortBookName = shortBookName;
        this.mEntryName = entryName != null ? entryName : bookName;
        this.mBookRef = bookRef;
        this.mChapterRef = chapterRef;
        this.mAliases = new ArrayList<>();
        if (shortBookName != null && !shortBookName.trim().isEmpty()) {
            this.mAliases.add(shortBookName.trim());
        }
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias != null && !alias.trim().isEmpty()) {
                    String trimmed = alias.trim();
                    if (!this.mAliases.contains(trimmed)) {
                        this.mAliases.add(trimmed);
                    }
                }
            }
        }
    }

    //
    // Accessors
    //

    public BibleBookEntryType getType() {
        return mType;
    }

    public String getBookName() {
        return this.mBookName;
    }

    public String getShortBookName() {
        return this.mShortBookName != null ? this.mShortBookName : this.mBookName;
    }

    public String getEntryName() {
        return this.mEntryName;
    }

    public String getBookRef() {
        return this.mBookRef;
    }

    public String getChapterRef() {
        return this.mChapterRef;
    }

    public List<String> getAliases() {
        return this.mAliases;
    }

    //
    // API
    //

    public BibleBook getBook() {
        if (this.mBook == null) {
            this.mBook = BibleBook.getBook(this.mBookRef);
        }
        return this.mBook;
    }

    public List<BibleBookChapter> getChapters() {
        BibleBook book = getBook();
        if (book == null) {
            return null;
        }
        return book.getChapters();
    }

    public BibleBookChapter getChapter(int position) {
        BibleBook book = getBook();
        if (book == null) {
            return null;
        }
        return book.getChapter(position);
    }

    public int getChapterRefPosition() {
        if (this.mChapterRef == null) {
            return 0;
        }
        return getBook().getChapterPosition(this.mChapterRef);
    }
}

