package co.epitre.aelf_lectures;

import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.widget.DatePicker;

public class DatePickerFragment extends DialogFragment
	implements DatePickerDialog.OnDateSetListener {
	
	/* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface CalendarDialogListener {
        public void onCalendarDialogPicked(int year, int month, int day);
    }

    CalendarDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	// Verify that the host activity implements the callback interface
    	try {
    		mListener = (CalendarDialogListener) activity;
    	} catch (ClassCastException e) {
    		throw new ClassCastException(activity.toString()+ " must implement NoticeDialogListener");
    	}
    }

    
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		final Calendar c = Calendar.getInstance();
		
		// if we got an initial date --> load it
		if(args != null)
		{
			long timems = args.getLong("time");
			if(timems > 0) {
				c.setTimeInMillis(timems);
			}
		}
		
		// Use the current date as the default date in the picker
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);

		// Create a new instance of DatePickerDialog and return it
		DatePickerDialog dialog =  new DatePickerDialog(getActivity(), this, year, month, day);
		dialog.setCancelable(true);
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.button_cancel), (Message)null);
		return dialog;
	}

	public void onDateSet(DatePicker view, int year, int month, int day) {
		mListener.onCalendarDialogPicked(year, month, day);
	}
}
