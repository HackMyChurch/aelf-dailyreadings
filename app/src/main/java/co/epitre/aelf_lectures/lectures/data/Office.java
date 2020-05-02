package co.epitre.aelf_lectures.lectures.data;

import com.squareup.moshi.Json;

import java.util.List;

class Office {
    String date;
    @Json(name = "office") String name;
    String source;
    OfficeInformations informations;
    List<OfficeVariant> variants;
}
