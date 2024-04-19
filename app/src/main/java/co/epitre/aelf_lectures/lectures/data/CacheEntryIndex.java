package co.epitre.aelf_lectures.lectures.data;

import android.util.Pair;

public final class CacheEntryIndex extends Pair<String, String> {
    public CacheEntryIndex(String what_str, String when_str) {
        super(what_str, when_str);
    }

    public CacheEntryIndex(LecturesController.WHAT what, AelfDate when) {
        this(what.toString(), when.toIsoString());
    }
}
