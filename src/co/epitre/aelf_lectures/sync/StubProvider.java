package co.epitre.aelf_lectures.sync;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/*
 * Define an implementation of ContentProvider that stubs out
 * all methods
 * 
 * The real provider is in .data.LecturesController.
 * FIXME: merge them
 */
public class StubProvider extends ContentProvider {
	private static final String TAG = "SyncDB";
	
    /*
     * Always return true, indicating that the
     * provider loaded correctly.
     */
    @Override
    public boolean onCreate() {
    	Log.i(TAG, "Provider created");
        return true;
    }
    /*
     * Return an empty String for MIME type
     */
    @Override
	public String getType(Uri arg0) {
    	Log.i(TAG, "Provider: Type");
		return new String();
	}
    /*
     * query() always returns no results
     *
     */
    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
    	Log.i(TAG, "Provider: query");
        return null;
    }
    /*
     * insert() always returns null (no URI)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
    	Log.i(TAG, "Provider: insert");
        return null;
    }
    /*
     * delete() always returns "no rows affected" (0)
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
    	Log.i(TAG, "Provider: delete");
        return 0;
    }
    /*
     * update() always returns "no rows affected" (0)
     */
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
    	Log.i(TAG, "Provider: update");
        return 0;
    }
	
}