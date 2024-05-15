package co.epitre.aelf_lectures.lectures.data;

public final class OfficeResponse {
    public final Office office;
    public final String checksum;

    OfficeResponse(Office office, String checksum) {
        this.office = office;
        this.checksum = checksum;
    }
}
