package co.epitre.aelf_lectures.lectures.data.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import co.epitre.aelf_lectures.lectures.data.AelfDate;
import co.epitre.aelf_lectures.lectures.data.IsoDate;
import co.epitre.aelf_lectures.lectures.data.OfficeTypes;
import co.epitre.aelf_lectures.lectures.data.office.OfficesMetadata;

class EpitreApiTest {
    @Test
    void offices_checksums_json_parser() throws IOException {
        String json_input = """
        {
            "2024-05-14": {
                "complies": {
                    "checksum": "0326067588ea4e14b3cea8d8139ad910b191d20d6e7477789bf1c76f5e5b1749",
                    "generation-date": "2024-05-20T18:07:54.386121"
                },
                "informations": {
                    "checksum": "e18f20be64ea16794168c5545425f10ebceeff125c206be2c45de2b4038726a4",
                    "generation-date": ""
                },
                "messes": {
                    "checksum": "db8fbc6478911b24fc1369518e0406bee2ddda7bb30a7c5ac43d0cb19715b89e",
                    "generation-date": ""
                },
                "laudes": {
                    "checksum": "7883ffcb441ce6a943703312f8cd51eb157f5617436148bfff95565b56ceda1f",
                    "generation-date": ""
                }
            }
        }""";

        EpitreApi api = new EpitreApi(null);
        OfficesMetadata officesMetadata = api.officesChecksumsJsonAdapter.fromJson(json_input);

        // Note: Months are 0-indexed
        AelfDate target_date = new AelfDate(2024, 4, 14);

        // Sanity test (deeper tests are already where they belong)
        assertEquals(
                "0326067588ea4e14b3cea8d8139ad910b191d20d6e7477789bf1c76f5e5b1749",
                officesMetadata.getOfficeChecksum(OfficeTypes.COMPLIES, target_date).checksum()
        );
        assertEquals(
                new IsoDate(2024, 4, 20, 18, 7, 54),
                officesMetadata.getOfficeChecksum(OfficeTypes.COMPLIES, target_date).generationDate()
        );
    }
}