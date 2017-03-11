package co.epitre.aelf_lectures.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;


/**
 * Internal cache manager (SQLite)
 */

final class AelfCacheHelper extends SQLiteOpenHelper {
    private static final String TAG = "AELFCacheHelper";
    private static final int DB_VERSION = 2;
    private static final String DB_NAME = "aelf_cache.db";

    private static final String DB_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS `%s` (date TEXT PRIMARY KEY, lectures BLOB)";
    private static final String DB_TABLE_SET = "INSERT OR REPLACE INTO `%s` VALUES (?,?)";

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat keyFormatter = new SimpleDateFormat("yyyy-MM-dd");

    // TODO: prepare requests

    AelfCacheHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Api
     */

    @SuppressLint("SimpleDateFormat")
    private String computeKey(GregorianCalendar when) {
        return keyFormatter.format(when.getTime());
    }
    
    private boolean _execute_stmt(SQLiteStatement stmt, int max_retries) {
        // Attempt to run this statement up to max_retries times
        do {
            try {
                stmt.execute();
                return true;
            } catch(SQLiteException e) {
                Log.w(TAG, "Failed to save item in cache (SQLiteException): "+e.toString());
            } catch(IllegalStateException e) {
                Log.w(TAG, "Failed to save item in cache (IllegalStateException): "+e.toString());
            }

        } while (--max_retries > 0);

        // All attempts failed
        return false;
    }

    void store(LecturesController.WHAT what, GregorianCalendar when, List<LectureItem> lectures) {
        String key  = computeKey(when);
        byte[] blob;

        // build blob
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos;
            oos = new ObjectOutputStream(bos);
            oos.writeObject(lectures);
            blob = bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // insert into the database
        String sql = String.format(DB_TABLE_SET, what);
        SQLiteStatement stmt = getWritableDatabase().compileStatement(sql);
        stmt.bindString(1, key);
        stmt.bindBlob(2, blob);

        // Multiple attempts. On failure ignore. This is cache --> best effort
        _execute_stmt(stmt, 3);
    }

    // cleaner helper method
    void truncateBefore(LecturesController.WHAT what, GregorianCalendar when) {
        String key = computeKey(when);
        SQLiteDatabase db = getWritableDatabase();
        db.delete(what.toString(), "`date` < ?", new String[] {key});
    }

    // cast is not checked when decoding the blob but we where responsible for its creation so... dont care
    @SuppressWarnings("unchecked")
    List<LectureItem> load(LecturesController.WHAT what, GregorianCalendar when) {
        String key  = computeKey(when);
        byte[] blob;

        // load from db
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.query(what.toString(), new String[] {"lectures"}, "`date`=?", new String[] {key}, null, null, null, "1");
        if(cur != null && cur.getCount() > 0) {
            // any records ? load it
            cur.moveToFirst();
            blob = cur.getBlob(0);

            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(blob);
                ObjectInputStream ois = new ObjectInputStream(bis);

                return (List<LectureItem>)ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException(e);
            } finally {
                cur.close();
            }

        } else {
            return null;
        }
    }

    /**
     * Internal logic
     */
    
    private void createCache(SQLiteDatabase db, LecturesController.WHAT what) {
        String sql = String.format(DB_TABLE_CREATE, what);
        db.execSQL(sql);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (LecturesController.WHAT what : LecturesController.WHAT.class.getEnumConstants()) {
            createCache(db, what);
        }
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // FIXME: how do I make sure all tables are always present ?
        if(oldVersion == 1) {
            createCache(db, LecturesController.WHAT.METAS);
        }
    }

}