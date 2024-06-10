package co.epitre.aelf_lectures.lectures.data.office;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import co.epitre.aelf_lectures.lectures.data.AelfDate;
import co.epitre.aelf_lectures.lectures.data.IsoDate;
import co.epitre.aelf_lectures.lectures.data.OfficeTypes;

class OfficesMetadataTest {
    @Test
    void nominal() {
        HashMap<String, OfficeMetadata> rawOfficeChecksums = new HashMap<>();
        IsoDate now = new IsoDate();
        rawOfficeChecksums.put("complies", new OfficeMetadata("0326067588ea4e14b3cea8d8139ad910b191d20d6e7477789bf1c76f5e5b1749", now));
        rawOfficeChecksums.put("informations", new OfficeMetadata("e18f20be64ea16794168c5545425f10ebceeff125c206be2c45de2b4038726a4", now));
        rawOfficeChecksums.put("messes", new OfficeMetadata("db8fbc6478911b24fc1369518e0406bee2ddda7bb30a7c5ac43d0cb19715b89e", now));
        rawOfficeChecksums.put("laudes", new OfficeMetadata("7883ffcb441ce6a943703312f8cd51eb157f5617436148bfff95565b56ceda1f", now));

        HashMap<String, Map<String, OfficeMetadata>> rawOfficesChecksums = new HashMap<>();
        rawOfficesChecksums.put("2024-05-14", rawOfficeChecksums);
        OfficesMetadata officesMetadata = new OfficesMetadata(rawOfficesChecksums);

        // Note: Months are 0-indexed
        AelfDate target_date = new AelfDate(2024, 4, 14);

        // Nominal tests
        assertEquals(
                "0326067588ea4e14b3cea8d8139ad910b191d20d6e7477789bf1c76f5e5b1749",
                officesMetadata.getOfficeChecksum(OfficeTypes.COMPLIES, target_date).checksum()
        );
        assertEquals(
                "db8fbc6478911b24fc1369518e0406bee2ddda7bb30a7c5ac43d0cb19715b89e",
                officesMetadata.getOfficeChecksum(OfficeTypes.MESSE, target_date).checksum()
        );
        assertEquals(
                "7883ffcb441ce6a943703312f8cd51eb157f5617436148bfff95565b56ceda1f",
                officesMetadata.getOfficeChecksum(OfficeTypes.LAUDES, target_date).checksum()
        );

        // Validate missing entries
        assertNull(officesMetadata.getOfficeChecksum(OfficeTypes.SEXTE, target_date));
        assertNull(officesMetadata.getOfficeChecksum(OfficeTypes.MESSE, new AelfDate(2024, 4, 15)));
    }
}