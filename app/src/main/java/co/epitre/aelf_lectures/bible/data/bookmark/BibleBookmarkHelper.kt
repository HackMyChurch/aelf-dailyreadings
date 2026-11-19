package co.epitre.aelf_lectures.bible.data.bookmark

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import org.sqlite.database.sqlite.SQLiteDatabase
import org.sqlite.database.sqlite.SQLiteOpenHelper

class BibleBookmarkHelper(context: Context) : SQLiteOpenHelper(
    context,
    context.getDatabasePath(DB_NAME).absolutePath,
    null,
    DB_VERSION
) {
    companion object {
        const val DB_NAME = "bible_bookmarks.db"
        const val DB_VERSION = 1

        const val TABLE_BOOKMARKS = "bible_bookmarks"

        const val COL_ID = "id"
        const val COL_BOOK_REF = "book_ref"
        const val COL_CHAPTER_REF = "chapter_ref"
        const val COL_START_VERSE = "start_verse"
        const val COL_START_OFFSET = "start_offset"
        const val COL_END_VERSE = "end_verse"
        const val COL_END_OFFSET = "end_offset"
        const val COL_EXACT = "exact"
        const val COL_PREFIX = "prefix"
        const val COL_SUFFIX = "suffix"
        const val COL_CREATED_AT = "created_at"
        const val COL_UPDATED_AT = "updated_at"

        private const val CREATE_TABLE_BOOKMARKS = """
            CREATE TABLE IF NOT EXISTS `$TABLE_BOOKMARKS` (
                `$COL_ID` TEXT PRIMARY KEY NOT NULL,
                `$COL_BOOK_REF` TEXT NOT NULL,
                `$COL_CHAPTER_REF` TEXT NOT NULL,
                `$COL_START_VERSE` INTEGER NOT NULL,
                `$COL_START_OFFSET` INTEGER NOT NULL,
                `$COL_END_VERSE` INTEGER NOT NULL,
                `$COL_END_OFFSET` INTEGER NOT NULL,
                `$COL_EXACT` TEXT NOT NULL,
                `$COL_PREFIX` TEXT NOT NULL DEFAULT '',
                `$COL_SUFFIX` TEXT NOT NULL DEFAULT '',
                `$COL_CREATED_AT` INTEGER NOT NULL,
                `$COL_UPDATED_AT` INTEGER NOT NULL
            );
        """

        private const val CREATE_INDEX_BOOK_CHAPTER = """
            CREATE INDEX IF NOT EXISTS `idx_bible_bookmarks_book_chapter`
            ON `$TABLE_BOOKMARKS` (`$COL_BOOK_REF`, `$COL_CHAPTER_REF`);
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_BOOKMARKS)
        db.execSQL(CREATE_INDEX_BOOK_CHAPTER)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database migrations here when DB_VERSION is incremented
    }

    fun insertOrReplace(bookmark: BibleBookmark): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, bookmark.id)
            put(COL_BOOK_REF, bookmark.bookRef)
            put(COL_CHAPTER_REF, bookmark.chapterRef)
            put(COL_START_VERSE, bookmark.startVerse)
            put(COL_START_OFFSET, bookmark.startOffset)
            put(COL_END_VERSE, bookmark.endVerse)
            put(COL_END_OFFSET, bookmark.endOffset)
            put(COL_EXACT, bookmark.exact)
            put(COL_PREFIX, bookmark.prefix)
            put(COL_SUFFIX, bookmark.suffix)
            put(COL_CREATED_AT, bookmark.createdAt)
            put(COL_UPDATED_AT, bookmark.updatedAt)
        }
        return db.insertWithOnConflict(
            TABLE_BOOKMARKS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun deleteBookmark(id: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_BOOKMARKS, "$COL_ID = ?", arrayOf(id))
    }

    fun getBookmarkById(id: String): BibleBookmark? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BOOKMARKS,
            null,
            "$COL_ID = ?",
            arrayOf(id),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                cursorToBookmark(it)
            } else null
        }
    }

    fun getBookmarksForChapter(bookRef: String, chapterRef: String): List<BibleBookmark> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BOOKMARKS,
            null,
            "$COL_BOOK_REF = ? AND $COL_CHAPTER_REF = ?",
            arrayOf(bookRef, chapterRef),
            null,
            null,
            "$COL_START_VERSE ASC, $COL_START_OFFSET ASC"
        )
        val list = mutableListOf<BibleBookmark>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToBookmark(it))
            }
        }
        return list
    }

    fun getAllBookmarks(): List<BibleBookmark> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BOOKMARKS,
            null,
            null,
            null,
            null,
            null,
            "$COL_CREATED_AT DESC"
        )
        val list = mutableListOf<BibleBookmark>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToBookmark(it))
            }
        }
        return list
    }

    fun deleteAll(): Int {
        val db = writableDatabase
        return db.delete(TABLE_BOOKMARKS, null, null)
    }

    private fun cursorToBookmark(cursor: Cursor): BibleBookmark {
        return BibleBookmark(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
            bookRef = cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOK_REF)),
            chapterRef = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHAPTER_REF)),
            startVerse = cursor.getInt(cursor.getColumnIndexOrThrow(COL_START_VERSE)),
            startOffset = cursor.getInt(cursor.getColumnIndexOrThrow(COL_START_OFFSET)),
            endVerse = cursor.getInt(cursor.getColumnIndexOrThrow(COL_END_VERSE)),
            endOffset = cursor.getInt(cursor.getColumnIndexOrThrow(COL_END_OFFSET)),
            exact = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXACT)),
            prefix = cursor.getString(cursor.getColumnIndexOrThrow(COL_PREFIX)),
            suffix = cursor.getString(cursor.getColumnIndexOrThrow(COL_SUFFIX)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
        )
    }
}
