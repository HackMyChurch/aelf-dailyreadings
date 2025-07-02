package co.epitre.aelf_lectures.bible.v2

import androidx.lifecycle.ViewModel
import co.epitre.aelf_lectures.bible.data.BibleBookChapter
import co.epitre.aelf_lectures.bible.data.BibleBookEntry
import co.epitre.aelf_lectures.bible.data.BibleBookList
import co.epitre.aelf_lectures.bible.data.BibleController
import co.epitre.aelf_lectures.bible.data.BibleVerse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class BibleBookFragmentViewModel : ViewModel() {

    private val bibleController = BibleController.getInstance()

    private val _selectedChapterIndex = MutableStateFlow(0)
    val selectedChapterIndex = _selectedChapterIndex.asStateFlow()

    private lateinit var _bibleBookEntry: BibleBookEntry
    private lateinit var _bibleBookChapters: List<BibleBookChapter>


    val bookRef
        get() = _bibleBookEntry.bookRef

    val selectedChapterRef
        get() = _bibleBookChapters.getOrNull(selectedChapterIndex.value) ?: "1"


    fun init(biblePartId: Int, bibleBookId: Int) {
        val biblePart = BibleBookList.getInstance().parts[biblePartId]
        _bibleBookEntry = biblePart.bibleBookEntries[bibleBookId]
        _bibleBookChapters = bibleController.getBookChapters(_bibleBookEntry.bookRef)
    }

    suspend fun getChapterVerses(chapterIndex: Int): List<BibleVerse> {
        return withContext(Dispatchers.IO) {
            bibleController.getBookChapterVerses(
                _bibleBookEntry.bookRef,
                _bibleBookChapters.getOrNull(chapterIndex)?.chapterRef
            ).map {
                BibleVerse(
                    it.ref, it.text.replace("\n", " ")
                )
            }
        }
    }

    fun getChapterAt(index: Int) = _bibleBookChapters.getOrNull(index)
            ?: BibleBookChapter("", "", "")

    fun setSelectedChapterIndex(index: Int) {
        _selectedChapterIndex.value = index
    }

}