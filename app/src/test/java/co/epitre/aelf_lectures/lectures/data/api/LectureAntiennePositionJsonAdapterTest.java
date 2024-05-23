package co.epitre.aelf_lectures.lectures.data.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import co.epitre.aelf_lectures.lectures.data.office.Lecture;

public class LectureAntiennePositionJsonAdapterTest {
    @Test
    void deserialize() throws IOException {
        Moshi moshi = new Moshi.Builder()
                .add(new LectureAntiennePositionJsonAdapter())
                .build();
        JsonAdapter<Lecture.AntiennePosition> adapter = moshi.adapter(Lecture.AntiennePosition.class);

        assertEquals(Lecture.AntiennePosition.NONE, adapter.fromJson("\"nOne\""));
        assertEquals(Lecture.AntiennePosition.INITIAL, adapter.fromJson("\"iNiTiAl\""));
        assertEquals(Lecture.AntiennePosition.FINAL, adapter.fromJson("\"FINAL\""));
        assertEquals(Lecture.AntiennePosition.BOTH, adapter.fromJson("\"both\""));
        assertEquals(Lecture.AntiennePosition.NONE, adapter.fromJson("\"trash\""));
    }
}
