package co.epitre.aelf_lectures.lectures.data;

import java.io.Serializable;
import java.util.List;

public class OfficeVariant implements Serializable {
    String name;
    List<List<Lecture>> lectures;

    public final List<List<Lecture>> getLectures() {
        return lectures;
    }
}
