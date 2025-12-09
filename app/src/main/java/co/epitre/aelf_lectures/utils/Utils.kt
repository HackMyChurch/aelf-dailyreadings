package co.epitre.aelf_lectures.utils

import co.epitre.aelf_lectures.bible.data.LectureReference
import java.util.Locale

object Utils {
    @JvmStatic
    fun Capitalize(str: String): String {
        return str.substring(0, 1).uppercase(Locale.getDefault()) + str.substring(1)
    }

    fun getLectureReferences(ref: String): List<LectureReference> {

        val references = mutableListOf<LectureReference>()
        val parts = ref.split(";")

        parts.forEach { part ->
            val refs = part.split(",", ".")

            // Chapter
            val chapterPart = refs.getOrNull(0)?.uppercase() ?: "1"
            val chapterStart = chapterPart.split("-").getOrNull(0) ?: "1"
            val chapterEnd = chapterPart.split("-").getOrNull(1) ?: chapterStart

            if (refs.size == 1) {
                references += LectureReference(
                    chapter = chapterStart,
                    verseStart = null,
                    verseEnd = null
                )
                if (chapterStart != chapterEnd) {
                    references += LectureReference(
                        chapter = chapterEnd,
                        verseStart = null,
                        verseEnd = null
                    )
                }
            }

            refs.drop(1).forEachIndexed { i, it ->
                when (i) {
                    0 -> {
                        if (chapterStart != chapterEnd) {
                            references += LectureReference(
                                chapter = chapterStart,
                                verseStart = it.split("-").getOrNull(0)?.digitsOnly() ?: "1",
                                verseEnd = null
                            )
                            references += LectureReference(
                                chapter = chapterEnd,
                                verseStart = null,
                                verseEnd = it.split("-").getOrNull(1)?.digitsOnly() ?: "1"
                            )
                        } else {
                            val verseStart = it.split("-").getOrNull(0) ?: "1"
                            val verseEnd = it.split("-").getOrNull(1) ?: verseStart

                            references += LectureReference(
                                chapter = chapterStart,
                                verseStart = verseStart.digitsOnly(),
                                verseEnd = verseEnd.digitsOnly()
                            )
                        }

                    }

                    1 -> {
                        val verseStart = it.split("-").getOrNull(0) ?: "1"
                        val verseEnd = it.split("-").getOrNull(1) ?: verseStart

                        references += LectureReference(
                            chapter = chapterEnd,
                            verseStart = verseStart.digitsOnly(),
                            verseEnd = verseEnd.digitsOnly()
                        )
                    }

                    else -> {
                        val verseStart = it.split("-").getOrNull(0) ?: "1"
                        val verseEnd = it.split("-").getOrNull(1) ?: verseStart

                        references += LectureReference(
                            chapter = chapterEnd,
                            verseStart = verseStart.digitsOnly(),
                            verseEnd = verseEnd.digitsOnly()
                        )
                    }
                }
            }
        }

        return references
    }

    fun String.safeToInt() = replace(Regex("\\D"), "").toIntOrNull()

    fun List<LectureReference>.containsVerse(
        chapter: String,
        verse: String,
    ): Boolean {

        val intChapter = chapter.safeToInt() ?: return false
        val intVerse = verse.safeToInt() ?: return false

        this.forEach {
            val intRefChapter = it.chapter.safeToInt() ?: return false
            val intRefVerseStart = it.verseStart?.safeToInt() ?: Int.MIN_VALUE
            val intRefVerseEnd = it.verseEnd?.safeToInt() ?: Int.MAX_VALUE

            1 in 0..4
            if (intChapter == intRefChapter && intVerse in intRefVerseStart..intRefVerseEnd) {
                return true
            }
        }

        return false
    }
}

fun String.digitsOnly() = this.replace(Regex("\\D"), "")
