package co.epitre.aelf_lectures.utils

import co.epitre.aelf_lectures.bible.data.LectureReference
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UtilsTest {

    @Test
    fun testRangesSeparatedWithDot() = testGetLectureReferences(
        refToTest = "2,1-3.8-11",
        expected = listOf(
            LectureReference("2", "1", "3"),
            LectureReference("2", "8", "11"),
        )
    )

    @Test
    fun testRangesSeparatedWithComa() = testGetLectureReferences(
        refToTest = "2,1-3,8-11",
        expected = listOf(
            LectureReference("2", "1", "3"),
            LectureReference("2", "8", "11"),
        )
    )

    @Test
    fun testOneChapterOnly() = testGetLectureReferences(
        refToTest = "2",
        expected = listOf(
            LectureReference("2", null, null)
        )
    )

    @Test
    fun testTwoChaptersOnly() = testGetLectureReferences(
        refToTest = "2-3",
        expected = listOf(
            LectureReference("2", null, null),
            LectureReference("3", null, null),
        )
    )

    @Test
    fun testTwoChaptersAndOneRange() = testGetLectureReferences(
        refToTest = "2-3,30-6",
        expected = listOf(
            LectureReference("2", "30", null),
            LectureReference("3", null, "6"),
        )
    )


    @Test
    fun testOneChapterTwoRangesONeChapterOneRange() = testGetLectureReferences(
        refToTest = "2,1-3.8-11;4,13-17",
        expected = listOf(
            LectureReference("2", "1", "3"),
            LectureReference("2", "8", "11"),
            LectureReference("4", "13", "17"),
        )
    )


    @Test
    fun testTwoChaptersWithUnboundedRanges() = testGetLectureReferences(
        refToTest = "2-3,1-3.8-11,13-17",
        expected = listOf(
            LectureReference("2", "1", null),
            LectureReference("3", null, "3"),
            LectureReference("3", "8", "11"),
            LectureReference("3", "13", "17"),
        )
    )

    @Test
    fun testWithLetters() = testGetLectureReferences(
        refToTest = "9A,2-3",
        expected = listOf(
            LectureReference("9A", "2", "3"),
        )
    )

    @Test
    fun testWithLowercaseLetters() = testGetLectureReferences(
        refToTest = "9a,2-3;9b,5-6",
        expected = listOf(
            LectureReference("9A", "2", "3"),
            LectureReference("9B", "5", "6"),
        )
    )


    @Test
    fun testIgnorePartialVerses() = testGetLectureReferences(
        refToTest = "2,1b-3a,8a-11c",
        expected = listOf(
            LectureReference("2", "1", "3"),
            LectureReference("2", "8", "11"),
        )
    )


    private fun testGetLectureReferences(
        refToTest: String,
        expected: List<LectureReference>
    ) {
        val actual = Utils.getLectureReferences(refToTest)
        assertEquals(
            expected.size,
            actual.size,
            "Actual size is different from expected size. actual is $actual"
        )

        expected.forEachIndexed { i, it ->
            assert(
                it == actual[i],
            ) {
                "Expected $it but got ${actual[i]}"
            }
        }
    }
}