package co.epitre.aelf_lectures.lectures.data.office;

import java.util.Map;

import co.epitre.aelf_lectures.lectures.data.AelfDate;
import co.epitre.aelf_lectures.lectures.data.OfficeTypes;

public class OfficesMetadata {
    // Lightweight wrapper for Json of the form YYYY-MM-DD --> OfficeName --> checksum map
    private final Map<String, Map<String, OfficeMetadata>> checksums;

    public OfficesMetadata(Map<String, Map<String, OfficeMetadata>> checksums) {
        this.checksums = checksums;
    }

    public OfficeMetadata getOfficeChecksum(OfficeTypes office, AelfDate date) {
        Map<String, OfficeMetadata> officeChecksums = checksums.get(date.toIsoString());
        if (officeChecksums == null) {
            return null;
        }

        return officeChecksums.get(office.apiName());
    }
}
