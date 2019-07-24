package co.epitre.aelf_lectures.bible;

import android.content.res.AssetManager;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import co.epitre.aelf_lectures.LecturesApplication;

public class BibleBook {
    private final String BIBLE_DIR = "bible";

    // Book status
    private String mName;
    private String mRef;
    private ArrayList<BibleBookChapter> mChapters;

    // Cache of books
    private static HashMap<String, BibleBook> mBooks = new HashMap<>();

    //
    // Constructors
    //

    private BibleBook(@NonNull String ref, @NonNull String name) {
        this.mName = name;
        this.mRef = ref;
    }

    public static BibleBook getBook(@NonNull String ref, String name) {
        // Try to load book from cache
        BibleBook book = mBooks.get(ref);
        if (book != null) {
            return book;
        }

        // FIXME: We need the name to instanciate the book. This will become an issue when we access
        // bible books by URL.
        if (name == null) {
            return null;
        }

        // Create the book instance and add it to the cache
        book = new BibleBook(ref, name);
        mBooks.put(ref, book);
        return book;
    }

    //
    // Accessors
    //

    public String getName() {
        return this.mName;
    }

    public String getRef() {
        return this.mRef;
    }

    //
    // API
    //

    public List<BibleBookChapter> getChapters() {
        // Check if we have a cached version
        if (this.mChapters != null) {
            return this.mChapters;
        }

        // Allocate chapters list
        this.mChapters = new ArrayList<>();

        // Check if we have chapters
        if (mRef == null) {
            return this.mChapters;
        }

        AssetManager assets = LecturesApplication.getInstance().getAssets();

        // Get all chapters
        String[] files;
        try {
            files = assets.list(BIBLE_DIR+"/"+mRef);
        } catch (IOException e) {
            return this.mChapters;
        }

        for (String name : files) {
            if (name.endsWith(".html")) {
                BibleBookChapter chapter = new BibleBookChapter(mRef, name.split("\\.")[0]);
                this.mChapters.add(chapter);
            }
        }

        // If there is a single chapter, set the chapter title to the book name
        if (this.mChapters.size() == 1) {
            this.mChapters.get(0).setChapterName(mName);
        }

        // Sort the chapters in natural order
        Collections.sort(this.mChapters, new BibleBookChapter.BibleBookChapterComparator());
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
