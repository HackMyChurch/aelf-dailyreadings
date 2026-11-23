package co.epitre.aelf_lectures.bible.data.bible;

import androidx.annotation.NonNull;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BibleBookList {
    private final ArrayList<BiblePart> mParts;
    private final Map<String, String> mAliasMap;
    private static BibleBookList instance = null;

    synchronized public static BibleBookList getInstance() {
        if (instance == null) {
            instance = new BibleBookList();
        }
        return instance;
    }

    private BibleBookList() {
        this.mParts = new ArrayList<>();
        this.mAliasMap = new HashMap<>();

        this.addPart(new BiblePart("Ancien Testament", "ancien")
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Pentateuque"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de la Genèse", "Genèse", "Gn"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de l'Exode", "Exode", "Ex"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre du Lévitique", "Lévitique", "Lv"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Nombres", "Nombres", "Nb"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre du Deutéronome", "Deutéronome", "Dt"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Historiques"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Josué", "Josué", "Jos"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Juges", "Juges", "Jg"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Ruth", "Ruth", "Rt"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier livre de Samuel", "1 Samuel", "1S"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième livre de Samuel", "2 Samuel", "2S"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier livre des Rois", "1 Rois", "1R"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième livre des Rois", "2 Rois", "2R"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier livre des Chroniques", "1 Chroniques", "1Ch"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième livre des Chroniques", "2 Chroniques", "2Ch"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Esdras", "Esdras", "Esd"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Néhémie", "Néhémie", "Ne"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Tobie", "Tobie", "Tb"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Judith", "Judith", "Jdt"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Esther", "Esther", "Est"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier livre des Martyrs d'Israël", "1 Maccabées", "1M"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Livre des Martyrs d'Israël", "2 Maccabées", "2M"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Poètiques et Sapientiaux"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Job", "Job", "Jb"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Proverbes", "Proverbes", "Pr"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de l'Écclésiaste (Qohèlet)", "Écclésiaste", "Qo", new String[]{"Qohèlet"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Cantique des Cantiques", "Cantique", "Ct", new String[]{"Cantique des Cantiques"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de la Sagesse", "Sagesse", "Sg"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Ben Sira (Ecclésiastique)", "Ben Sira", "Si", new String[]{"Siracide", "Ecclésiastique"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Prophètiques"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Isaïe", "Isaïe", "Is"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Jérémie", "Jérémie", "Jr"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Lamentations", "Lamentations", "Lm"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Baruch", "Baruch", "Ba"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre de Jérémie", "1Jr"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Ézékiel", "Ézékiel", "Ez", new String[]{"Ézéchiel"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Daniel", "Daniel", "Dn"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Osée", "Osée", "Os"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Joël", "Joël", "Jl"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Amos", "Amos", "Am"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Abdias", "Abdias", "Ab"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Jonas", "Jonas", "Jon"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Michée", "Michée", "Mi"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Nahoum", "Nahoum", "Na", new String[]{"Nahum"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Habacuc", "Habacuc", "Ha"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Sophonie", "Sophonie", "So"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Aggée", "Aggée", "Ag"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Zacharie", "Zacharie", "Za"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Malachie", "Malachie", "Ml"))
        );

        BiblePart biblePart = new BiblePart("Psaumes", "psaumes");
        for (int i = 1; i <= 150; i++) {
            if (i == 9 || i == 113) {
                biblePart.addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Psaumes", "Psaume " + i + "A", "Ps", i + "A"));
                biblePart.addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Psaumes", "Psaume " + i + "B", "Ps", i + "B"));
            } else {
                biblePart.addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Psaumes", "Psaume " + i, "Ps", "" + i));
            }
        }
        this.addPart(biblePart);

        this.addPart(new BiblePart("Nouveau Testament", "nouveau")
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Évangiles"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon saint Matthieu", "Matthieu", "Mt", new String[]{"Saint Matthieu"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon saint Marc", "Marc", "Mc", new String[]{"Saint Marc"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon saint Luc", "Luc", "Lc", new String[]{"Saint Luc"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon saint Jean", "Jean", "Jn", new String[]{"Saint Jean"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Actes"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Actes des Apôtres", "Actes", "Ac", new String[]{"Actes des Apôtres"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Lettres de saint Paul"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Romains", "Romains", "Rm"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre aux Corinthiens", "1 Corinthiens", "1Co"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre aux Corinthiens", "2 Corinthiens", "2Co"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Galates", "Galates", "Ga"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Ephésiens", "Éphésiens", "Ep"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Philippiens", "Philippiens", "Ph"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Colossiens", "Colossiens", "Col"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre aux Thessaloniciens", "1 Thessaloniciens", "1Th"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre aux Thessaloniciens", "2 Thessaloniciens", "2Th"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre à Timothée", "1 Timothée", "1Tm"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre à Timothée", "2 Timothée", "2Tm"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre à Tite", "Tite", "Tt"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre à Philémon", "Philémon", "Phm"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Hébreux", "Hébreux", "He"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Lettres Catholiques"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre de saint Jacques", "Jacques", "Jc", new String[]{"Saint Jacques"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre de saint Pierre", "1 Pierre", "1P"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre de saint Pierre", "2 Pierre", "2P"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre de saint Jean", "1 Jean", "1Jn"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre de saint Jean", "2 Jean", "2Jn"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Troisième lettre de saint Jean", "3 Jean", "3Jn"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre de saint Jude", "Jude", "Jude", new String[]{"Saint Jude"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Apocalypse"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Apocalypse", "Apocalypse", "Ap"))
        );

        initAliasMap();
    }

    private void registerAlias(String alias, String canonicalBookRef) {
        if (alias != null && !alias.trim().isEmpty()) {
            String key = normalizeKey(alias);
            mAliasMap.put(key, canonicalBookRef);
        }
    }

    private void initAliasMap() {
        for (BiblePart part : mParts) {
            for (BibleBookEntry entry : part.getBibleBookEntries()) {
                if (entry.getType() == BibleBookEntryType.BOOK && entry.getBookRef() != null) {
                    String canonicalRef = entry.getBookRef();
                    registerAlias(canonicalRef, canonicalRef);
                    registerAlias(entry.getBookName(), canonicalRef);
                    if (entry.getAliases() != null) {
                        for (String alias : entry.getAliases()) {
                            registerAlias(alias, canonicalRef);
                        }
                    }
                }
            }
        }
    }

    public static String normalizeKey(String input) {
        if (input == null) {
            return "";
        }

        // Decompose accentuated letters into base+diacritics
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // Strip diacritics
        normalized = normalized.replaceAll("\\p{M}", "");

        // Lower case and remove blanks
        return normalized.toLowerCase().replaceAll("\\s+", "");
    }

    /**
     * Resolves a book code, name, or alias (e.g. "Lc", "Luc", "1 Co", "Éphésiens", "Psaume")
     * to its canonical bookRef (e.g. "Lc", "1Co", "Ep", "Ps").
     * Returns null if not found.
     */
    public String findBookRef(String candidate) {
        if (candidate == null || candidate.trim().isEmpty()) {
            return null;
        }
        String key = normalizeKey(candidate);
        return mAliasMap.get(key);
    }

    /**
     * Returns the 0-based ordinal index of a book in canonical Bible order.
     * Returns -1 if candidate cannot be resolved.
     */
    public int getBookIndex(String candidate) {
        if (candidate == null || candidate.trim().isEmpty()) {
            return -1;
        }
        String canonical = findBookRef(candidate);
        if (canonical == null) {
            canonical = candidate;
        }
        int index = 0;
        for (BiblePart part : mParts) {
            for (BibleBookEntry entry : part.getBibleBookEntries()) {
                if (entry.getType() == BibleBookEntryType.BOOK && entry.getBookRef() != null) {
                    if (entry.getBookRef().equalsIgnoreCase(canonical)) {
                        return index;
                    }
                    index++;
                }
            }
        }
        return -1;
    }

    /**
     * Resolves a book code, name, or alias to its corresponding BibleBookEntry.
     * Returns null if not found.
     */
    public BibleBookEntry findBookEntry(String candidate) {
        String bookRef = findBookRef(candidate);
        if (bookRef == null) {
            return null;
        }
        for (BiblePart part : mParts) {
            for (BibleBookEntry entry : part.getBibleBookEntries()) {
                if (entry.getType() == BibleBookEntryType.BOOK && bookRef.equals(entry.getBookRef())) {
                    return entry;
                }
            }
        }
        return null;
    }

    public List<BiblePart> getParts() {
        return this.mParts;
    }

    public BibleBookList addPart(@NonNull BiblePart part) {
        this.mParts.add(part);
        return this;
    }
}
