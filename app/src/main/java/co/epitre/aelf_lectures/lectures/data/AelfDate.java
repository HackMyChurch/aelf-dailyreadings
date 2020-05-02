package co.epitre.aelf_lectures.lectures.data;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * This class centralizes all helpers around dates like
 * - is it today ?
 * - is it this week ?
 * - is it tomorow ?
 * - is it ext sunday ?
 * - ...
 */

public class AelfDate extends GregorianCalendar {

    // Default constructor
    public AelfDate() {
        super();
    }

    // Date constructor
    public AelfDate(int year, int month, int day) {
        super(year, month, day);
    }

    // Timestamp constructor in milisec
    public AelfDate(long timestamp) {
        super();
        setTimeInMillis(timestamp);
    }

    //
    // String formatting
    //

    private String internalPrettyString(String dayFormat, String monthFormat, boolean withDeterminant) {
        if (isToday()) {
            return withDeterminant ? "d'aujourd'hui" : "aujourd'hui";
        }

        if (isTomorrow() && !isSunday()) {
            return withDeterminant ? "de demain" : "demain";
        }

        if (isYesterday() && !isSunday()) {
            return withDeterminant ? "d'hier" : "hier";
        }

        if (isWithin7NextDays()) {
            String intro = withDeterminant ? "de " : "";
            return intro + new SimpleDateFormat(dayFormat).format(getTimeInMillis()) + " prochain";
        }

        if (isWithin7PrevDays()) {
            String intro = withDeterminant ? "de " : "";
            return intro + new SimpleDateFormat(dayFormat).format(getTimeInMillis()) + " dernier";
        }

        if (isSameYear(new GregorianCalendar())) {
            String intro = withDeterminant ? "du " : "";
            return intro + new SimpleDateFormat(dayFormat+" d "+monthFormat).format(getTimeInMillis());
        }

        // Long version: be explicit
        String intro = withDeterminant ? "du " : "";
        return intro + new SimpleDateFormat(dayFormat+" d "+monthFormat+" y").format(getTimeInMillis());
    }

    public String toPrettyString() {
        return internalPrettyString("EEEE", "MMMM", true);
    }

    public String toShortPrettyString() {
        return internalPrettyString("E", "MMM", false);
    }

    public String toIsoString() {
        return new SimpleDateFormat("yyyy-MM-dd").format(getTimeInMillis());
    }

    public String toUrlString() {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(getTimeInMillis());
    }

    public String dayName() {
        return new SimpleDateFormat("EEEE", Locale.FRANCE).format(getTimeInMillis());
    }

    //
    // High level helpers
    //

    public boolean isToday() {
        GregorianCalendar today = new GregorianCalendar();
        return isSameDay(today);
    }

    public boolean isSunday() {
        return get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SUNDAY;
    }

    public boolean isYesterday() {
        GregorianCalendar yesterday = new GregorianCalendar();
        yesterday.add(GregorianCalendar.DAY_OF_YEAR, -1);
        return isSameDay(yesterday);
    }

    public boolean isTomorrow() {
        GregorianCalendar tomorrow = new GregorianCalendar();
        tomorrow.add(GregorianCalendar.DAY_OF_YEAR, 1);
        return isSameDay(tomorrow);
    }

    public boolean isWithin7PrevDays() {
        GregorianCalendar today = new GregorianCalendar();
        GregorianCalendar prevWeek = new GregorianCalendar();
        prevWeek.add(GregorianCalendar.DAY_OF_YEAR, -8);
        return compareTo(today) < 0 && compareTo(prevWeek) >= 0;
    }

    public boolean isWithin7NextDays() {
        // Get today's date, clear the time part
        GregorianCalendar today = new GregorianCalendar();
        today.set(GregorianCalendar.HOUR_OF_DAY, 0);
        today.set(GregorianCalendar.MINUTE, 0);
        today.set(GregorianCalendar.SECOND, 0);
        today.set(GregorianCalendar.MILLISECOND, 0);

        // Get next weeks's date, clear the time part
        GregorianCalendar nextWeek = new GregorianCalendar();
        nextWeek.add(GregorianCalendar.DAY_OF_YEAR, 8);

        // Compare
        return compareTo(today) >= 0 && compareTo(nextWeek) < 0;
    }

    //
    // Low level helper
    //

    public boolean isSameYear(GregorianCalendar other) {
        return (get(GregorianCalendar.ERA) == other.get(GregorianCalendar.ERA) &&
                get(GregorianCalendar.YEAR) == other.get(GregorianCalendar.YEAR));
    }

    public boolean isSameDay(GregorianCalendar other) {
        return (isSameYear(other) && get(GregorianCalendar.DAY_OF_YEAR) == other.get(GregorianCalendar.DAY_OF_YEAR));
    }

    // Will return the number of days between self and other. If other is in the future, will be positive
    public long dayBetween(GregorianCalendar other) {
        return (getTimeInMillis() - other.getTimeInMillis()) / (1000 * 60 * 60 * 24);
    }
}
