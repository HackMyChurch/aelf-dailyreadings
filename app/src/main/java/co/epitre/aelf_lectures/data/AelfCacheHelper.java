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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.preference.PreferenceManager;
import android.util.Log;

import com.getsentry.raven.android.Raven;

import org.piwik.sdk.Tracker;
import org.piwik.sdk.extra.PiwikApplication;
import org.piwik.sdk.extra.TrackHelper;


/**
 * Internal cache manager (SQLite). There is one table per office and one line per day.
 * Each line tracks the
 * - office date
 * - office content (serialized list<LectureItem>)
 * - when this office was loaded               --> used for server initiated invalidation
 * - which version of the application was used --> used for upgrade initiated invalidation
 */

final class AelfCacheHelper extends SQLiteOpenHelper {
    private static final String TAG = "AELFCacheHelper";
    private static final int DB_VERSION = 3;
    private static final String DB_NAME = "aelf_cache.db";
    private SharedPreferences preference = null;
    private Context ctx;
    Tracker tracker;

    private static final String DB_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS `%s` (" +
            "date TEXT PRIMARY KEY," +
            "lectures BLOB," +
            "create_date TEXT," +
            "create_version INTEGER" +
            ")";
    private static final String DB_TABLE_SET = "INSERT OR REPLACE INTO `%s` VALUES (?,?,?,?)";

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat keyFormatter = new SimpleDateFormat("yyyy-MM-dd");

    // TODO: prepare requests

    AelfCacheHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        preference = PreferenceManager.getDefaultSharedPreferences(context);
        ctx = context;
        tracker = ((PiwikApplication) context.getApplicationContext()).getTracker();
    }

    /**
     * Api
     */

    @SuppressLint("SimpleDateFormat")
    private String computeKey(GregorianCalendar when) {
        if (when == null) {
            return "0000-00-00";
        }
        return keyFormatter.format(when.getTime());
    }

    private void onSqliteError(SQLiteException e) {
        // If a migration did not go well, the best we can do is drop the database and re-create
        // it from scratch. This is hackish but should allow more or less graceful recoveries.
        Log.e(TAG, "Critical database error. Droping + Re-creating", e);
        Raven.capture(e);
        TrackHelper.track().event("Office", "cache.db.error").name("critical").value(1f).with(tracker);

        // Close and drop the database. It will be re-opened automatically
        close();
        ctx.deleteDatabase(DB_NAME);
    }
    
    private boolean _execute_stmt(SQLiteStatement stmt, int max_retries) {
        // Attempt to run this statement up to max_retries times
        do {
            try {
                stmt.execute();
                return true;
            } catch(SQLiteException e) {
                Log.w(TAG, "Failed to save item in cache (SQLiteException): "+e.toString());
                Raven.capture(e);
            } catch(IllegalStateException e) {
                Log.w(TAG, "Failed to save item in cache (IllegalStateException): "+e.toString());
                Raven.capture(e);
            }

        } while (--max_retries > 0);

        // All attempts failed
        return false;
    }

    void store(LecturesController.WHAT what, GregorianCalendar when, List<LectureItem> lectures) {
        String key  = computeKey(when);
        String create_date = computeKey(new GregorianCalendar());

        // Build version number
        long create_version = preference.getInt("version", -1);

        // build blob
        byte[] blob;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos;
            oos = new ObjectOutputStream(bos);
            oos.writeObject(lectures);
            blob = bos.toByteArray();
        } catch (IOException e) {
            Raven.capture(e);
            throw new RuntimeException(e);
        }

        // insert into the database
        String sql = String.format(DB_TABLE_SET, what);
        SQLiteStatement stmt;
        try {
            stmt = getWritableDatabase().compileStatement(sql);
        } catch (SQLiteException e) {
            // Drop DB and retry
            onSqliteError(e);
            stmt = getWritableDatabase().compileStatement(sql);
        }

        stmt.bindString(1, key);
        stmt.bindBlob(2, blob);
        stmt.bindString(3, create_date);
        stmt.bindLong(4, create_version);

        // Multiple attempts. On failure ignore. This is cache --> best effort
        _execute_stmt(stmt, 3);
    }

    // cleaner helper method
    void truncateBefore(LecturesController.WHAT what, GregorianCalendar when) {
        String key = computeKey(when);
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.delete(what.toString(), "`date` < ?", new String[] {key});
        } catch (SQLiteException e) {
            // Drop DB and retry
            onSqliteError(e);
            db.delete(what.toString(), "`date` < ?", new String[] {key});
        }
    }

    // cast is not checked when decoding the blob but we where responsible for its creation so... dont care
    @SuppressWarnings("unchecked")
    List<LectureItem> load(LecturesController.WHAT what, GregorianCalendar when, GregorianCalendar minLoadDate, Long minLoadVersion) {
        String key  = computeKey(when);
        String min_create_date = computeKey(minLoadDate);
        String min_create_version = String.valueOf(minLoadVersion);
        byte[] blob;

        // load from db
        Log.i(TAG, "Trying to load lecture from cache create_date>="+min_create_date+" create_version>="+min_create_version);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur;
        try {
            cur = db.query(
                    what.toString(),                                           // FROM
                    new String[]{"lectures", "create_date", "create_version"}, // SELECT
                    "`date`=? AND `create_date` >= ? AND create_version >= ?", // WHERE
                    new String[]{key, min_create_date, min_create_version},    // params
                    null, null, null, "1"                                      // GROUP BY, HAVING, ORDER, LIMIT
            );
        } catch (SQLiteException e) {
            // Drop DB and retry
            onSqliteError(e);
            cur = db.query(
                    what.toString(),                                           // FROM
                    new String[]{"lectures", "create_date", "create_version"}, // SELECT
                    "`date`=? AND `create_date` >= ? AND create_version >= ?", // WHERE
                    new String[]{key, min_create_date, min_create_version},    // params
                    null, null, null, "1"                                      // GROUP BY, HAVING, ORDER, LIMIT
            );
        }

        if(cur != null && cur.getCount() > 0) {
            // any records ? load it
            cur.moveToFirst();
            blob = cur.getBlob(0);

            Log.i(TAG, "Loaded lecture from cache create_date="+cur.getString(1)+" create_version="+cur.getLong(2));

            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(blob);
                ObjectInputStream ois = new ObjectInputStream(bis);

                return (List<LectureItem>)ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                Raven.capture(e);
                throw new RuntimeException(e);
            } finally {
                cur.close();
            }

        } else {
            return null;
        }
    }

    boolean has(LecturesController.WHAT what, GregorianCalendar when, GregorianCalendar minLoadDate, Long minLoadVersion) {
        String key  = computeKey(when);
        String min_create_date = computeKey(minLoadDate);
        String min_create_version = String.valueOf(minLoadVersion);

        // load from db
        Log.i(TAG, "Checking if lecture is in cache with create_date>="+min_create_date+" create_version>="+min_create_version);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur;
        try {
            cur = db.query(
                    what.toString(),                                           // FROM
                    new String[]{"date"},                                      // SELECT
                    "`date`=? AND `create_date` >= ? AND create_version >= ?", // WHERE
                    new String[]{key, min_create_date, min_create_version},    // params
                    null, null, null, "1"                                      // GROUP BY, HAVING, ORDER, LIMIT
            );
        } catch (SQLiteException e) {
            return false;
        }

        return cur != null && cur.getCount() > 0;
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
        if(oldVersion <= 1) {
            Log.i(TAG, "Upgrading DB from version 1");
            createCache(db, LecturesController.WHAT.METAS);
        }

        if(oldVersion <= 2) {
            // Add create_date + create_version for finer grained invalidation
            Log.i(TAG, "Upgrading DB from version 2");
            db.beginTransaction();
            try {
                for (LecturesController.WHAT what: LecturesController.WHAT.values()) {
                    db.execSQL("ALTER TABLE `" + what + "` ADD COLUMN create_date TEXT");
                    db.execSQL("ALTER TABLE `" + what + "` ADD COLUMN create_version INTEGER;");
                    db.execSQL("UPDATE `" + what + "` SET create_date = '0000-00-00', create_version = 0;");
                }
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Raven.capture(e);
                throw e;
            } finally {
                db.endTransaction();
            }
        }
    }

}