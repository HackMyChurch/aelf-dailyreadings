package co.epitre.aelf_lectures.bible.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BibleBookListTest {

    @Test
    void testShortBookNameAndImplicitAlias() {
        BibleBookList bookList = BibleBookList.getInstance();
        assertNotNull(bookList);

        // Test Matthieu
        String matthieuRef = bookList.findBookRef("Matthieu");
        assertEquals("Mt", matthieuRef);

        // Find Matthieu entry
        BibleBookEntry matthieuEntry = null;
        for (BiblePart part : bookList.getParts()) {
            for (BibleBookEntry entry : part.getBibleBookEntries()) {
                if ("Mt".equals(entry.getBookRef())) {
                    matthieuEntry = entry;
                    break;
                }
            }
        }
        assertNotNull(matthieuEntry);
        assertEquals("Évangile selon saint Matthieu", matthieuEntry.getBookName());
        assertEquals("Matthieu", matthieuEntry.getShortBookName());
        assertTrue(matthieuEntry.getAliases().contains("Matthieu"));
        assertTrue(matthieuEntry.getAliases().contains("Saint Matthieu"));

        // Test alias resolution for shortBookName
        assertEquals("Mt", bookList.findBookRef("Matthieu"));
        assertEquals("Mt", bookList.findBookRef("Saint Matthieu"));
        assertEquals("Mt", bookList.findBookRef("Évangile selon saint Matthieu"));

        // Test Genèse
        assertEquals("Gn", bookList.findBookRef("Genèse"));

        // Test 1 Samuel
        assertEquals("1S", bookList.findBookRef("1 Samuel"));
    }
}
