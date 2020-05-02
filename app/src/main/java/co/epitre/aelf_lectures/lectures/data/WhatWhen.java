package co.epitre.aelf_lectures.lectures.data;

import java.util.GregorianCalendar;

/**
 * Created by jean-tiare on 11/03/17.
 */

public class WhatWhen {
    public LecturesController.WHAT what;
    public AelfDate when;
    public int position;
    public boolean useCache = true;
    public String anchor = null;

    public WhatWhen() {}

    public WhatWhen(LecturesController.WHAT what, AelfDate when) {
        this.what = what;
        this.when = when;
    }

    public String toTrackerName() {
        return what.urlName()+"."+when.dayBetween(new GregorianCalendar());
    }

    public String toUrlName() {
        return what.urlName()+"/"+when.toIsoString();
    }

    public WhatWhen copy() {
        WhatWhen c = new WhatWhen();
        c.what = what;
        c.when = when;
        c.position = position;
        c.useCache = useCache;
        c.anchor = anchor;
        return c;
    }
}
