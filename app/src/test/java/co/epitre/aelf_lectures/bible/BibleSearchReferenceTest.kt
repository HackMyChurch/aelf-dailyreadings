package co.epitre.aelf_lectures.bible

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class BibleSearchReferenceTest {

    @Test
    fun testParseBibleReferenceFormatting() {
        // Book only
        val targetMt = parseBibleReference("Mt")
        assertNotNull(targetMt)
        assertEquals("Matthieu", targetMt?.displayRef)

        // Book + Chapter
        val targetMt26 = parseBibleReference("Mt 26")
        assertNotNull(targetMt26)
        assertEquals("Matthieu chapitre 26", targetMt26?.displayRef)

        // Book + Chapter + Single Verse
        val targetMt26_36 = parseBibleReference("Mt 26, 36")
        assertNotNull(targetMt26_36)
        assertEquals("Matthieu chapitre 26 verset 36", targetMt26_36?.displayRef)

        // Book + Chapter + Verse Range
        val targetMt26_36_46 = parseBibleReference("Mt 26, 36-46")
        assertNotNull(targetMt26_36_46)
        assertEquals("Matthieu chapitre 26 versets 36 à 46", targetMt26_36_46?.displayRef)

        // Book + Chapter + Multiple Verse Ranges & Verses
        val targetMt26_complex = parseBibleReference("Mt 26, 1-3, 6, 8-10")
        assertNotNull(targetMt26_complex)
        assertEquals("Matthieu chapitre 26 versets 1 à 3, 6 et 8 à 10", targetMt26_complex?.displayRef)

        // Multi-chapter range without verses
        val targetMt26_27 = parseBibleReference("Mt 26-27")
        assertNotNull(targetMt26_27)
        assertEquals("Matthieu chapitres 26 à 27", targetMt26_27?.displayRef)

        // Multi-chapter range with verses
        val targetMtMultiVerse = parseBibleReference("Mt 26, 36 - 27, 5")
        assertNotNull(targetMtMultiVerse)
        assertEquals("Matthieu chapitre 26 verset 36 à chapitre 27 verset 5", targetMtMultiVerse?.displayRef)

        // Book with number prefix and range
        val target1Co = parseBibleReference("1Co 13, 4-8")
        assertNotNull(target1Co)
        assertEquals("1 Corinthiens chapitre 13 versets 4 à 8", target1Co?.displayRef)

        // Dot notation (e.g. Mc 1, 2-3.6)
        val targetMc = parseBibleReference("Mc 1, 2-3.6")
        assertNotNull(targetMc)
        assertEquals("Marc chapitre 1 versets 2 à 3 et 6", targetMc?.displayRef)

        // Colon notation (e.g. Jn 3:16)
        val targetJn = parseBibleReference("Jn 3:16")
        assertNotNull(targetJn)
        assertEquals("Jean chapitre 3 verset 16", targetJn?.displayRef)

        // Full book name search
        val targetGen = parseBibleReference("Genèse 1, 1-5")
        assertNotNull(targetGen)
        assertEquals("Genèse chapitre 1 versets 1 à 5", targetGen?.displayRef)
    }
}
