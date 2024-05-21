package co.epitre.aelf_lectures.lectures.data.api;

import co.epitre.aelf_lectures.lectures.data.IsoDate;
import co.epitre.aelf_lectures.lectures.data.office.Office;

public record OfficeResponse (
        Office office,
        String checksum,
        IsoDate generationDate
) {}
