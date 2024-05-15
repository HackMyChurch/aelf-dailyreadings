package co.epitre.aelf_lectures.lectures.data.cache;

import co.epitre.aelf_lectures.lectures.data.office.Office;

public class CacheEntry {
    public final Office office;
    public final String checksum;

    CacheEntry(Office office, String checksum) {
        this.office = office;
        this.checksum = checksum;
    }
}
