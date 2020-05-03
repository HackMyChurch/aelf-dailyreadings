package co.epitre.aelf_lectures.lectures.data;

import com.squareup.moshi.Json;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Office implements Serializable {
    /*
     * Data
     */
    String date;
    @Json(name = "office") String name;
    String source;
    OfficeInformations informations;
    List<OfficeVariant> variants;

    /*
     * Cache
     */
    private transient List<List<Lecture>> lectures;

    public final List<OfficeVariant> getVariants() {
        return variants;
    }

    public final List<List<Lecture>> getLectures() {
        if (lectures != null) {
            return lectures;
        }

        lectures = new ArrayList<>();
        for (OfficeVariant officeVariant : getVariants()) {
            for (List<Lecture> lectureVariant : officeVariant.getLectures()) {
                lectures.add(lectureVariant);
            }
        }

        return lectures;
    }

    public int getLecturePosition(String key) {
        int position = -1;
        for (List<Lecture> lectureVariants : lectures) {
            position++;
            Lecture lecture = lectureVariants.get(0);
            if (key.equals(lecture.key)) {
                return position;
            }
        }

        return -1;
    }

    public static Office createError(String message) {

        Lecture errorLecture = new Lecture();
        errorLecture.text = message;
        errorLecture.title = "Erreur";
        errorLecture.key = "error";

        List<Lecture> lectures = Arrays.asList(errorLecture);

        OfficeVariant errorVariant = new OfficeVariant();
        errorVariant.lectures = Arrays.asList(lectures);

        Office office = new Office();
        office.variants = Arrays.asList(errorVariant);

        return office;
    }
}
