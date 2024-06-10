package co.epitre.aelf_lectures.lectures.data.office;

import androidx.annotation.NonNull;

import com.squareup.moshi.Json;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OfficeInformations implements Serializable {
    @Json(name = "liturgical_day") String liturgicalDay;
    @Json(name = "liturgical_period") String liturgicalPeriod;
    @Json(name = "liturgical_year") String liturgicalYear;
    @Json(name = "psalter_week") Integer psalterWeek;
    @Json(name = "liturgy_options") List<OfficeLiturgyOption> liturgyOptions;

    public String getLiturgicalDay() {
        if (liturgicalDay == null) {
            return "Jour inconnu";
        }
        return liturgicalDay;
    }

    public String getLiturgicalPeriod() {
        if (liturgicalPeriod == null) {
            return "Inconnue";
        }
        return liturgicalPeriod;
    }

    public String getLiturgicalYear() {
        return liturgicalYear;
    }

    public String getRomanPsalterWeek() {
        if (psalterWeek == null) {
            return null;
        }
        return switch (psalterWeek) {
            case 1 -> "Ⅰ";
            case 2 -> "Ⅱ";
            case 3 -> "Ⅲ";
            case 4 -> "Ⅳ";
            default -> null;
        };
    }

    @NonNull public List<OfficeLiturgyOption> getLiturgyOptions() {
        if (liturgyOptions == null) {
            return new ArrayList<>();
        }
        return liturgyOptions;
    }
}
