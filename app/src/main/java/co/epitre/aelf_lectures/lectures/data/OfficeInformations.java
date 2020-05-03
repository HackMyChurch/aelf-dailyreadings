package co.epitre.aelf_lectures.lectures.data;

import com.squareup.moshi.Json;

import java.io.Serializable;

class OfficeInformations implements Serializable {
    String couleur;
    String date;
    String zone;
    String fete;
    String degree;
    String jour;
    String semaine;
    String ligne1;
    String ligne2;
    String ligne3;
    String text;
    @Json(name = "temps_liturgique")    String tempsLiturgique;
    @Json(name = "jour_liturgique_nom") String jourLiturgique;
}
