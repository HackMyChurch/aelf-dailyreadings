package co.epitre.aelf_lectures.lectures.data.api;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.lang.reflect.Field;
import java.util.Map;

import co.epitre.aelf_lectures.lectures.data.office.OfficeChecksum;
import co.epitre.aelf_lectures.lectures.data.office.OfficesChecksums;

public class OfficesChecksumsJsonAdapter {
    @FromJson
    OfficesChecksums OfficesChecksumsFromJson(Map<String, Map<String, OfficeChecksum>> raw_checksums) {
        return new OfficesChecksums(raw_checksums);
    }

    @ToJson
    Map<String, Map<String, OfficeChecksum>> OfficesChecksumsToJson(OfficesChecksums officesChecksums) throws NoSuchFieldException, IllegalAccessException {
        // Get internal field via reflection to avoid exposing it
        Field privateField = OfficesChecksums.class.getDeclaredField("checksums");
        privateField.setAccessible(true);
        return (Map<String, Map<String, OfficeChecksum>>) privateField.get(officesChecksums);
    }
}
