package co.epitre.aelf_lectures.bible.biblebookfragment.content

import android.net.Uri
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

class BibleBookFragmentViewModel : ViewModel() {

    private val bibleController = BibleController.getInstance()

    private val _selectedChapterIndex = MutableStateFlow(-1)
    val selectedChapterIndex = _selectedChapterIndex.asStateFlow()

    private lateinit var _bibleBookEntry: BibleBookEntry
    private lateinit var _bibleBookChapters: List<BibleBookChapter>


    val bookRef
        get() = _bibleBookEntry.bookRef

    val selectedChapterRef
        get() = _bibleBookChapters.getOrNull(selectedChapterIndex.value)?.chapterRef ?: "1"

    val chapters
        get() = _bibleBookChapters

    val bookTitle
        get() = _bibleBookEntry.bookName


    fun initWithId(biblePartId: Int, bibleBookId: Int) {
        val biblePart = BibleBookList.getInstance().parts[biblePartId]
        _bibleBookEntry = biblePart.bibleBookEntries[bibleBookId]
        _bibleBookChapters = bibleController.getBookChapters(_bibleBookEntry.bookRef)
        _selectedChapterIndex.value = _bibleBookChapters.indexOfFirst {
            it.chapterRef == _bibleBookEntry.chapterRef
        }.coerceAtLeast(0)
    }

    fun initWithUri(uri: Uri) {
        val path = uri.path
        val chunks = path?.split("/")
        val bookRef = chunks?.getOrNull(2) ?: "Gn"
        val chapterRef = chunks?.getOrNull(3)?.uppercase() ?: "1"

        _bibleBookEntry = try {
            BibleBookList.getInstance().parts.flatMap {
                it.bibleBookEntries
            }.find {
                it.bookRef == bookRef
            } ?: throw IllegalStateException()

        } catch (e: Exception) {
            BibleBookList.getInstance().parts.first().bibleBookEntries.first()

        }
        _bibleBookChapters = bibleController.getBookChapters(_bibleBookEntry.bookRef)

        _selectedChapterIndex.value = _bibleBookChapters.indexOfFirst {
            it.chapterRef == chapterRef
        }.coerceAtLeast(0)
    }

    suspend fun getChapterVerses(chapterIndex: Int): List<BibleVerse> {
        return withContext(Dispatchers.IO) x@{
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