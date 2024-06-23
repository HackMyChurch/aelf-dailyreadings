package co.epitre.aelf_lectures.lectures.data;

/**
 * Created by jean-tiare on 11/03/17.
 */

public class WhatWhen {
    public OfficeTypes what;
    public AelfDate when;
    public String anchor = null;

    public WhatWhen() {}

    public WhatWhen copy() {
        WhatWhen c = new WhatWhen();
        c.what = what;
        c.when = when;
        c.anchor = anchor;
        return c;
    }
}
