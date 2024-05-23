package co.epitre.aelf_lectures.lectures.data.api;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

import co.epitre.aelf_lectures.lectures.data.office.Lecture;

public class LectureAntiennePositionJsonAdapter {
    @FromJson
    Lecture.AntiennePosition LectureAntiennePositionFromJson(String raw) {
        return Lecture.AntiennePosition.fromString(raw);
    }

    @ToJson
    String LectureAntiennePositionToJson(Lecture.AntiennePosition pos) {
        return pos.toString();
    }
}
