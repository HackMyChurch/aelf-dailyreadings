package co.epitre.aelf_lectures.lectures.data.api;

import co.epitre.aelf_lectures.lectures.data.office.Office;

public final class OfficeResponse {
    public final Office office;
    public final String checksum;

    OfficeResponse(Office office, String checksum) {
        this.office = office;
        this.checksum = checksum;
    }
}
