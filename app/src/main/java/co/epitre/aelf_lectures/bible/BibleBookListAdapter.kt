package co.epitre.aelf_lectures.bible

class BibleBookListAdapter {
    class OnBibleEntryClickEvent(
        @JvmField val mBiblePartId: Int,
        @JvmField val mBibleBookId: Int
    )
}
