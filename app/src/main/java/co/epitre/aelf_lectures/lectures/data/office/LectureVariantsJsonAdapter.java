package co.epitre.aelf_lectures.lectures.data.office;

import com.squareup.moshi.FromJson;

import java.util.List;

public class LectureVariantsJsonAdapter {
    @FromJson
    LectureVariants lectureVariantsFromJson(List<Lecture> lectureVariantsFromJson) {
        return new LectureVariants(lectureVariantsFromJson);
    }
}
