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
public class AboutDialogFragment extends DialogFragment {
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        String message = getString(R.string.dialog_about_content);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_about_title)
               .setMessage(message)
               .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // Stub
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }

}
