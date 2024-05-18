package co.epitre.aelf_lectures.lectures.data.api;

import com.squareup.moshi.FromJson;

import java.util.List;

import co.epitre.aelf_lectures.lectures.data.office.Lecture;
import co.epitre.aelf_lectures.lectures.data.office.LectureVariants;

public class LectureVariantsJsonAdapter {
    @FromJson
    LectureVariants lectureVariantsFromJson(List<Lecture> lectureVariantsFromJson) {
        return new LectureVariants(lectureVariantsFromJson);
    }
}
