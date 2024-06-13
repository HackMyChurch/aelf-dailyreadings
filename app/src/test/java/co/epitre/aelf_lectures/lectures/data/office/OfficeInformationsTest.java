package co.epitre.aelf_lectures.lectures.data.office;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.util.List;

public class OfficeInformationsTest {
    @Test
    void empty() {
        OfficeInformations info = new OfficeInformations();

        // Test general field decoding
        assertEquals("Jour inconnu", info.getLiturgicalDay());
        assertEquals("Inconnue", info.getLiturgicalPeriod());
        assertNull(info.getLiturgicalYear());
        assertNull(info.getRomanPsalterWeek());

        // Test liturgical options
        List<OfficeLiturgyOption> liturgyOptions = info.getLiturgyOptions();
        assertNotNull(liturgyOptions);
        assertEquals(0, liturgyOptions.size());
    }

    @Test
    void psalterWeek() {
        OfficeInformations info = new OfficeInformations();

        // Field not set
        assertNull(info.getRomanPsalterWeek());

        // 1-4
        info.psalterWeek = 1;
        assertEquals("Ⅰ", info.getRomanPsalterWeek());
        info.psalterWeek = 2;
        assertEquals("Ⅱ", info.getRomanPsalterWeek());
        info.psalterWeek = 3;
        assertEquals("Ⅲ", info.getRomanPsalterWeek());
        info.psalterWeek = 4;
        assertEquals("Ⅳ", info.getRomanPsalterWeek());

        // Invalid values
        info.psalterWeek = -1;
        assertNull(info.getRomanPsalterWeek());
        info.psalterWeek = 0;
        assertNull(info.getRomanPsalterWeek());
        info.psalterWeek = 5;
        assertNull(info.getRomanPsalterWeek());

    }
}
