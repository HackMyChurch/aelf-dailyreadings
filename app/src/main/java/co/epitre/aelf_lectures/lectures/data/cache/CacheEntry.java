package co.epitre.aelf_lectures.lectures.data.cache;

import co.epitre.aelf_lectures.lectures.data.office.Office;

public record CacheEntry(Office office, String checksum) {}
