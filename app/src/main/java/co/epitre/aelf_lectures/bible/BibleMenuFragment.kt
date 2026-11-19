package co.epitre.aelf_lectures.bible

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import co.epitre.aelf_lectures.LecturesActivity
import co.epitre.aelf_lectures.bible.data.bible.BibleBookList
import co.epitre.aelf_lectures.bible.ui.BibleMenuScreen

class BibleMenuFragment : BibleFragment() {

    private var initialBiblePartId: Int = 0
    private var currentPageState: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val activity = requireActivity() as LecturesActivity
        val actionBar: ActionBar? = activity.supportActionBar
        actionBar?.title = "Bible de la liturgie"

        setHasOptionsMenu(true)

        val args = arguments
        initialBiblePartId = args?.getInt(BIBLE_PART_ID, 0) ?: 0
        currentPageState = initialBiblePartId

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BibleMenuScreen(
                    initialBiblePartId = initialBiblePartId,
                    onPageChanged = { page ->
                        currentPageState = page
                    }
                )
            }
        }
    }

    override fun getRoute(): String? {
        val parts = BibleBookList.getInstance().parts
        if (currentPageState < 0 || currentPageState >= parts.size) {
            return null
        }
        return "/bible" + parts[currentPageState].route
    }

    override fun getTitle(): String? {
        val parts = BibleBookList.getInstance().parts
        if (currentPageState < 0 || currentPageState >= parts.size) {
            return null
        }
        val partTitle = parts[currentPageState].name
        return "Bible de la liturgie — $partTitle"
    }

    companion object {
        private const val BIBLE_PART_ID = "biblePartId"

        @JvmStatic
        fun newInstance(uri: Uri): BibleMenuFragment {
            val fragmentStr = uri.fragment
            var biblePartId = 0
            var found = false
            val parts = BibleBookList.getInstance().parts
            for (candidateBiblePart in parts) {
                if (candidateBiblePart.partRef == fragmentStr) {
                    found = true
                    break
                }
                biblePartId++
            }
            if (!found) {
                biblePartId = 0
            }
            return newInstance(biblePartId)
        }

        @JvmStatic
        fun newInstance(biblePartId: Int): BibleMenuFragment {
            val fragment = BibleMenuFragment()
            val args = Bundle()
            args.putInt(BIBLE_PART_ID, biblePartId)
            fragment.arguments = args
            return fragment
        }
    }
}
