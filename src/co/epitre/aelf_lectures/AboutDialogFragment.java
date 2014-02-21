/**
 *
 */
package co.epitre.aelf_lectures;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

/**
 * @author jean-tiare
 *
 */
public class AboutDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
    	Context context = getActivity();
    	String versionName = "";
    	try {
			versionName = "v"+context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// Only drawback here is no version displayed in about. Minor anoyance
		}
    	String message = getString(R.string.dialog_about_content).replace("#VERSION#", versionName);
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
