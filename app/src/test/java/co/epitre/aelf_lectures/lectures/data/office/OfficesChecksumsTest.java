package co.epitre.aelf_lectures.lectures.data.office;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import co.epitre.aelf_lectures.lectures.data.AelfDate;
import co.epitre.aelf_lectures.lectures.data.OfficeTypes;

class OfficesChecksumsTest {
    @Test
    void nominal() {
        HashMap<String, String> rawOfficeChecksums = new HashMap<>();
        rawOfficeChecksums.put("complies", "0326067588ea4e14b3cea8d8139ad910b191d20d6e7477789bf1c76f5e5b1749");
        rawOfficeChecksums.put("informations", "e18f20be64ea16794168c5545425f10ebceeff125c206be2c45de2b4038726a4");
        rawOfficeChecksums.put("messes", "db8fbc6478911b24fc1369518e0406bee2ddda7bb30a7c5ac43d0cb19715b89e");
        rawOfficeChecksums.put("laudes", "7883ffcb441ce6a943703312f8cd51eb157f5617436148bfff95565b56ceda1f");

        HashMap<String, Map<String, String>> rawOfficesChecksums = new HashMap<>();
        rawOfficesChecksums.put("2024-05-14", rawOfficeChecksums);
        OfficesChecksums officesChecksums = new OfficesChecksums(rawOfficesChecksums);

        // Note: Months are 0-indexed
        AelfDate target_date = new AelfDate(2024, 4, 14);

        // Nominal tests
        assertEquals(
                "0326067588ea4e14b3cea8d8139ad910b191d20d6e7477789bf1c76f5e5b1749",
                officesChecksums.getOfficeChecksum(OfficeTypes.COMPLIES, target_date)
        );
        assertEquals(
                "e18f20be64ea16794168c5545425f10ebceeff125c206be2c45de2b4038726a4",
                officesChecksums.getOfficeChecksum(OfficeTypes.INFORMATIONS, target_date)
        );
        assertEquals(
                "db8fbc6478911b24fc1369518e0406bee2ddda7bb30a7c5ac43d0cb19715b89e",
                officesChecksums.getOfficeChecksum(OfficeTypes.MESSE, target_date)
        );
        assertEquals(
                "7883ffcb441ce6a943703312f8cd51eb157f5617436148bfff95565b56ceda1f",
                officesChecksums.getOfficeChecksum(OfficeTypes.LAUDES, target_date)
        );

        // Validate missing entries
        assertNull(officesChecksums.getOfficeChecksum(OfficeTypes.SEXTE, target_date));
        assertNull(officesChecksums.getOfficeChecksum(OfficeTypes.MESSE, new AelfDate(2024, 4, 15)));
    }
}