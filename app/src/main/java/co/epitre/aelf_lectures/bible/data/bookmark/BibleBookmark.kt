package co.epitre.aelf_lectures.bible.data.bookmark

import org.json.JSONObject
import java.util.UUID

data class BibleBookmark(
    val id: String = UUID.randomUUID().toString(),
    val bookRef: String,
    val chapterRef: String,
    val startVerse: Int,
    val startOffset: Int,
    val endVerse: Int,
    val endOffset: Int,
    val exact: String,
    val prefix: String = "",
    val suffix: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun toJsonObject(): JSONObject {
        val rangeObj = JSONObject().apply {
            put("startVerse", startVerse)
            put("startOffset", startOffset)
            put("endVerse", endVerse)
            put("endOffset", endOffset)
        }
        val textQuoteObj = JSONObject().apply {
            put("exact", exact)
            put("prefix", prefix)
            put("suffix", suffix)
        }
        return JSONObject().apply {
            put("id", id)
            put("bookRef", bookRef)
            put("chapterRef", chapterRef)
            put("range", rangeObj)
            put("textQuote", textQuoteObj)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
        }
    }

    fun toJsonString(): String = toJsonObject().toString()

    companion object {
        @JvmStatic
        fun fromJsonObject(json: JSONObject): BibleBookmark {
            val id = json.optString("id").ifEmpty { UUID.randomUUID().toString() }
            val bookRef = json.optString("bookRef", "")
            val chapterRef = json.optString("chapterRef", "")
            val createdAt = if (json.has("createdAt")) json.optLong("createdAt", System.currentTimeMillis()) else System.currentTimeMillis()
            val updatedAt = if (json.has("updatedAt")) json.optLong("updatedAt", System.currentTimeMillis()) else System.currentTimeMillis()

            val (startVerse, startOffset, endVerse, endOffset) = if (json.has("range") && !json.isNull("range")) {
                val range = json.getJSONObject("range")
                Quadruple(
                    range.optInt("startVerse", 0),
                    range.optInt("startOffset", 0),
                    range.optInt("endVerse", 0),
                    range.optInt("endOffset", 0),
                )
            } else {
                Quadruple(
                    json.optInt("startVerse", 0),
                    json.optInt("startOffset", 0),
                    json.optInt("endVerse", 0),
                    json.optInt("endOffset", 0)
                )
            }

            val (exact, prefix, suffix) = if (json.has("textQuote") && !json.isNull("textQuote")) {
                val quote = json.getJSONObject("textQuote")
                Triple(
                    quote.optString("exact", ""),
                    quote.optString("prefix", ""),
                    quote.optString("suffix", "")
                )
            } else {
                Triple(
                    json.optString("exact", ""),
                    json.optString("prefix", ""),
                    json.optString("suffix", "")
                )
            }

            return BibleBookmark(
                id = id,
                bookRef = bookRef,
                chapterRef = chapterRef,
                startVerse = startVerse,
                startOffset = startOffset,
                endVerse = endVerse,
                endOffset = endOffset,
                exact = exact,
                prefix = prefix,
                suffix = suffix,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }

        @JvmStatic
        fun fromJsonString(jsonString: String): BibleBookmark {
            return fromJsonObject(JSONObject(jsonString))
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
