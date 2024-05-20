package co.epitre.aelf_lectures.lectures.data.cache;

import co.epitre.aelf_lectures.lectures.data.AelfDate;
import co.epitre.aelf_lectures.lectures.data.OfficeTypes;

public record CacheEntryIndex(String what_str, String when_str) {
    public CacheEntryIndex(OfficeTypes what, AelfDate when) {
        this(what.toString(), when.toIsoString());
    }
}
