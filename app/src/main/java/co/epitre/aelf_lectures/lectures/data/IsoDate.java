package co.epitre.aelf_lectures.lectures.data;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class IsoDate extends GregorianCalendar {
    private boolean isValid = true;
    private final DateFormat[] dateFormats = new DateFormat[]{
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT),
            new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT),
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US), // HTTP dates
    };

    private void init() {
        // Normalize all to UTC
        TimeZone UTC = TimeZone.getTimeZone("UTC");
        this.setTimeZone(UTC);
        for (DateFormat dateFormat: dateFormats) {
            dateFormat.setTimeZone(UTC);
            dateFormat.getCalendar().setTimeZone(UTC);
        }
    }

    public IsoDate() {
        super();
        init();
    }

    public IsoDate(String isoString) {
        init();

        // Attempt to parse the date as a full date and fallback on day only
        Date parsed = null;
        for (DateFormat dateFormat: dateFormats) {
            // Attempt this parser
            try {
                parsed = dateFormat.parse(isoString);
                break;
            } catch (ParseException ignored) {}
        }

        // If parse failed, set the date to 0
        if (parsed != null) {
            this.setTimeInMillis(parsed.getTime());
        } else {
            this.isValid = false;
            this.setTimeInMillis(0);
        }
    }

    public IsoDate(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second) {
        super(year, month, dayOfMonth, hourOfDay, minute, second);
        init();
    }

    @NonNull
    public String toString() {
        return dateFormats[0].format(getTime());
    }

    public boolean isValid() {
        return isValid;
    }
}
