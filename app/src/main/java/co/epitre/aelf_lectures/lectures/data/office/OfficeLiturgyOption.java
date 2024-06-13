package co.epitre.aelf_lectures.lectures.data.office;

import com.squareup.moshi.Json;

import java.io.Serializable;

public class OfficeLiturgyOption implements Serializable {
    @Json(name = "liturgical_color") String color;
    @Json(name = "liturgical_degree") String degree;
    @Json(name = "liturgical_name") String name;

    public String getColor() {
        return color;
    }

    public String getDegree() {
        return degree;
    }

    public String getName() {
        return name;
    }
}
