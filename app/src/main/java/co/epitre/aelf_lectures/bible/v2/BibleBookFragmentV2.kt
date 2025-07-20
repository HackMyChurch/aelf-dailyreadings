package co.epitre.aelf_lectures.bible.v2

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import co.epitre.aelf_lectures.LecturesActivity
import co.epitre.aelf_lectures.R
import co.epitre.aelf_lectures.bible.BibleFragment
import co.epitre.aelf_lectures.settings.SettingsActivity

class BibleBookFragmentV2 private constructor() : BibleFragment() {

    private val viewmodel by viewModels<BibleBookFragmentViewModel>()

    companion object {


        const val BIBLE_PART_ID: String = "biblePartId"
        const val BIBLE_BOOK_ID: String = "bibleBookId"
        const val BIBLE_URL: String = "bibleUrl"

        fun newInstance(biblePartId: Int, bibleBookId: Int): BibleFragment {
            /* comment this line with // to activate this block

          return BibleBookFragment().apply {
              arguments = Bundle().apply {
                  putInt(BIBLE_PART_ID, biblePartId)
                  putInt(BIBLE_BOOK_ID, bibleBookId)
              }
          }


        // */

            //      /* comment this line with // to activate this block
            return BibleBookFragmentV2().apply {
                arguments = Bundle().apply {
                    putInt(BIBLE_PART_ID, biblePartId)
                    putInt(BIBLE_BOOK_ID, bibleBookId)
                }
            }

            //      */

        }

        fun newInstance(uri: Uri): BibleFragment {
            return BibleBookFragmentV2().apply {
                arguments = Bundle().apply {
                    putString(BIBLE_URL, uri.toString())
                }
            }
        }
    }


    override fun getRoute(): String {
        return "/bible/${viewmodel.bookRef}/${viewmodel.selectedChapterRef}"
    }

    override fun getTitle(): String {
        return ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activity = activity as? LecturesActivity
        // Enable "home" button to get back to the menu
        if (parentFragmentManager.backStackEntryCount > 0) {
            activity?.setHomeButtonEnabled(true, View.OnClickListener { v: View? ->
                parentFragmentManager.popBackStack()
            })
        } else {
            activity?.setHomeButtonEnabled(false, null)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_section_bible_book_v2, container, false)
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)

        arguments?.let {
            if (it.containsKey(BIBLE_PART_ID) && it.containsKey(BIBLE_BOOK_ID)) {
                viewmodel.initWithId(it.getInt(BIBLE_PART_ID), it.getInt(BIBLE_BOOK_ID))
            } else if (it.containsKey(BIBLE_URL)) {
                viewmodel.initWithUri((it.getString(BIBLE_URL) ?: "").toUri())
            }
        }

        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            val preferences =
                PreferenceManager.getDefaultSharedPreferences(this.context.applicationContext)
            val initZoom = preferences.getInt(
                SettingsActivity.KEY_PREF_DISP_FONT_SIZE,
                100
            ) / 100f

            setContent {
                val selectedChapterIndex by viewmodel.selectedChapterIndex.collectAsStateWithLifecycle()
                var zoom by remember { mutableFloatStateOf(initZoom) }

                if (selectedChapterIndex != -1) {

                    (LocalActivity.current as? AppCompatActivity)?.supportActionBar?.title =
                        viewmodel.bookTitle

                    BibleBookFragmentScreenContent(
                        chapters = viewmodel.chapters,
                        selectedChapterIndex = selectedChapterIndex,
                        setSelectedChapterIndex = {
                            viewmodel.setSelectedChapterIndex(it)
                        },
                        verses = {
                            viewmodel.getChapterVerses(it)
                        },
                        chapter = {
                            viewmodel.getChapterAt(it)
                        },
                        zoom = zoom,
                        onPinchToZoom = { pZoom ->
                            zoom = (zoom * pZoom).coerceIn(1f, 7f)

                            val editor: SharedPreferences.Editor = preferences.edit()
                            editor.putInt(
                                SettingsActivity.KEY_PREF_DISP_FONT_SIZE,
                                (zoom * 100).toInt()
                            )
                            editor.apply()
                        }
                    )
                }
            }
        }
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}