package co.epitre.aelf_lectures.bible.data;

import androidx.annotation.NonNull;

import java.util.List;


public class BibleBookEntry {
    private BibleBookEntryType mType;
    private String mBookName;
    private String mEntryName;
    private String mBookRef;
    private String mChapterRef;
    private BibleBook mBook;

    // Section title entry constructor
    public BibleBookEntry(@NonNull BibleBookEntryType type, @NonNull String bookName) {
        this(type, bookName, null);
    }

    // Book entry constructor
    public BibleBookEntry(@NonNull BibleBookEntryType type, @NonNull String bookName, String bookRef) {
        this(type, bookName, bookName, bookRef, null);
    }

    // Chapter entry constructor
    public BibleBookEntry(@NonNull BibleBookEntryType type, @NonNull String bookName, String entryName, String bookRef, String chapterRef) {
        this.mType = type;
        this.mBookName = bookName;
        this.mEntryName = entryName;
        this.mBookRef = bookRef;
        this.mChapterRef = chapterRef;
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

    public String getEntryName() {
        return this.mEntryName;
    }

    public String getBookRef() {
        return this.mBookRef;
    }

    public String getChapterRef() {
        return this.mChapterRef;
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