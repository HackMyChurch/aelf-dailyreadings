package co.epitre.aelf_lectures.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.preference.PreferenceManager
import co.epitre.aelf_lectures.R
import co.epitre.aelf_lectures.settings.SettingsActivity

interface ThemeColors {
    val textAnnotation: Color
    val textNeutral: Color
    val surfaceFilled: Color
    val onSurfaceFilled: Color
    val focusText: Color
    val tabText: Color
    val highlightBackground: Color
    val textSelectionHandle: Color
    val textSelectionBackground: Color
    val elevatedSurface: Color
    val onElevatedSurface: Color
}

val lightThemeColors = object : ThemeColors {
    override val textAnnotation: Color = ColorsFromTheme.lightAelfAccent
    override val textNeutral: Color = ColorsFromTheme.lightAelfLectureText
    override val surfaceFilled: Color = ColorsFromTheme.lightAelfAccent
    override val onSurfaceFilled: Color = ColorsFromTheme.white
    override val focusText: Color = ColorsFromTheme.lightAelfAccent
    override val tabText: Color = ColorsFromTheme.lightAelfLectureBackground
    override val highlightBackground: Color = ColorsFromTheme.lightAelfAccent.copy(alpha = .12f)
    override val textSelectionHandle: Color = ColorsFromTheme.lightAelfAccent
    override val textSelectionBackground: Color = ColorsFromTheme.CYAN
    override val elevatedSurface: Color = ColorsFromTheme.white
    override val onElevatedSurface: Color = ColorsFromTheme.darkAelfLectureBackground
}

val darkThemeColors = object : ThemeColors {
    override val textAnnotation: Color = ColorsFromTheme.darkAelfAccent
    override val textNeutral: Color = ColorsFromTheme.darkAelfLectureText
    override val surfaceFilled: Color = ColorsFromTheme.darkAelfLectureBackground
    override val onSurfaceFilled: Color = ColorsFromTheme.darkAelfAccent
    override val focusText: Color = ColorsFromTheme.darkAelfAccent
    override val tabText: Color = ColorsFromTheme.darkAelfAccent
    override val highlightBackground: Color = ColorsFromTheme.darkAelfAccent.copy(alpha = .12f)
    override val textSelectionHandle: Color = ColorsFromTheme.darkAelfAccent
    override val textSelectionBackground: Color = ColorsFromTheme.PETROL_BLUE
    override val elevatedSurface: Color = ColorsFromTheme.DARK_GRAY
    override val onElevatedSurface: Color = ColorsFromTheme.darkAelfLectureText
}


val colors: ThemeColors
    @Composable
    get() {
        val context = LocalContext.current

        val defaultMode = stringResource(R.string.pref_disp_night_mode_v2_def)

        val preferences =
            PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

        val desiredDisplayMode: String = preferences.getString(
            SettingsActivity.KEY_PREF_DISP_NIGHT_MODE_V2,
            defaultMode
        ) ?: defaultMode

        return when {
            desiredDisplayMode.equals("day") -> lightThemeColors
            desiredDisplayMode.equals("night") -> darkThemeColors
            else -> if (isSystemInDarkTheme()) darkThemeColors else lightThemeColors
        }
    }