package co.epitre.aelf_lectures;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.widget.DatePicker;

/**
 * Overload stock DatePickerDialog to force date display in title in ICS
 * + refresh on date change
 * @author jean-tiare
 * 
 * source taken from https://github.com/android/platform_frameworks_base/blob/9066cfe9886ac131c34d59ed0e2d287b0e3c0087/core/java/android/app/DatePickerDialog.java
 *
 */
class SupportDatePickerDialog extends DatePickerDialog {
	private final Calendar mCalendar;
	private final java.text.DateFormat mDateFormat;
	private final String[] mWeekDays;
	
    /**
	* @param context The context the dialog is to run in.
	* @param callBack How the parent is notified that the date is set.
	* @param year The initial year of the dialog.
	* @param monthOfYear The initial month of the dialog.
	* @param dayOfMonth The initial day of the dialog.
	*/
    public SupportDatePickerDialog(Context context,
            OnDateSetListener callBack,
            int year,
            int monthOfYear,
            int dayOfMonth) {
        super(context, callBack, year, monthOfYear, dayOfMonth);
        
        DateFormatSymbols symbols = new DateFormatSymbols();
    	mWeekDays = symbols.getShortWeekdays();

    	mDateFormat = DateFormat.getMediumDateFormat(context);
    	mCalendar = Calendar.getInstance();
    	updateTitle(year, monthOfYear, dayOfMonth);
    }
	
	/**
	 * @param context The context the dialog is to run in.
	 * @param theme the theme to apply to this dialog
	 * @param callBack How the parent is notified that the date is set.
	 * @param year The initial year of the dialog.
	 * @param monthOfYear The initial month of the dialog.
	 * @param dayOfMonth The initial day of the dialog.
	 */
    public SupportDatePickerDialog(Context context,
    		int theme,
    		OnDateSetListener callBack,
    		int year,
    		int monthOfYear,
    		int dayOfMonth) {
    	super(context, theme, callBack, year, monthOfYear, dayOfMonth);
    	DateFormatSymbols symbols = new DateFormatSymbols();
    	mWeekDays = symbols.getShortWeekdays();

    	mDateFormat = DateFormat.getMediumDateFormat(context);
    	mCalendar = Calendar.getInstance();
    	updateTitle(year, monthOfYear, dayOfMonth);
    }

	
	private void updateTitle(int year, int month, int day) {
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, day);
        String weekday = mWeekDays[mCalendar.get(Calendar.DAY_OF_WEEK)];
        setTitle(weekday + " " + mDateFormat.format(mCalendar.getTime()));
    }
	
	public void onDateChanged(DatePicker view, int year,
            int month, int day) {
        updateTitle(year, month, day);
    }
}

public class DatePickerFragment extends DialogFragment
	implements DatePickerDialog.OnDateSetListener, DialogInterface.OnClickListener {
	
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
		DatePickerDialog dialog =  new SupportDatePickerDialog(getActivity(), this, year, month, day);
		dialog.setCancelable(true);
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.button_cancel), (Message)null);
		dialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.button_next_sunday), this);
		return dialog;
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(which == DialogInterface.BUTTON_NEUTRAL) {
			// find next sunday's date
			GregorianCalendar date = new GregorianCalendar();
			while (date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
				date.add(Calendar.DATE, +1);
			}
			
			// set date picker's date
			DatePickerDialog dateDialog = (DatePickerDialog)dialog;
	    	dateDialog.updateDate(
	    			date.get(Calendar.YEAR), 
	    			date.get(Calendar.MONTH),
	    			date.get(Calendar.DAY_OF_MONTH));
	    	
	    	// trigger listener
	    	mListener.onCalendarDialogPicked(
	    			date.get(Calendar.YEAR), 
	    			date.get(Calendar.MONTH),
	    			date.get(Calendar.DAY_OF_MONTH));
	    	
	    	// quit the dialog
	    	dialog.dismiss();
	    	
	    	// FIXME: avoid flickering
		}
	}

	public void onDateSet(DatePicker view, int year, int month, int day) {
		mListener.onCalendarDialogPicked(year, month, day);
	}
}
