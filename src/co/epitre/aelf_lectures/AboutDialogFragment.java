/**
 *
 */
package co.epitre.aelf_lectures;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * @author jean-tiare
 *
 */
public class AboutDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_about_title)
               .setMessage(R.string.dialog_about_content)
               .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // Stub
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }

}
