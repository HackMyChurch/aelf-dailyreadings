package co.epitre.aelf_lectures.bible;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import co.epitre.aelf_lectures.LecturesApplication;

public class BibleBookChapter {
    private String mBookRef;
    private String mChapterRef;
    private String mContent;
    private String mChapterName;

    public BibleBookChapter(@NonNull String bookRef, @NonNull String chapterRef, @NonNull String chapterName) {
        this.mBookRef = bookRef;
        this.mChapterRef = chapterRef;
        this.mChapterName = chapterName;
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

        // Load from the assets
        String assetPath = "bible/"+mBookRef+"/"+mChapterRef+".html";
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

    public String getRoute() {
        return "/"+mBookRef+"/"+mChapterRef;
    }
}
