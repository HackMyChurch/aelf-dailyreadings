package co.epitre.aelf_lectures.lectures.data.office;

import java.util.Map;

import co.epitre.aelf_lectures.lectures.data.AelfDate;
import co.epitre.aelf_lectures.lectures.data.OfficeTypes;

public class OfficesChecksums {
    // Lightweight wrapper for Json of the form YYYY-MM-DD --> OfficeName --> checksum map
    private final Map<String, Map<String, String>> checksums;

    public OfficesChecksums(Map<String, Map<String, String>> checksums) {
        this.checksums = checksums;
    }

    public String getOfficeChecksum(OfficeTypes office, AelfDate date) {
        Map<String, String> officeChecksums = checksums.get(date.toIsoString());
        if (officeChecksums == null) {
            return null;
        }

        return officeChecksums.get(office.apiName());
    }
}
