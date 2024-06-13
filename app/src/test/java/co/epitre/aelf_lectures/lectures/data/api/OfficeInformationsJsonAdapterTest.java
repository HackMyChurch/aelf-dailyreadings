package co.epitre.aelf_lectures.lectures.data.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import co.epitre.aelf_lectures.lectures.data.office.OfficeInformations;
import co.epitre.aelf_lectures.lectures.data.office.OfficeLiturgyOption;

public class OfficeInformationsJsonAdapterTest {
    @Test
    void nominal() throws IOException {
        // Validate nominal decoding of all fields, including unused legacy fields
        String json_input = """
        {
          "annee": "Paire",
          "date": "2024-06-04",
          "jour_liturgique_nom": "de la férie",
          "liturgical_day": "mardi",
          "liturgical_period": "ordinaire",
          "liturgical_week": 9,
          "liturgical_year": "paire",
          "liturgy_options": [
            {
              "liturgical_color": "vert",
              "liturgical_degree": "Férie",
              "liturgical_name": "mardi 9ème Semaine du Temps Ordinaire"
            },
            {
              "liturgical_color": "blanc",
              "liturgical_degree": "Mémoire facultative",
              "liturgical_name": "Clotilde"
            }
          ],
          "psalter_week": 1,
          "semaine": "9ème Semaine du Temps Ordinaire",
          "temps_liturgique": "ordinaire",
          "text": "Mardi de la férie, 9<sup>ème</sup> Semaine du Temps Ordinaire (semaine I du psautier) de l'année Paire. Nous célèbrons Clotilde. La couleur liturgique est le vert.",
          "zone": "france"
        }""";

        Moshi moshi = new Moshi.Builder()
                .add(new OfficeChecksumJsonAdapter())
                .build();
        JsonAdapter<OfficeInformations> adapter = moshi.adapter(OfficeInformations.class);
        OfficeInformations officeInformations = adapter.fromJson(json_input);

        assertNotNull(officeInformations);

        // Test general field decoding
        assertEquals("mardi", officeInformations.getLiturgicalDay());
        assertEquals("ordinaire", officeInformations.getLiturgicalPeriod());
        assertEquals("paire", officeInformations.getLiturgicalYear());
        assertEquals("Ⅰ", officeInformations.getRomanPsalterWeek());

        // Test liturgical options
        List<OfficeLiturgyOption> liturgyOptions = officeInformations.getLiturgyOptions();
        assertNotNull(liturgyOptions);
        assertEquals(2, liturgyOptions.size());
        assertEquals("vert", liturgyOptions.get(0).getColor());
        assertEquals("Férie", liturgyOptions.get(0).getDegree());
        assertEquals("mardi 9ème Semaine du Temps Ordinaire", liturgyOptions.get(0).getName());

        assertEquals("blanc", liturgyOptions.get(1).getColor());
        assertEquals("Mémoire facultative", liturgyOptions.get(1).getDegree());
        assertEquals("Clotilde", liturgyOptions.get(1).getName());
    }

    @Test
    void empty() throws IOException {
        // Make sure this does not crash
        String json_input = "{}";

        Moshi moshi = new Moshi.Builder()
                .add(new OfficeChecksumJsonAdapter())
                .build();
        JsonAdapter<OfficeInformations> adapter = moshi.adapter(OfficeInformations.class);
        OfficeInformations officeInformations = adapter.fromJson(json_input);

        assertNotNull(officeInformations);

        // Test general field decoding
        assertEquals("Jour inconnu", officeInformations.getLiturgicalDay());
        assertEquals("Inconnue", officeInformations.getLiturgicalPeriod());
        assertNull(officeInformations.getLiturgicalYear());
        assertNull(officeInformations.getRomanPsalterWeek());

        // Test liturgical options
        List<OfficeLiturgyOption> liturgyOptions = officeInformations.getLiturgyOptions();
        assertNotNull(liturgyOptions);
        assertEquals(0, liturgyOptions.size());
    }
}
