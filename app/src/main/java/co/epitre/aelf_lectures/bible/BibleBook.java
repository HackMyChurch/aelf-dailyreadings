package co.epitre.aelf_lectures.bible;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;


public class BibleBook {
    // Book status
    private String mName;
    private String mRef;
    private List<BibleBookChapter> mChapters;
    private BibleController mBibleController;

    // Cache of books
    private static HashMap<String, BibleBook> mBooks = new HashMap<>();

    //
    // Constructors
    //

    private BibleBook(@NonNull String ref) {
        this.mRef = ref;
        this.mBibleController = BibleController.getInstance();
    }

    public static BibleBook getBook(@NonNull String ref) {
        // Try to load book from cache
        BibleBook book = mBooks.get(ref);
        if (book != null) {
            return book;
        }

        // Create the book instance and add it to the cache
        book = new BibleBook(ref);
        mBooks.put(ref, book);
        return book;
    }

    //
    // Accessors
    //

    public String getName() {
        if (this.mName == null) {
            this.mName = mBibleController.getBookTitle(this.mRef);
        }
        return this.mName;
    }

    //
    // API
    //

    public List<BibleBookChapter> getChapters() {
        if (this.mChapters == null) {
            this.mChapters = mBibleController.getBookChapters(mRef);
        }
        return this.mChapters;
    }

    public BibleBookChapter getChapter(int position) {
        return getChapters().get(position);
    }

    public int getChapterPosition(String chapterRef) {
        int i = 0;
        for (BibleBookChapter chapter: getChapters()) {
            if (chapter.getChapterRef().equals(chapterRef)) {
                return i;
            }
            i++;
        }
        throw new IndexOutOfBoundsException();
    }
}
