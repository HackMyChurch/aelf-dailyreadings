package co.epitre.aelf_lectures.lectures.data.office;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.squareup.moshi.Json;

import java.io.Serializable;
import java.util.Locale;

public class Lecture implements Serializable {
    public enum AntiennePosition {
        NONE, INITIAL, FINAL, BOTH;

        public static AntiennePosition fromString(String raw) {
            raw = raw.toUpperCase();
            for (AntiennePosition pos : AntiennePosition.values()) {
                if (pos.name().equals(raw)) {
                    return pos;
                }
            }
            return AntiennePosition.NONE;
        }

        @NonNull
        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
    String key;
    @Json(name = "key.orig") String keyOrig;
    String type;
    String title;
    @Json(name = "short_title") String shortTitle;
    @Json(name = "long_title")  String longTitle;
    @Json(name = "variant_title")  String variantTitle;
    @Json(name = "has_antienne")  AntiennePosition hasAntienne = AntiennePosition.BOTH;
    @Json(name = "has_doxology")  boolean hasDoxology;
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

        if (this.should_display_initial_antienne()) {
            bodyBuilder.append("<div class=\"antienne\"><span tabindex=\"0\" id=\"");
            bodyBuilder.append(this.key);
            bodyBuilder.append("-antienne-1\" class=\"line\"><span class=\"antienne-title\">Antienne&nbsp;:</span> ");
            bodyBuilder.append(TextUtils.htmlEncode(this.antienne));
            bodyBuilder.append("</span></div>");
        }

        bodyBuilder.append(this.text);

        if (this.should_display_doxology()) {
            bodyBuilder.append("<div class=\"doxology\"><p>");
            bodyBuilder.append("<span tabindex=\"0\" id=\"");
            bodyBuilder.append(this.key);
            bodyBuilder.append("-doxology-1\" class=\"line line-wrap\">Gloire au Père, et au Fils, et au Saint-Esprit,</span>");
            bodyBuilder.append("<span tabindex=\"0\" id=\"");
            bodyBuilder.append(this.key);
            bodyBuilder.append("-doxology-2\" class=\"line line-wrap\">pour les siècles des siècles. Amen.</span>");
            bodyBuilder.append("</p></div>");
        }

        if (this.should_display_final_antienne()) {
            bodyBuilder.append("<div class=\"antienne\"><span tabindex=\"0\" id=\"");
            bodyBuilder.append(this.key);
            bodyBuilder.append("-antienne-2\" class=\"line\"><span class=\"antienne-title\">Antienne&nbsp;:</span> ");
            bodyBuilder.append(TextUtils.htmlEncode(this.antienne));
            bodyBuilder.append("</span></div>");
        }

        if (this.verset != null && !this.verset.isEmpty()) {
            bodyBuilder.append("<p class=\"verset\">");
            bodyBuilder.append(this.verset);
            bodyBuilder.append("</p>");
        }

        if (this.repons != null && !this.repons.isEmpty()) {
            bodyBuilder.append("<p class=\"repons\">");
            bodyBuilder.append(this.repons);
            bodyBuilder.append("</p>");
        }

        this.html = bodyBuilder.toString();
        return this.html;
    }

    /**
     * Predicates
     */

    private boolean should_display_initial_antienne() {
        return this.antienne != null && (this.hasAntienne == AntiennePosition.INITIAL || this.hasAntienne == AntiennePosition.BOTH);
    }

    private boolean should_display_final_antienne() {
        return this.antienne != null && (this.hasAntienne == AntiennePosition.FINAL || this.hasAntienne == AntiennePosition.BOTH);
    }

    private boolean should_display_doxology() {
        return this.hasDoxology;
    }
}
