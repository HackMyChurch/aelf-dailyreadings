package co.epitre.aelf_lectures.bible.bookmark

import co.epitre.aelf_lectures.bible.data.bookmark.BibleBookmark
import co.epitre.aelf_lectures.bible.data.bookmark.BibleBookmarkRepository
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class BibleBookmarkTest {

    @Test
    fun testSerializationRoundtrip() {
        val original = BibleBookmark(
            id = "test-id-123",
            bookRef = "Gn",
            chapterRef = "1",
            startVerse = 1,
            startOffset = 5,
            endVerse = 3,
            endOffset = 20,
            exact = "Au commencement, Dieu créa le ciel et la terre.",
            prefix = "Text before ",
            suffix = " text after",
            createdAt = 1000000L,
            updatedAt = 2000000L
        )

        val jsonString = original.toJsonString()
        val restored = BibleBookmark.fromJsonString(jsonString)

        assertEquals(original.id, restored.id)
        assertEquals(original.bookRef, restored.bookRef)
        assertEquals(original.chapterRef, restored.chapterRef)
        assertEquals(original.startVerse, restored.startVerse)
        assertEquals(original.startOffset, restored.startOffset)
        assertEquals(original.endVerse, restored.endVerse)
        assertEquals(original.endOffset, restored.endOffset)
        assertEquals(original.exact, restored.exact)
        assertEquals(original.prefix, restored.prefix)
        assertEquals(original.suffix, restored.suffix)
        assertEquals(original.createdAt, restored.createdAt)
        assertEquals(original.updatedAt, restored.updatedAt)
    }

    @Test
    fun testJsNestedFormatDeserialization() {
        val jsJsonString = """
            {
              "id": "bookmark-456",
              "bookRef": "Ex",
              "chapterRef": "3",
              "range": {
                "startVerse": 2,
                "startOffset": 10,
                "endVerse": 4,
                "endOffset": 15
              },
              "textQuote": {
                "exact": "Le buisson était enflammé",
                "prefix": "et voici ",
                "suffix": " mais ne se consumait pas"
              }
            }
        """.trimIndent()

        val bookmark = BibleBookmark.fromJsonString(jsJsonString)

        assertEquals("bookmark-456", bookmark.id)
        assertEquals("Ex", bookmark.bookRef)
        assertEquals("3", bookmark.chapterRef)
        assertEquals(2, bookmark.startVerse)
        assertEquals(10, bookmark.startOffset)
        assertEquals(4, bookmark.endVerse)
        assertEquals(15, bookmark.endOffset)
        assertEquals("Le buisson était enflammé", bookmark.exact)
        assertEquals("et voici ", bookmark.prefix)
        assertEquals(" mais ne se consumait pas", bookmark.suffix)
    }

    @Test
    fun testDefaultValuesWhenMissing() {
        val json = JSONObject().apply {
            put("bookRef", "Ps")
            put("chapterRef", "23")
            put("exact", "Le Seigneur est mon berger")
        }

        val bookmark = BibleBookmark.fromJsonObject(json)

        assertNotNull(bookmark.id)
        assertEquals("Ps", bookmark.bookRef)
        assertEquals("23", bookmark.chapterRef)
        assertEquals("Le Seigneur est mon berger", bookmark.exact)
    }

    @Test
    fun testBibleOrderComparator() {
        val bmGenesis1 = BibleBookmark(
            bookRef = "Gn", chapterRef = "1", startVerse = 1, startOffset = 0,
            endVerse = 2, endOffset = 10, exact = "Genesis 1:1"
        )
        val bmGenesis1Later = BibleBookmark(
            bookRef = "Gn", chapterRef = "1", startVerse = 1, startOffset = 15,
            endVerse = 2, endOffset = 10, exact = "Genesis 1:1 later"
        )
        val bmExodus = BibleBookmark(
            bookRef = "Ex", chapterRef = "1", startVerse = 1, startOffset = 0,
            endVerse = 1, endOffset = 5, exact = "Exodus 1:1"
        )
        val bmMatthew = BibleBookmark(
            bookRef = "Mt", chapterRef = "1", startVerse = 1, startOffset = 0,
            endVerse = 1, endOffset = 5, exact = "Matthew 1:1"
        )
        val bmRevelation = BibleBookmark(
            bookRef = "Ap", chapterRef = "22", startVerse = 21, startOffset = 0,
            endVerse = 22, endOffset = 10, exact = "Revelation 22:21"
        )

        val list = listOf(bmRevelation, bmExodus, bmGenesis1Later, bmMatthew, bmGenesis1)
        val sorted = list.sortedWith(BibleBookmarkRepository.BIBLE_ORDER_COMPARATOR)

        assertEquals(bmGenesis1, sorted[0])
        assertEquals(bmGenesis1Later, sorted[1])
        assertEquals(bmExodus, sorted[2])
        assertEquals(bmMatthew, sorted[3])
        assertEquals(bmRevelation, sorted[4])
    }
}
