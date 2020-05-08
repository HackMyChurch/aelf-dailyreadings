package co.epitre.aelf_lectures.lectures.data;

import java.io.Serializable;
import java.util.List;

public class OfficeVariant implements Serializable {
    String name;
    List<LectureVariants> lectures;

    public final List<LectureVariants> getLectures() {
        return lectures;
    }
}
