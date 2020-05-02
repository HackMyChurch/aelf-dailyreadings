package co.epitre.aelf_lectures.lectures.data;

import android.text.TextUtils;

import com.squareup.moshi.Json;

class Lecture {
    String key;
    @Json(name = "key.orig") String keyOrig;
    String type;
    String title;
    @Json(name = "short_title") String shortTitle;
    @Json(name = "long_title")  String longTitle;
    String reference;
    String antienne;
    String verset;
    String repons;
    String text;

    private String html = null;

    public String getShortTitle() {
        if (this.shortTitle != null && !this.shortTitle.isEmpty()) {
            return this.shortTitle;
        } else {
            return this.title;
        }
    }

    public String getReference() {
        return reference;
    }

    public String getKey() {
        return key;
    }

    public String toHtml() {
        if (this.html != null) {
            return this.html;
        }

        StringBuilder bodyBuilder = new StringBuilder();

        if (this.longTitle != null && !this.longTitle.isEmpty()) {
            bodyBuilder.append("<h3>");
            bodyBuilder.append(TextUtils.htmlEncode(this.longTitle));
            if (this.reference != null && !this.reference.isEmpty()) {
                bodyBuilder.append("<small><i>— ");
                bodyBuilder.append(TextUtils.htmlEncode(this.reference));
                bodyBuilder.append("</i></small>");
            }
            bodyBuilder.append("</h3>");
            bodyBuilder.append("<div style=\"clear: both;\"></div>");
        }

        if (this.antienne != null && !this.antienne.isEmpty()) {
            bodyBuilder.append("<div class=\"antienne\"><span tabindex=\"0\" id=\"");
            bodyBuilder.append(this.key);
            bodyBuilder.append("-antienne-1\" class=\"line\"><span class=\"antienne-title\">Antienne&nbsp;:</span> ");
            bodyBuilder.append(TextUtils.htmlEncode(this.antienne));
            bodyBuilder.append("</span></div>");
        }

        bodyBuilder.append(this.text);

        if (this.antienne != null && !this.antienne.isEmpty()) {
            bodyBuilder.append("<div class=\"gloria_patri\"><span tabindex=\"0\" id=\"");
            bodyBuilder.append(this.key);
            bodyBuilder.append("-gloria_patri\" class=\"line\">Gloire au Père, ...</span></div>");

            bodyBuilder.append("<div class=\"antienne\"><span tabindex=\"0\" id=\"");
            bodyBuilder.append(this.key);
            bodyBuilder.append("-antienne-2\" class=\"line\"><span class=\"antienne-title\">Antienne&nbsp;:</span> ");
            bodyBuilder.append(TextUtils.htmlEncode(this.antienne));
            bodyBuilder.append("</span></div>");
        }
        if (this.verset != null && !this.verset.isEmpty()) {
            bodyBuilder.append("<blockquote class=\"verset\">");
            bodyBuilder.append(this.verset);
            bodyBuilder.append("</blockquote>");
        }
        if (this.repons != null && !this.repons.isEmpty()) {
            bodyBuilder.append("<blockquote class=\"repons\">");
            bodyBuilder.append(this.repons);
            bodyBuilder.append("</blockquote>");
        }

        this.html = bodyBuilder.toString();
        return this.html;
    }
}
