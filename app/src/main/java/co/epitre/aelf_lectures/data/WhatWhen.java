package co.epitre.aelf_lectures.data;

/**
 * Created by jean-tiare on 11/03/17.
 */

public class WhatWhen {
    public LecturesController.WHAT what;
    public AelfDate when;
    public boolean today;
    public int position;
    public boolean useCache = true;
    public String anchor = null;

    public WhatWhen copy() {
        WhatWhen c = new WhatWhen();
        c.what = what;
        c.when = when;
        c.today = today;
        c.position = position;
        c.useCache = useCache;
        c.anchor = anchor;
        return c;
    }
}
