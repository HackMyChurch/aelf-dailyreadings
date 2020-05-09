package co.epitre.aelf_lectures.lectures.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Callable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

import org.sqlite.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException;
import org.sqlite.database.sqlite.SQLiteConstraintException;
import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteDatabaseCorruptException;
import org.sqlite.database.sqlite.SQLiteDatatypeMismatchException;
import org.sqlite.database.sqlite.SQLiteException;
import org.sqlite.database.sqlite.SQLiteOpenHelper;
import org.sqlite.database.sqlite.SQLiteStatement;

/**
 * Internal cache manager (SQLite). There is one table per office and one line per day.
 * Each line tracks the
 * - office date
 * - office content (serialized list<LectureItem>)
 * - when this office was loaded               --> used for server initiated invalidation
 * - which version of the application was used --> used for upgrade initiated invalidation
 */

public final class AelfCacheHelper extends SQLiteOpenHelper {
    private static final String TAG = "AELFCacheHelper";
    private static final int DB_VERSION = 4;
    private static final String DB_NAME = "aelf_cache.db";
    private SharedPreferences preference = null;
    private Context ctx;

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
        super(context, context.getDatabasePath(DB_NAME).getAbsolutePath(), null, DB_VERSION);
        context.getDatabasePath(DB_NAME).getParentFile().mkdirs();
        preference = PreferenceManager.getDefaultSharedPreferences(context);
        ctx = context;
    }

    /**
     * Api
     */

    public static void dropDatabase(Context context) {
        context.getDatabasePath(DB_NAME).delete();
    }

    public static long getDatabaseSize(Context context) {
        return context.getDatabasePath(DB_NAME).length();
    }

    @SuppressLint("SimpleDateFormat")
    private String computeKey(GregorianCalendar when) {
        if (when == null) {
            return "0000-00-00";
        }
        return keyFormatter.format(when.getTime());
    }

    private void onSqliteError(SQLiteException e) {
        if (
            e instanceof SQLiteBindOrColumnIndexOutOfRangeException ||
            e instanceof SQLiteConstraintException ||
            e instanceof SQLiteDatabaseCorruptException ||
            e instanceof SQLiteDatatypeMismatchException
        ) {
            // If a migration did not go well, the best we can do is drop the database and re-create
            // it from scratch. This is hackish but should allow more or less graceful recoveries.
            Log.e(TAG, "Critical database error. Droping + Re-creating", e);
            close();
            ctx.deleteDatabase(DB_NAME);
        } else {
            // Generic error. Close + re-open
            Log.e(TAG, "Datable "+e.getClass().getName()+". Closing + re-opening", e);
            close();
        }
    }

    // Retry code statement 3 times, recover from sqlite exceptions. Even if everything went well, close
    // the db in hope to mitigate concurrent access issues.
    private Object retry(Callable code) throws IOException {
        long maxAttempts = 3;
        while (maxAttempts-- > 0) {
            try {
                return code.call();
            } catch (SQLiteException e) {
                if (maxAttempts > 0) {
                    onSqliteError(e);
                } else {
                }
            } catch (java.io.InvalidClassException e) {
                // Old cache --> act as missing
                return null;
            } catch (Exception e) {
                throw new IOException(e);
            } finally {
                close();
            }
        }

        return null;
    }



    synchronized void store(LecturesController.WHAT what, String when, Office office) throws IOException {
        final String key  = when;
        final String create_date = computeKey(new GregorianCalendar());
        final long create_version = preference.getInt("version", -1);

        // build blob
        final byte[] blob;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos;
            oos = new ObjectOutputStream(bos);
            oos.writeObject(office);
            blob = bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // insert into the database
        final String sql = String.format(DB_TABLE_SET, what.toString());
        retry(new Callable() {
            @Override
            public Object call() throws Exception {
                SQLiteStatement stmt;
                stmt = getWritableDatabase().compileStatement(sql);
                stmt.bindString(1, key);
                stmt.bindBlob(2, blob);
                stmt.bindString(3, create_date);
                stmt.bindLong(4, create_version);

                stmt.execute();

                return null;
            }
        });
    }

    // cleaner helper method
    synchronized void truncateBefore(LecturesController.WHAT what, GregorianCalendar when) throws IOException {
        final String key = computeKey(when);
        final String table_name = what.toString();

        retry(new Callable() {
            @Override
            public Object call() throws Exception {
                SQLiteDatabase db = getWritableDatabase();
                db.delete(table_name, "`date` < ?", new String[] {key});
                return null;
            }
        });
    }

    // cast is not checked when decoding the blob but we where responsible for its creation so... dont care
    @SuppressWarnings("unchecked")
    synchronized Office load(LecturesController.WHAT what, GregorianCalendar when, GregorianCalendar minLoadDate, Long minLoadVersion) throws IOException {
        final String key  = computeKey(when);
        final String table_name = what.toString();
        final String min_create_date = computeKey(minLoadDate);
        final String min_create_version = String.valueOf(minLoadVersion);

        // load from db
        Log.i(TAG, "Trying to load lecture from cache create_date>="+min_create_date+" create_version>="+min_create_version);
        return (Office)retry(new Callable() {
            @Override
            public Object call() throws Exception {
                SQLiteDatabase db = getReadableDatabase();
                Cursor cur = db.query(
                        table_name,                                                // FROM
                        new String[]{"lectures", "create_date", "create_version"}, // SELECT
                        "`date`=? AND `create_date` >= ? AND create_version >= ?", // WHERE
                        new String[]{key, min_create_date, min_create_version},    // params
                        null, null, null, "1"                                      // GROUP BY, HAVING, ORDER, LIMIT
                );

                // If there is no result --> exit
                if(cur == null || cur.getCount() == 0) {
                    return null;
                }

                cur.moveToFirst();
                byte[] blob = cur.getBlob(0);

                Log.i(TAG, "Loaded lecture from cache create_date="+cur.getString(1)+" create_version="+cur.getLong(2));

                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(blob);
                    ObjectInputStream ois = new ObjectInputStream(bis);

                    return ois.readObject();
                } catch (ClassNotFoundException | IOException e) {
                    throw e;
                } finally {
                    cur.close();
                }
            }
        });
    }

    synchronized boolean has(LecturesController.WHAT what, GregorianCalendar when, GregorianCalendar minLoadDate, Long minLoadVersion) {
        String key  = computeKey(when);
        String min_create_date = computeKey(minLoadDate);
        String min_create_version = String.valueOf(minLoadVersion);

        // load from db
        Log.i(TAG, "Checking if lecture is in cache with create_date>="+min_create_date+" create_version>="+min_create_version);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = null;
        try {
            cur = db.query(
                    what.toString(),                                           // FROM
                    new String[]{"date"},                                      // SELECT
                    "`date`=? AND `create_date` >= ? AND create_version >= ?", // WHERE
                    new String[]{key, min_create_date, min_create_version},    // params
                    null, null, null, "1"                                      // GROUP BY, HAVING, ORDER, LIMIT
            );
            return cur != null && cur.getCount() > 0;
        } catch (SQLiteException e) {
            return false;
        } catch (IllegalStateException e) {
            Log.e(TAG, "Illegal state", e);
            return false;
        } finally {
            if (cur != null) cur.close();
            close();
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
        if(oldVersion <= 1) {
            Log.i(TAG, "Upgrading DB from version 1");
            createCache(db, LecturesController.WHAT.INFORMATIONS);
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
                throw e;
            } finally {
                db.endTransaction();
            }
        }

        if(oldVersion <= 3) {
            Log.i(TAG, "Upgrading DB from version 3");
            db.beginTransaction();
            try {
                db.execSQL("ALTER TABLE `lectures_metas` RENAME TO `"+LecturesController.WHAT.INFORMATIONS+"`");
                db.setTransactionSuccessful();
            } catch (Exception e) {
                throw e;
            } finally {
                db.endTransaction();
            }
        }
    }

}