package co.epitre.aelf_lectures.bible.data.bookmark

import android.content.Context
import co.epitre.aelf_lectures.bible.data.bible.BibleBookList
import java.util.concurrent.CopyOnWriteArrayList

enum class SortOrder {
    CREATION_DATE,
    BIBLE_ORDER
}

class BibleBookmarkRepository private constructor(context: Context) {

    interface OnBookmarkChangeListener {
        fun onBookmarkAdded(bookmark: BibleBookmark) {}
        fun onBookmarkUpdated(bookmark: BibleBookmark) {}
        fun onBookmarkDeleted(id: String) {}
        fun onBookmarksChanged() {}
    }

    private val dbHelper = BibleBookmarkHelper(context.applicationContext)
    private val listeners = CopyOnWriteArrayList<OnBookmarkChangeListener>()

    companion object {
        @Volatile
        private var INSTANCE: BibleBookmarkRepository? = null

        @JvmStatic
        fun getInstance(context: Context): BibleBookmarkRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BibleBookmarkRepository(context).also { INSTANCE = it }
            }
        }

        val BIBLE_ORDER_COMPARATOR = Comparator<BibleBookmark> { b1, b2 ->
            val bookList = BibleBookList.getInstance()
            val bookIdx1 = bookList.getBookIndex(b1.bookRef)
            val bookIdx2 = bookList.getBookIndex(b2.bookRef)

            if (bookIdx1 != bookIdx2) {
                return@Comparator bookIdx1.compareTo(bookIdx2)
            }

            val (chNum1, chRest1) = parseChapterSortKey(b1.chapterRef)
            val (chNum2, chRest2) = parseChapterSortKey(b2.chapterRef)

            if (chNum1 != chNum2) {
                return@Comparator chNum1.compareTo(chNum2)
            }
            if (chRest1 != chRest2) {
                return@Comparator chRest1.compareTo(chRest2)
            }

            if (b1.startVerse != b2.startVerse) {
                return@Comparator b1.startVerse.compareTo(b2.startVerse)
            }
            if (b1.startOffset != b2.startOffset) {
                return@Comparator b1.startOffset.compareTo(b2.startOffset)
            }
            if (b1.endVerse != b2.endVerse) {
                return@Comparator b1.endVerse.compareTo(b2.endVerse)
            }
            b1.endOffset.compareTo(b2.endOffset)
        }

        private fun parseChapterSortKey(chapterRef: String): Pair<Int, String> {
            val digits = chapterRef.takeWhile { it.isDigit() }
            val num = digits.toIntOrNull() ?: 0
            val rest = chapterRef.substring(digits.length)
            return Pair(num, rest)
        }
    }

    fun registerListener(listener: OnBookmarkChangeListener) {
        listeners.addIfAbsent(listener)
    }

    fun unregisterListener(listener: OnBookmarkChangeListener) {
        listeners.remove(listener)
    }

    fun saveBookmark(bookmark: BibleBookmark): Boolean {
        val existing = dbHelper.getBookmarkById(bookmark.id)
        val rowId = dbHelper.insertOrReplace(bookmark)
        val success = rowId != -1L
        if (success) {
            if (existing == null) {
                listeners.forEach { it.onBookmarkAdded(bookmark) }
            } else {
                listeners.forEach { it.onBookmarkUpdated(bookmark) }
            }
            listeners.forEach { it.onBookmarksChanged() }
        }
        return success
    }

    fun deleteBookmark(id: String): Boolean {
        val count = dbHelper.deleteBookmark(id)
        val success = count > 0
        if (success) {
            listeners.forEach {
                it.onBookmarkDeleted(id)
                it.onBookmarksChanged()
            }
        }
        return success
    }

    fun getBookmark(id: String): BibleBookmark? {
        return dbHelper.getBookmarkById(id)
    }

    fun getBookmarksForChapter(bookRef: String, chapterRef: String): List<BibleBookmark> {
        return dbHelper.getBookmarksForChapter(bookRef, chapterRef)
    }

    fun getAllBookmarks(sortOrder: SortOrder = SortOrder.CREATION_DATE): List<BibleBookmark> {
        val list = dbHelper.getAllBookmarks()
        return when (sortOrder) {
            SortOrder.CREATION_DATE -> list.sortedByDescending { it.createdAt }
            SortOrder.BIBLE_ORDER -> list.sortedWith(BIBLE_ORDER_COMPARATOR)
        }
    }

    fun clearAll() {
        dbHelper.deleteAll()
        listeners.forEach { it.onBookmarksChanged() }
    }
}
