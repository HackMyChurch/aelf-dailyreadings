package co.epitre.aelf_lectures.bible.data.bookmark

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONException

class BibleBookmarkWebBridge @JvmOverloads constructor(
    context: Context,
    private val repository: BibleBookmarkRepository = BibleBookmarkRepository.getInstance(context),
) {
    companion object {
        private const val TAG = "BibleBookmarkBridge"
        const val INTERFACE_NAME = "AndroidBridge"
    }

    @Volatile
    var isEditingExistingBookmark: Boolean = false
        private set

    @Volatile
    var currentEditingBookmarkId: String = ""
        private set

    @JavascriptInterface
    fun onSelectionStateChanged(isEditingExisting: Boolean, bookmarkId: String?) {
        Log.d(TAG, "onSelectionStateChanged: isEditingExisting=$isEditingExisting, bookmarkId=$bookmarkId")
        this.isEditingExistingBookmark = isEditingExisting
        this.currentEditingBookmarkId = bookmarkId ?: ""
    }

    fun clearSelectionState() {
        this.isEditingExistingBookmark = false
        this.currentEditingBookmarkId = ""
    }

    @JavascriptInterface
    fun createBookmark(jsonString: String): String {
        Log.d(TAG, "createBookmark: $jsonString")
        return try {
            val bookmark = BibleBookmark.fromJsonString(jsonString)
            val now = System.currentTimeMillis()
            val createdAt = if (bookmark.createdAt == 0L) now else bookmark.createdAt
            val updated = bookmark.copy(createdAt = createdAt, updatedAt = now)
            val saved = repository.saveBookmark(updated)
            if (saved) updated.id else ""
        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing bookmark JSON: $jsonString", e)
            ""
        }
    }

    @JavascriptInterface
    fun updateBookmark(jsonString: String): Boolean {
        Log.d(TAG, "updateBookmark: $jsonString")
        return try {
            val bookmark = BibleBookmark.fromJsonString(jsonString)
            val updated = bookmark.copy(updatedAt = System.currentTimeMillis())
            repository.saveBookmark(updated)
        } catch (e: JSONException) {
            Log.e(TAG, "Error updating bookmark JSON: $jsonString", e)
            false
        }
    }

    @JavascriptInterface
    fun deleteBookmark(id: String): Boolean {
        Log.d(TAG, "deleteBookmark: $id")
        return repository.deleteBookmark(id)
    }

    @JavascriptInterface
    fun getBookmarksForChapter(bookRef: String, chapterRef: String): String {
        val list = repository.getBookmarksForChapter(bookRef, chapterRef)
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it.toJsonObject()) }
        return jsonArray.toString()
    }

    @JavascriptInterface
    fun logBookmark(jsonString: String) {
        Log.d(TAG, "logBookmark: $jsonString")
    }

    @JavascriptInterface
    fun logMessage(message: String) {
        Log.d(TAG, "logMessage: $message")
    }
}
