package co.epitre.aelf_lectures.lectures.data.cache;

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

import co.epitre.aelf_lectures.lectures.data.AelfDate;
import co.epitre.aelf_lectures.lectures.data.OfficeTypes;
import co.epitre.aelf_lectures.lectures.data.office.Office;

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
    private static final int DB_VERSION = 6;
    private static final String DB_NAME = "aelf_cache.db";
    private Context ctx;

    private static final String DB_CACHE_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS `lectures` (" +
            "date TEXT NOT NULL," +
            "office TEXT NOT NULL,"+
            "lectures BLOB," +
            "checksum TEXT," +
            "create_date TEXT," +
            "create_version INTEGER," +
            "PRIMARY KEY (date, office)" +
            ")";
    private static final String DB_CACHE_ENTRY_SET = "INSERT OR REPLACE INTO `lectures` VALUES (?,?,?,?,?,?)";

    // TODO: prepare requests

    public AelfCacheHelper(Context context) {
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

    synchronized public void store(OfficeTypes what, AelfDate when, Office office, String checksum, int ApiVersion) throws IOException {
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
        stmt.bindString(4, checksum);
        stmt.bindString(5, create_date);
        stmt.bindLong(6, ApiVersion);

        stmt.execute();
    }

    // cleaner helper method
    synchronized public void truncateBefore(AelfDate when) throws IOException {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("lectures", "`date` < ?", new String[] {when.toIsoString()});
    }

    // cast is not checked when decoding the blob but we where responsible for its creation so... dont care
    synchronized public CacheEntry load(OfficeTypes what, AelfDate when, Long minLoadVersion) throws IOException {
        final String min_create_version = String.valueOf(minLoadVersion);

        // load from db
        Log.i(TAG, "Trying to load lecture from cache create_version>="+min_create_version);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.query(
                "lectures",
                new String[]{"lectures", "checksum", "create_date", "create_version"},
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
            String checksum = cur.getString(1);
            Log.i(TAG, "Loaded lecture from cache create_date=" + cur.getString(2) + " create_version=" + cur.getLong(3) + " checksum=" + checksum);
            ByteArrayInputStream bis = new ByteArrayInputStream(blob);
            ObjectInputStream ois = new ObjectInputStream(bis);

            Office office = (Office) ois.readObject();
            return new CacheEntry(office, checksum);
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    synchronized public CacheEntries listCachedEntries(AelfDate since, int minLoadVersion) {
        final String min_create_version = String.valueOf(minLoadVersion);

        // load from db
        Log.i(TAG, "Listing cached entries date>="+since.toIsoString()+" create_version>="+min_create_version);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.query(
                "lectures",
                new String[]{"date", "office", "checksum"},
                "`date`>=? AND `create_version` >= ?",
                new String[]{since.toIsoString(), min_create_version},
                null, null, null
        );

        CacheEntries entries = new CacheEntries();
        try (cur) {
            while (cur.moveToNext()) {
                String what_str = cur.getString(1);
                String when_str = cur.getString(0);
                String checksum = cur.getString(2);
                entries.put(new CacheEntryIndex(what_str, when_str), new CacheEntry(null, checksum));
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