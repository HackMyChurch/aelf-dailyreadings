package co.epitre.aelf_lectures.bible;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;

import co.epitre.aelf_lectures.LecturesApplication;
import co.epitre.aelf_lectures.utils.AlphanumComparator;

public class BibleBookChapter {
    private String mBookRef;
    private String mChapterRef;
    private String mContent;
    private String mChapterName;

    public static class BibleBookChapterComparator implements Comparator<BibleBookChapter> {
        AlphanumComparator alphanumComparator = new AlphanumComparator();

        @Override
        public int compare(BibleBookChapter o1, BibleBookChapter o2) {
            return alphanumComparator.compare(o1.mChapterRef, o2.mChapterRef);
        }
    }

    public BibleBookChapter(@NonNull String bookRef, @NonNull String chapterRef) {
        this.mBookRef = bookRef;
        this.mChapterRef = chapterRef;

        // Compute chapter name
        if (mChapterRef.equals("0") && mBookRef.equals("Est")) {
            mChapterName = "Préliminaires";
        } else if (mChapterRef.equals("0") && mBookRef.equals("Si")) {
            mChapterName = "Prologue";
        } else if (mBookRef.equals("Ps")) {
            mChapterName = "Psaume "+this.mChapterRef;
        } else {
            mChapterName = "Chapitre "+this.mChapterRef;
        }
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

    public void setChapterName(String chapterName) {
        mChapterName = chapterName;
    }

    //
    // API
    //

    public String getContent() {
        // Try to load from the internal cache
        if (mContent != null) {
            return mContent;
        }

        // Load from the assets
        String assetPath = "bible/"+mBookRef+"/"+mChapterRef+".html";
        StringBuilder chapterStringBuilder = new StringBuilder();
        try {
            InputStreamReader chapterStream = new InputStreamReader(LecturesApplication.getInstance().getAssets().open(assetPath));
            BufferedReader br = new BufferedReader(chapterStream);
            String line;

            while ((line = br.readLine()) != null) {
                chapterStringBuilder.append(line);
                chapterStringBuilder.append('\n');
            }
            br.close();
        } catch (IOException e) {
            return "";
        }

        mContent = chapterStringBuilder.toString();
        return mContent;
    }
}
