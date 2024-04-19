package co.epitre.aelf_lectures.lectures.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteOpenHelper;
import org.sqlite.database.sqlite.SQLiteStatement;

/**
 * Internal cache manager (SQLite). There is a single 'lectures' table and one line per office.
 * Each line tracks the
 * - office date
 * - office name
 * - office content (serialized Office)
 * - when this office was loaded               --> used for server initiated invalidation
 * - which version of the application was used --> used for upgrade initiated invalidation
 */

public final class AelfCacheHelper extends SQLiteOpenHelper {
    private static final String TAG = "AELFCacheHelper";
    private static final int DB_VERSION = 5;
    private static final String DB_NAME = "aelf_cache.db";
    private Context ctx;

    private static final String DB_CACHE_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS `lectures` (" +
            "date TEXT NOT NULL," +
            "office TEXT NOT NULL,"+
            "lectures BLOB," +
            "create_date TEXT," +
            "create_version INTEGER," +
            "PRIMARY KEY (date, office)" +
            ")";
    private static final String DB_CACHE_ENTRY_SET = "INSERT OR REPLACE INTO `lectures` VALUES (?,?,?,?,?)";

    // TODO: prepare requests

    AelfCacheHelper(Context context) {
        super(context, context.getDatabasePath(DB_NAME).getAbsolutePath(), null, DB_VERSION);
        File dbDir = context.getDatabasePath(DB_NAME).getParentFile();
        if (dbDir != null) {
            dbDir.mkdirs();
        }
        ctx = context;
    }

    /**
     * Api
     */

    public void dropDatabase() {
        this.ctx.deleteDatabase(DB_NAME);
    }

    public long getDatabaseSize() {
        return this.ctx.getDatabasePath(DB_NAME).length();
    }

    synchronized void store(LecturesController.WHAT what, AelfDate when, Office office, int ApiVersion) throws IOException {
        final String create_date = (new AelfDate()).toIsoString();

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
        SQLiteStatement stmt;
        stmt = getWritableDatabase().compileStatement(DB_CACHE_ENTRY_SET);
        stmt.bindString(1, when.toIsoString());
        stmt.bindString(2, what.toString());
        stmt.bindBlob(3, blob);
        stmt.bindString(4, create_date);
        stmt.bindLong(5, ApiVersion);

        stmt.execute();
    }

    // cleaner helper method
    synchronized void truncateBefore(AelfDate when) throws IOException {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("lectures", "`date` < ?", new String[] {when.toIsoString()});
    }

    // cast is not checked when decoding the blob but we where responsible for its creation so... dont care
    @SuppressWarnings("unchecked")
    synchronized Office load(LecturesController.WHAT what, AelfDate when, Long minLoadVersion) throws IOException {
        final String min_create_version = String.valueOf(minLoadVersion);

        // load from db
        Log.i(TAG, "Trying to load lecture from cache create_version>="+min_create_version);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.query(
                "lectures",
                new String[]{"lectures", "create_date", "create_version"},
                "`date`=? AND `office`=? AND create_version >= ?",
                new String[]{when.toIsoString(), what.toString(), min_create_version},
                null, null, null, "1"
        );

        // If there is no result --> exit

        try (cur) {
            if (cur == null || cur.getCount() == 0) {
                return null;
            }
            cur.moveToFirst();
            byte[] blob = cur.getBlob(0);
            Log.i(TAG, "Loaded lecture from cache create_date=" + cur.getString(1) + " create_version=" + cur.getLong(2));
            ByteArrayInputStream bis = new ByteArrayInputStream(blob);
            ObjectInputStream ois = new ObjectInputStream(bis);

            return (Office) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    synchronized CacheEntryIndexes listCachedEntries(AelfDate since, int minLoadVersion) {
        final String min_create_version = String.valueOf(minLoadVersion);

        // load from db
        Log.i(TAG, "Listing cached entries date>="+since.toIsoString()+" create_version>="+min_create_version);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.query(
                "lectures",
                new String[]{"date", "office"},
                "`date`>=? AND `create_version` >= ?",
                new String[]{since.toIsoString(), min_create_version},
                null, null, null
        );

        CacheEntryIndexes entries = new CacheEntryIndexes();
        try (cur) {
            while (cur.moveToNext()) {
                String what_str = cur.getString(1);
                String when_str = cur.getString(0);
                entries.add(new CacheEntryIndex(what_str, when_str));
            }
        }

        return entries;
    }

    /**
     * Internal logic
     */

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_CACHE_TABLE_CREATE);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        this.dropDatabase(); // This is a cache, we can re-build it
    }
}