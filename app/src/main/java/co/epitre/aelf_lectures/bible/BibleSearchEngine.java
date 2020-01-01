package co.epitre.aelf_lectures.bible;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sqlite.database.sqlite.SQLiteDatabase;

import co.epitre.aelf_lectures.LecturesApplication;

import static org.sqlite.database.sqlite.SQLiteDatabase.OPEN_READONLY;

public class BibleSearchEngine {
    private static BibleSearchEngine instance;
    private InitThread initThread;
    private File dbFile;
    private SQLiteDatabase db;

    synchronized public static BibleSearchEngine getInstance() {
        if(instance == null) {
            instance = new BibleSearchEngine();
        }
        return instance;
    }

    private BibleSearchEngine() {
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

    public Cursor search(String query) {
        waitReady();

        // Attempt to match exact search OR regular 'AND' search. This improves the score of exact matches
        query = "\""+query+"\" OR "+query;

        // FIXME: to improve results, we could parse the query into words, add '*' to each and add AND clauses
        return db.query(
                "search",
                new String[]{"book", "chapter", "title", "rank", "snippet(search, -1, '<b>', '</b>', '...', 32) AS snippet"},
                "content MATCH ?",
                new String[]{query},
                null,
                null,
                "rank"
        );
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
