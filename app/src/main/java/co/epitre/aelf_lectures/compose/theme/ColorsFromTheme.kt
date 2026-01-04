package co.epitre.aelf_lectures.compose.theme

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import co.epitre.aelf_lectures.R

object ColorsFromTheme {

    lateinit var application: Application

    @JvmStatic
    fun init(application: Application) {
        this.application = application
    }

    val lightAelfAccent by lazy { application.color(R.color.light_aelf_accent) }
    val lightAelfLectureText by lazy { application.color(R.color.light_aelf_lecture_text) }
    val lightAelfLectureBackground by lazy { application.color(R.color.light_aelf_lecture_background) }
    val darkAelfLectureBackground: Color by lazy {
        application.color(R.color.dark_aelf_lecture_background)
    }
    val darkAelfLectureText by lazy { application.color(R.color.dark_aelf_lecture_text) }
    val darkAelfAccent by lazy { application.color(R.color.dark_aelf_accent) }
    val white by lazy { application.color(R.color.white) }

    private fun Context.color(color: Int) = Color(
        ContextCompat.getColor(
            this,
            color
        )
    )

    // Other Colors Primitives
    val CYAN = Color(0xFFA5CFD9)
    val PETROL_BLUE = Color(0xFF1E475A)
    val DARK_GRAY = Color(0xFF303030)
}