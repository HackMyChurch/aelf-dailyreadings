package co.epitre.aelf_lectures.bible.data;

import static org.sqlite.database.sqlite.SQLiteDatabase.OPEN_READONLY;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import org.sqlite.database.sqlite.SQLiteDatabase;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.epitre.aelf_lectures.LecturesApplication;

public class BibleController {
    private static final String TAG = "BibleController";
    private static BibleController instance;
    private InitThread initThread;
    private File dbFile;
    private SQLiteDatabase db;

    private static final List<String> tokenIgnore = Arrays.asList(
            // Conjonctions de coordinations
            "mais", "ou", "et", "donc", "or", "ni", "car",

            // Conjonctions de subordination (shortest/most frequent only)
            "qu", "que", "si", "alors", "tandis",

            // Déterminants
            "le", "la", "les", "un", "une", "du", "de", "la",
            "ce", "cet", "cette", "ces",
            "ma", "ta", "sa",
            "mon", "ton", "son", "notre", "votre", "leur",
            "nos", "tes", "ses", "nos", "vos", "leurs",

            // Interrogatifs
            "quel", "quelle", "quelles", "quoi"
    );

    public enum Sort {
        Relevance,
        Bible,
    }

    synchronized public static BibleController getInstance() {
        if(instance == null) {
            instance = new BibleController();
        }
        return instance;
    }

    private BibleController() {
        initThread = new InitThread(LecturesApplication.getInstance());
        initThread.start();
    }

    private void waitReady() {
        while (true) {
            try {
                initThread.join();
                break;
            } catch (InterruptedException e) {
                continue;
            }
        }
    }
    private boolean shouldIgnore(String token) {
        token = token.toLowerCase();
        token = Normalizer.normalize(token, Normalizer.Form.NFD).replaceAll("[^a-z0-9* ]","");

        if (token.length() <= 1) {
            return true;
        }

        // Ignore too common words
        if (tokenIgnore.contains(token)) {
            return true;
        }

        return false;
    }

    public String getBookTitle(String bookRef) {
        waitReady();

        Cursor cursor = db.query(
                true,
                "verses",
                new String[]{"book_title"},
                "book = '?'",
                new String[]{bookRef},
                null,
                null,
                null,
                "1"
        );

        if (cursor.getCount() != 1) {
            return null;
        }

        return cursor.getString(0);
    }

    public List<BibleBookChapter> getBookChapters(String bookRef) {
        waitReady();

        Cursor cursor = db.query(
                true,
                "verses",
                new String[]{"chapter", "chapter_title", "book_title"},
                "book = ?",
                new String[]{bookRef},
                null,
                null,
                "CAST(chapter AS INTEGER)",
                null
        );

        // Allocate chapters list
        int chapterCount = cursor.getCount();
        ArrayList<BibleBookChapter> chapters = new ArrayList<>(chapterCount);

        // Fill the chapter list
        try {
            while (cursor.moveToNext()) {
                String chapterRef = cursor.getString(0);
                String chapterTitle = cursor.getString(1);
                if (chapterCount == 1) {
                    chapterTitle = cursor.getString(2);
                }
                BibleBookChapter chapter = new BibleBookChapter(bookRef, chapterRef, chapterTitle);
                chapters.add(chapter);
            }
        } finally {
            cursor.close();
        }

        return chapters;
    }

    public List<BibleVerse> getBookChapterVerses(String bookRef, String chapterRef) {
        waitReady();

        Cursor cursor = db.query(
                "verses",
                new String[]{"verse", "text"},
                "book = ? AND chapter = ?",
                new String[]{bookRef, chapterRef},
                null,
                null,
                "rowid",
                null
        );

        // Allocate verses list
        ArrayList<BibleVerse> verses = new ArrayList<>(cursor.getCount());

        // Fill the verse list
        try {
            while (cursor.moveToNext()) {
                String verseRef = cursor.getString(0);
                if(cursor.isNull(0)) {
                    verseRef = "";
                }
                String verseText = cursor.getString(1);
                BibleVerse verse = new BibleVerse(verseRef, verseText);
                verses.add(verse);
            }
        } finally {
            cursor.close();
        }

        return verses;
    }

    public Cursor search(String search, Sort sort) {
        waitReady();

        // Filter joining characters
        search = search.replaceAll("[-']"," ");

        // Build a list of tokens
        //Log.i(TAG, "search: "+search);
        List<String> tokens = new ArrayList<>();
        for(String token: search.split("\\s+")) {
            if (shouldIgnore(token)) {
                continue;
            }

            tokens.add(token);
        }

        // Try fast path (all terms)
        Cursor cursor = searchFast(search, tokens, sort);
        if (cursor.getCount() > 0 || tokens.size() <= 1) {
            return cursor;
        }

        // Fallback on slow path (Variants with each token omitted once)
        return searchSlow(search, tokens, sort);
    }

    private Cursor searchFast(String search, List<String> tokens, Sort sort) {
        // Build the query
        StringBuilder queryBuilder = new StringBuilder();

        // Base query: all terms
        queryBuilder.append(" SELECT book, chapter, title, rank, '' AS skipped, snippet(search, -1, '<b>', '</b>', '...', 32) AS snippet");
        queryBuilder.append(" FROM search");
        queryBuilder.append(" WHERE text MATCH '\"");
        queryBuilder.append(search);
        queryBuilder.append("\"");
        if (!tokens.isEmpty()) {
            queryBuilder.append(" OR ");
            queryBuilder.append(" NEAR(\"");
            queryBuilder.append(TextUtils.join("\" \"", tokens));
            queryBuilder.append("\", ");
            queryBuilder.append(tokens.size()*2);
            queryBuilder.append(" ) OR ");
            queryBuilder.append(TextUtils.join(" ", tokens));
        }
        queryBuilder.append("'");
        queryBuilder.append(orderByLimit(sort));
        queryBuilder.append(";");

        // Launch the query
        //Log.i(TAG, "search: "+queryBuilder.toString());
        return db.rawQuery(queryBuilder.toString(), null);
    }

    private Cursor searchSlow(String search, List<String> tokens, Sort sort) {
        // Build the query
        StringBuilder queryBuilder = new StringBuilder();

        // Query header
        queryBuilder.append("SELECT * FROM (");

        // Base query: all terms
        queryBuilder.append(" SELECT book, chapter, title, rank, '' AS skipped, snippet(search, -1, '<b>', '</b>', '...', 32) AS snippet");
        queryBuilder.append(" FROM search");
        queryBuilder.append(" WHERE text MATCH '\"");
        queryBuilder.append(search);
        queryBuilder.append("\" OR ");
        queryBuilder.append(" NEAR(\"");
        queryBuilder.append(TextUtils.join("\" \"", tokens));
        queryBuilder.append("\", ");
        queryBuilder.append(tokens.size()*2);
        queryBuilder.append(" ) OR ");
        queryBuilder.append(TextUtils.join(" ", tokens));
        queryBuilder.append("'");

        // Query variants: skip each of the tokens once
        double rankScaling = ((double)tokens.size() - 1) / (double)tokens.size();
        // Generate query with one skipped token
        for (int i = 0; i < tokens.size(); i++) {
            queryBuilder.append(" UNION");
            queryBuilder.append(" SELECT book, chapter, title, rank * "+rankScaling+", '"+tokens.get(i).replace("*", "")+"' AS skipped, snippet(search, -1, '<b>', '</b>', '...', 32) AS snippet");
            queryBuilder.append(" FROM search");
            queryBuilder.append(" WHERE text MATCH '");
            queryBuilder.append(" NEAR(");
            for (int j = 0; j < tokens.size(); j++) {
                if (i == j) {
                    continue;
                }
                queryBuilder.append("\"");
                queryBuilder.append(tokens.get(j));
                queryBuilder.append("\" ");
            }
            queryBuilder.append(", ");
            queryBuilder.append((tokens.size() - 1)*2);
            queryBuilder.append(" ) OR ");
            for (int j = 0; j < tokens.size(); j++) {
                if (i == j) {
                    continue;
                }
                queryBuilder.append(" ");
                queryBuilder.append(tokens.get(j));
            }
            queryBuilder.append("'");
        }

        // Query trailer
        queryBuilder.append(orderByLimit(sort));
        queryBuilder.append(")");
        queryBuilder.append(orderByLimit(sort));
        queryBuilder.append(";");

        // Launch the query
        //Log.i(TAG, "search: "+queryBuilder.toString());
        return db.rawQuery(queryBuilder.toString(), null);
    }

    private String orderByLimit(Sort sort) {
        switch (sort) {
            case Bible:
                return " ORDER BY CAST(book_id as INTEGER),CAST(chapter AS INTEGER)";
            case Relevance:
                return " ORDER BY rank LIMIT 50";
            default:
                return " ";
        }
    }

    class InitThread extends Thread {
        private Context context;

        public InitThread(Context context) {
            this.context = context;
        }

        public void run() {
            // Load index from cache
            CopyIndexToCache();

            // Open index
            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, OPEN_READONLY);
        }

        private void CopyIndexToCache() {
            InputStream in = null;
            OutputStream out = null;
            String filename = "bible.db";
            dbFile = new File(context.getCacheDir(), filename);

            // Skip copy if already done to avoid I/Os
            if (dbFile.exists()) {
                return;
            }

            // Copy from assets
            AssetManager assetManager = context.getAssets();
            try {
                // Open source and destination
                in = assetManager.open(filename);
                out = new FileOutputStream(dbFile);

                // Copy
                byte[] buffer = new byte[1024];
                int read;
                while((read = in.read(buffer)) != -1){
                    out.write(buffer, 0, read);
                }
            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            } finally {
                close(in);
                close(out);
            }
        }

        private void close(Closeable closeable) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }
    }
}
