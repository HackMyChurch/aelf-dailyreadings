package co.epitre.aelf_lectures.bible.data;

import androidx.annotation.NonNull;


public class BibleBookChapter {
    private String mBookRef;
    private String mChapterRef;
    private String mContent;
    private String mChapterName;
    private BibleController mBibleController;

    public BibleBookChapter(@NonNull String bookRef, @NonNull String chapterRef, @NonNull String chapterName) {
        this.mBookRef = bookRef;
        this.mChapterRef = chapterRef;
        this.mChapterName = chapterName;
        this.mBibleController = BibleController.getInstance();
    }

    //
    // Accessors
    //

    public String getChapterRef() {
        return this.mChapterRef;
    }

    public String getChapterName() {
        return mChapterName;
    }

    @NonNull
    @Override
    public String toString() {
        return getChapterName();
    }

    //
    // API
    //

    public String getContent() {
        // Try to load from the internal cache
        if (mContent != null) {
            return mContent;
        }

        StringBuilder chapterStringBuilder = new StringBuilder();

        // Insert the title
        chapterStringBuilder.append("<h3>");
        chapterStringBuilder.append(getChapterName());
        chapterStringBuilder.append("</h3>");

        // Load from the database
        chapterStringBuilder.append("<p>");
        for (BibleVerse bibleVerse: mBibleController.getBookChapterVerses(mBookRef, mChapterRef)) {
            String verseRef = bibleVerse.getRef();
            chapterStringBuilder.append("<span class=\"line\"");
            if (verseRef != null && !verseRef.isEmpty()) {
                chapterStringBuilder.append(" id=\"verse-");
                chapterStringBuilder.append(verseRef);
                chapterStringBuilder.append("\"");
            }
            chapterStringBuilder.append(" tabindex=\"0\">");
            if (verseRef != null && !verseRef.isEmpty()) {
                chapterStringBuilder.append("<span aria-hidden=\"true\" class=\"verse\">");
                chapterStringBuilder.append(verseRef);
                chapterStringBuilder.append("</span>");
            }
            chapterStringBuilder.append(bibleVerse.getText());
            chapterStringBuilder.append("</span>");
        }
        chapterStringBuilder.append("</p>");

        mContent = chapterStringBuilder.toString();
        return mContent;
    }

    public String getRoute() {
        return "/"+mBookRef+"/"+mChapterRef;
    }
}
