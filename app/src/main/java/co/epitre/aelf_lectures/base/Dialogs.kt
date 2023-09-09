package co.epitre.aelf_lectures.base

import android.app.AlertDialog
import android.content.Context

fun displayAboutDialog(context: Context) {
    AlertDialog.Builder(context)
        .setTitle(context.getString(co.epitre.aelf_lectures.R.string.dialog_about_title))
        .setMessage(context.getString(co.epitre.aelf_lectures.R.string.dialog_about_content))
        .setNeutralButton(android.R.string.ok, null)
        .show()
}

fun displayWhatsNewDialog(context: Context) {
    AlertDialog.Builder(context)
        .setTitle(context.getString(co.epitre.aelf_lectures.R.string.dialog_whats_new_title))
        .setMessage(context.getString(co.epitre.aelf_lectures.R.string.dialog_whats_new_content))
        .setNeutralButton(android.R.string.ok, null)
        .show()
}
