package co.epitre.aelf_lectures.lectures.data.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import co.epitre.aelf_lectures.lectures.data.IsoDate;
import co.epitre.aelf_lectures.lectures.data.office.OfficeMetadata;

public class OfficeMetadataJsonAdapterTest {
    @Test
    void deserialize() throws IOException {
        String json_input = """
        {
            "checksum": "0326067588ea4e14b3cea8d8139ad910b191d20d6e7477789bf1c76f5e5b1749",
            "generation-date": "2024-05-20T18:07:54.386121"
        }""";

        Moshi moshi = new Moshi.Builder()
                .add(new OfficeChecksumJsonAdapter())
                .build();
        JsonAdapter<OfficeMetadata> adapter = moshi.adapter(OfficeMetadata.class);
        OfficeMetadata officeMetadata = adapter.fromJson(json_input);

        assertNotNull(officeMetadata);

        assertEquals(
                "0326067588ea4e14b3cea8d8139ad910b191d20d6e7477789bf1c76f5e5b1749",
                officeMetadata.checksum()
        );

        assertEquals(
                new IsoDate(2024, 4, 20, 18, 7, 54),
                officeMetadata.generationDate()
        );
    }
}
