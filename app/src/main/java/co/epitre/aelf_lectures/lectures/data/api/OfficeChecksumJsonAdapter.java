package co.epitre.aelf_lectures.lectures.data.api;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import co.epitre.aelf_lectures.lectures.data.IsoDate;
import co.epitre.aelf_lectures.lectures.data.office.OfficeChecksum;

public class OfficeChecksumJsonAdapter {
    @FromJson
    OfficeChecksum OfficeChecksumFromJsonMap(Map<String, Object> raw_office_checksum) throws ParseException {
        String checksum = (String)raw_office_checksum.get("checksum");
        String raw_date = (String)raw_office_checksum.get("generation-date");

        return new OfficeChecksum(checksum, new IsoDate(raw_date));
    }

    @ToJson
    Map<String, String> OfficeChecksumToJsonMap(OfficeChecksum officeChecksum) {
        Map<String, String> buf = new HashMap<>();

        buf.put("checksum", officeChecksum.checksum());

        IsoDate date = officeChecksum.generationDate();
        if (date != null) {
            buf.put("generation-date", date.toString());
        }

        return buf;
    }
}
