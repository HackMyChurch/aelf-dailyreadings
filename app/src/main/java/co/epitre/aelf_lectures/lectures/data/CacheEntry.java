package co.epitre.aelf_lectures.lectures.data;

public class CacheEntry {
    public final Office office;
    public final String checksum;

    CacheEntry(Office office, String checksum) {
        this.office = office;
        this.checksum = checksum;
    }
}
