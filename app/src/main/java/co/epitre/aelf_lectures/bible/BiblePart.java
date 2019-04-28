package co.epitre.aelf_lectures.bible;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class BiblePart {
    private String mName;
    private ArrayList<BibleBookEntry> mBookEntries;

    public BiblePart(@NonNull String name) {
        this.mName = name;
        this.mBookEntries = new ArrayList<>();
    }

    public String getName() {
        return this.mName;
    }

    public List<BibleBookEntry> getBibleBookEntries() {
        return this.mBookEntries;
    }

    public BiblePart addBibleBookEntry(@NonNull BibleBookEntry bibleBookEntry) {
        this.mBookEntries.add(bibleBookEntry);
        return this;
    }
}