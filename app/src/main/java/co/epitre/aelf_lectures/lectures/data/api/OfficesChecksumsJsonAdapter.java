package co.epitre.aelf_lectures.lectures.data.api;

import com.squareup.moshi.FromJson;

import java.util.Map;

import co.epitre.aelf_lectures.lectures.data.office.OfficesChecksums;

public class OfficesChecksumsJsonAdapter {
    @FromJson
    OfficesChecksums OfficesChecksumsFromJson(Map<String, Map<String,String>> raw_checksums) {
        return new OfficesChecksums(raw_checksums);
    }
}
