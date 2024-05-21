package co.epitre.aelf_lectures.lectures.data.office;

import android.text.TextUtils;

import com.squareup.moshi.Json;

import java.io.Serializable;

public class Lecture implements Serializable {
    String key;
    @Json(name = "key.orig") String keyOrig;
    String type;
    String title;
    @Json(name = "short_title") String shortTitle;
    @Json(name = "long_title")  String longTitle;
    @Json(name = "variant_title")  String variantTitle;
    String reference;
    String antienne;
    String verset;
    String repons;
    String text;

    private transient String html = null;

    public final String getShortTitle() {
        if (this.shortTitle != null && !this.shortTitle.isEmpty()) {
            return this.shortTitle;
        } else {
            return this.title;
        }
    }

    public final String getVariantTitle() {
        if (variantTitle != null) {
            return variantTitle;
        }

        StringBuilder lectureVariantTitle = new StringBuilder();
        lectureVariantTitle.append(title.trim());
        if (reference != null && !reference.isEmpty()) {
            lectureVariantTitle.append(" (");
            lectureVariantTitle.append(reference);
            lectureVariantTitle.append(" )");
        }
        variantTitle = lectureVariantTitle.toString();

        return variantTitle;
    }

    public final String getTitle() {
        return title;
    }

    public final String getReference() {
        if (reference == null || reference.equals("")) {
            return null;
        }
        return reference;
    }

    public final String getKey() {
        return key;
    }

    public final String toHtml() {
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
            // Insert the doxology, unless Dn 3 which implies it.
            if (this.reference != null && !this.reference.equals("Dn 3")) {
                bodyBuilder.append("<div class=\"doxology\"><p>");
                bodyBuilder.append("<span tabindex=\"0\" id=\"");
                bodyBuilder.append(this.key);
                bodyBuilder.append("-doxology-1\" class=\"line line-wrap\">Gloire au Père, et au Fils, et au Saint-Esprit,</span>");
                bodyBuilder.append("<span tabindex=\"0\" id=\"");
                bodyBuilder.append(this.key);
                bodyBuilder.append("-doxology-2\" class=\"line line-wrap\">pour les siècles des siècles. Amen.</span>");
                bodyBuilder.append("</p></div>");
            }

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
