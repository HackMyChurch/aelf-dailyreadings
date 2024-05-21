package co.epitre.aelf_lectures.lectures.data.api;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.lang.reflect.Field;
import java.util.Map;

import co.epitre.aelf_lectures.lectures.data.office.OfficeMetadata;
import co.epitre.aelf_lectures.lectures.data.office.OfficesMetadata;

public class OfficesChecksumsJsonAdapter {
    @FromJson
    OfficesMetadata OfficesChecksumsFromJson(Map<String, Map<String, OfficeMetadata>> raw_checksums) {
        return new OfficesMetadata(raw_checksums);
    }

    @ToJson
    Map<String, Map<String, OfficeMetadata>> OfficesChecksumsToJson(OfficesMetadata officesMetadata) throws NoSuchFieldException, IllegalAccessException {
        // Get internal field via reflection to avoid exposing it
        Field privateField = OfficesMetadata.class.getDeclaredField("checksums");
        privateField.setAccessible(true);
        return (Map<String, Map<String, OfficeMetadata>>) privateField.get(officesMetadata);
    }
}
