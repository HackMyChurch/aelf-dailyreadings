package co.epitre.aelf_lectures.lectures.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IsoDateTest {
    @Test
    void from_date_string() {
        IsoDate date = new IsoDate("2024-05-20");

        assertTrue(date.isValid());

        assertEquals(2024, date.get(IsoDate.YEAR));
        assertEquals(4, date.get(IsoDate.MONTH));
        assertEquals(20, date.get(IsoDate.DAY_OF_MONTH));

        assertEquals(0, date.get(IsoDate.HOUR_OF_DAY));
        assertEquals(0, date.get(IsoDate.MINUTE));
        assertEquals(0, date.get(IsoDate.SECOND));

        assertEquals("2024-05-20T00:00:00", date.toString());
    }

    @Test
    void from_datetime_string() {
        IsoDate date = new IsoDate("2024-05-20T21:25:56.270580");

        assertTrue(date.isValid());

        assertEquals(2024, date.get(IsoDate.YEAR));
        assertEquals(4, date.get(IsoDate.MONTH));
        assertEquals(20, date.get(IsoDate.DAY_OF_MONTH));

        assertEquals(21, date.get(IsoDate.HOUR_OF_DAY));
        assertEquals(25, date.get(IsoDate.MINUTE));
        assertEquals(56, date.get(IsoDate.SECOND));

        assertEquals("2024-05-20T21:25:56", date.toString());
    }

    @Test
    void from_http_string() {
        IsoDate date = new IsoDate("Wed, 20 May 2024 21:25:56 GMT");

        assertTrue(date.isValid());

        assertEquals(2024, date.get(IsoDate.YEAR));
        assertEquals(4, date.get(IsoDate.MONTH));
        assertEquals(20, date.get(IsoDate.DAY_OF_MONTH));

        assertEquals(21, date.get(IsoDate.HOUR_OF_DAY));
        assertEquals(25, date.get(IsoDate.MINUTE));
        assertEquals(56, date.get(IsoDate.SECOND));

        assertEquals("2024-05-20T21:25:56", date.toString());
    }

    @Test
    void from_invalid_string() {
        IsoDate date = new IsoDate("I'm invalid");

        assertFalse(date.isValid());

        assertEquals(1970, date.get(IsoDate.YEAR));
        assertEquals(0, date.get(IsoDate.MONTH));
        assertEquals(1, date.get(IsoDate.DAY_OF_MONTH));

        assertEquals(0, date.get(IsoDate.HOUR_OF_DAY));
        assertEquals(0, date.get(IsoDate.MINUTE));
        assertEquals(0, date.get(IsoDate.SECOND));
    }
}
