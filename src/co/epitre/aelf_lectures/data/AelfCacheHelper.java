package co.epitre.aelf_lectures.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import android.util.Log;


/**
 * Internal cache manager (SQLite)
 */

final class AelfCacheHelper extends SQLiteOpenHelper {
	public static final String TAG = "AELFCacheHelper";
    private static final int DB_VERSION = 2;
    private static final String DB_NAME = "aelf_cache.db";

    private static final String DB_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS `%s` (date INTEGER PRIMARY KEY, lectures BLOB)";
    private static final String DB_TABLE_SET = "INSERT OR REPLACE INTO `%s` VALUES (?,?)";

    // TODO: prepare requests

    public AelfCacheHelper(Context context) {
    	super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Api
     */

    private long computeKey(GregorianCalendar when) {
    	return new GregorianCalendar(
    			when.get(Calendar.YEAR),
    			when.get(Calendar.MONTH),
    			when.get(Calendar.DAY_OF_MONTH)).getTimeInMillis();
    }

    public void store(LecturesController.WHAT what, GregorianCalendar when, List<LectureItem> lectures) {
    	long key  = computeKey(when);
    	byte[] blob = null;

    	// build blob
    	try {
    		ByteArrayOutputStream bos = new ByteArrayOutputStream();
    		ObjectOutputStream oos = null;
    		oos = new ObjectOutputStream(bos);
    		oos.writeObject(lectures);
    		blob = bos.toByteArray();
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    		// FIXME: error recovery
    	}

    	// insert into the database
    	String sql = String.format(DB_TABLE_SET, what);
    	SQLiteStatement stmt = getWritableDatabase().compileStatement(sql);
    	stmt.bindLong(1, key);
    	stmt.bindBlob(2, blob);
        stmt.execute();
    }

    // cleaner helper method
    public void truncateBefore(LecturesController.WHAT what, GregorianCalendar when) {
    	String key = Long.toString(computeKey(when));
    	SQLiteDatabase db = getWritableDatabase();
    	db.delete(what.toString(), "`date` < ?", new String[] {key});
    }

    // cast is not checked when decoding the blob but we where responsible for its creation so... dont care
    @SuppressWarnings("unchecked")
    public List<LectureItem> load(LecturesController.WHAT what, GregorianCalendar when) {
    	String key  = Long.toString(computeKey(when));
    	byte[] blob = null;

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
    		} catch (StreamCorruptedException e) {
    			throw new RuntimeException(e);
    			// FIXME: error recovery
    		} catch (IOException e) {
    			throw new RuntimeException(e);
    			// FIXME: error recovery
    		} catch (ClassNotFoundException e) {
    			throw new RuntimeException(e);
    			// FIXME: error recovery
    		}

    	} else {
    		return null;
    	}
    }

    /**
     * Internal logic
     */
    
    public void createCache(SQLiteDatabase db, LecturesController.WHAT what) {
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
    	if(oldVersion == 1) {
    		createCache(db, LecturesController.WHAT.METAS);
    	}
    }

}