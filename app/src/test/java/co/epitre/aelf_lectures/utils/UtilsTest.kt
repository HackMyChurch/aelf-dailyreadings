package co.epitre.aelf_lectures.utils

import co.epitre.aelf_lectures.bible.data.LectureReference
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.exp

class UtilsTest {

    @Test
    fun test1() = testGetLectureReferences(
        refToTest = "2,1-3.8-11",
        expected = listOf(
            LectureReference("2", "1", "3"),
            LectureReference("2", "8", "11"),
        )
    )

    @Test
    fun test2() = testGetLectureReferences(
        refToTest = "2,1-3,8-11",
        expected = listOf(
            LectureReference("2", "1", "3"),
            LectureReference("2", "8", "11"),
        )
    )

    @Test
    fun test3() = testGetLectureReferences(
        refToTest = "2",
        expected = listOf(
            LectureReference("2", null, null)
        )
    )

    @Test
    fun test4() = testGetLectureReferences(
        refToTest = "2-3",
        expected = listOf(
            LectureReference("2", null, null),
            LectureReference("3", null, null),
        )
    )

    @Test
    fun test5() = testGetLectureReferences(
        refToTest = "2-3,30-6",
        expected = listOf(
            LectureReference("2", "30", null),
            LectureReference("3", null, "6"),
        )
    )


    @Test
    fun test6() = testGetLectureReferences(
        refToTest = "2,1-3.8-11;4,13-17",
        expected = listOf(
            LectureReference("2", "1", "3"),
            LectureReference("2", "8", "11"),
            LectureReference("4", "13", "17"),
        )
    )


    @Test
    fun test7() = testGetLectureReferences(
        refToTest = "2-3,1-3.8-11,13-17",
        expected = listOf(
            LectureReference("2", "1", null),
            LectureReference("3", null, "3"),
            LectureReference("3", "8", "11"),
            LectureReference("3", "13", "17"),
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