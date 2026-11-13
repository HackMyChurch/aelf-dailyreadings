package co.epitre.aelf_lectures.bible.data;

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
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de la Genèse", "Gn", new String[]{"Genèse"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de l'Exode", "Ex", new String[]{"Exode"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre du Lévitique", "Lv", new String[]{"Lévitique"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Nombres", "Nb", new String[]{"Nombres"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre du Deutéronome", "Dt", new String[]{"Deutéronome"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Historiques"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Josué", "Jos", new String[]{"Josué"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Juges", "Jg", new String[]{"Juges"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Ruth", "Rt", new String[]{"Ruth"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier livre de Samuel", "1S", new String[]{"1 Samuel"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième livre de Samuel", "2S", new String[]{"2 Samuel"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier livre des Rois", "1R", new String[]{"1 Rois"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième livre des Rois", "2R", new String[]{"2 Rois"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier livre des Chroniques", "1Ch", new String[]{"1 Chroniques"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième livre des Chroniques", "2Ch", new String[]{"2 Chroniques"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Esdras", "Esd", new String[]{"Esdras"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Néhémie", "Ne", new String[]{"Néhémie"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Tobie", "Tb", new String[]{"Tobie"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Judith", "Jdt", new String[]{"Judith"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Esther", "Est", new String[]{"Esther"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier livre des Martyrs d'Israël", "1M", new String[]{"1 Maccabées"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Livre des Martyrs d'Israël", "2M", new String[]{"2 Maccabées"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Poètiques et Sapientiaux"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Job", "Jb", new String[]{"Job"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Proverbes", "Pr", new String[]{"Proverbes"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de l'Écclésiaste (Qohèlet)", "Qo", new String[]{"Écclésiaste", "Qohèlet"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Cantique des Cantiques", "Ct", new String[]{"Cantique des Cantiques", "Cantique"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de la Sagesse", "Sg", new String[]{"Sagesse"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Ben Sira (Ecclésiastique)", "Si", new String[]{"Ben Sira", "Siracide", "Ecclésiastique"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Prophètiques"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Isaïe", "Is", new String[]{"Isaïe"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Jérémie", "Jr", new String[]{"Jérémie"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Lamentations", "Lm", new String[]{"Lamentations"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Baruch", "Ba", new String[]{"Baruch"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre de Jérémie", "1Jr"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Ézékiel", "Ez", new String[]{"Ézékiel", "Ézéchiel"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Daniel", "Dn", new String[]{"Daniel"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Osée", "Os", new String[]{"Osée"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Joël", "Jl", new String[]{"Joël"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Amos", "Am", new String[]{"Amos"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Abdias", "Ab", new String[]{"Abdias"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Jonas", "Jon", new String[]{"Jonas"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Michée", "Mi", new String[]{"Michée"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Nahoum", "Na", new String[]{"Nahoum", "Nahum"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Habacuc", "Ha", new String[]{"Habacuc"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Sophonie", "So", new String[]{"Sophonie"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Aggée", "Ag", new String[]{"Aggée"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Zacharie", "Za", new String[]{"Zacharie"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Malachie", "Ml", new String[]{"Malachie"}))
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
        // Book-level entry for Psaumes aliases
        biblePart.addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Psaumes", "Ps", new String[]{"Psaume", "Psaumes"}));
        this.addPart(biblePart);

        this.addPart(new BiblePart("Nouveau Testament", "nouveau")
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Évangiles"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon saint Matthieu", "Mt", new String[]{"Matthieu", "Saint Matthieu"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon saint Marc", "Mc", new String[]{"Marc", "Saint Marc"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon saint Luc", "Lc", new String[]{"Luc", "Saint Luc"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon saint Jean", "Jn", new String[]{"Jean", "Saint Jean"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Actes"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Actes des Apôtres", "Ac", new String[]{"Actes", "Actes des Apôtres"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Lettres de saint Paul"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Romains", "Rm", new String[]{"Romains"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre aux Corinthiens", "1Co", new String[]{"1 Corinthiens"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "genesDeuxième lettre aux Corinthiens", "2Co", new String[]{"2 Corinthiens"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Galates", "Ga", new String[]{"Galates"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Ephésiens", "Ep", new String[]{"Éphésiens"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Philippiens", "Ph", new String[]{"Philippiens"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Colossiens", "Col", new String[]{"Colossiens"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre aux Thessaloniciens", "1Th", new String[]{"1 Thessaloniciens"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre aux Thessaloniciens", "2Th", new String[]{"2 Thessaloniciens"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre à Timothée", "1Tm", new String[]{"1 Timothée"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre à Timothée", "2Tm", new String[]{"2 Timothée"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre à Tite", "Tt", new String[]{"Tite"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre à Philémon", "Phm", new String[]{"Philémon"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Hébreux", "He", new String[]{"Hébreux"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Lettres Catholiques"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre de saint Jacques", "Jc", new String[]{"Jacques", "Saint Jacques"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre de saint Pierre", "1P", new String[]{"1 Pierre"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre de saint Pierre", "2P", new String[]{"2 Pierre"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre de saint Jean", "1Jn", new String[]{"1 Jean"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre de saint Jean", "2Jn", new String[]{"2 Jean"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Troisième lettre de saint Jean", "3Jn", new String[]{"3 Jean"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre de saint Jude", "Jude", new String[]{"Saint Jude"}))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Apocalypse"))
                .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Apocalypse", "Ap", new String[]{"Apocalypse"}))
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

    public List<BiblePart> getParts() {
        return this.mParts;
    }

    public BibleBookList addPart(@NonNull BiblePart part) {
        this.mParts.add(part);
        return this;
    }
}
