/**
 *
 */
package co.epitre.aelf_lectures.components;

import android.app.AlertDialog;
import android.app.Dialog;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import android.content.DialogInterface;
import android.os.Bundle;

import co.epitre.aelf_lectures.R;

/**
 * @author jean-tiare
 *
 */
public class MessageDialogFragment extends DialogFragment {
    private final String messageTitle;
    private final String messageText;

    public MessageDialogFragment(String title, String text) {
        this.messageTitle = title;
        this.messageText = text;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(this.messageTitle)
               .setMessage(this.messageText)
               .setPositiveButton(R.string.button_close, (dialog, id) -> {
                   // Stub
               })
               .create();
    }
}
