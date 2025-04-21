package co.epitre.aelf_lectures.bible.data;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class BibleBookList {
    private ArrayList<BiblePart> mParts;
    private static BibleBookList instance = null;

    synchronized public static BibleBookList getInstance() {
        if(instance == null) {
            instance = new BibleBookList();
        }
        return instance;
    }

    private BibleBookList() {
        this.mParts = new ArrayList<>();
        this.addPart(new BiblePart("Ancien Testament", "ancien")
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Pentateuque"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de la Genèse", "Gn"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de l'Exode", "Ex"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre du Lévitique", "Lv"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Nombres", "Nb"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre du Deutéronome", "Dt"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Historiques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Josué", "Jos"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Juges", "Jg"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Ruth", "Rt"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier livre de Samuel", "1S"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième livre de Samuel", "2S"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier livre des Rois", "1R"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième livre des Rois", "2R"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier livre des Chroniques", "1Ch"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième livre des Chroniques", "2Ch"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Esdras", "Esd"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Néhémie", "Ne"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Tobie", "Tb"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Judith", "Jdt"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Esther", "Est"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier livre des Martyrs d'Israël", "1M"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Livre des Martyrs d'Israël", "2M"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Poètiques et Sapientiaux"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Job", "Jb"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Proverbes", "Pr"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de l'Écclésiaste (ou Qohèlet)", "Qo"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Cantique des Cantiques", "Ct"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de la Sagesse", "Sg"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Ben Sira (ou Ecclésiastique)", "Si"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Prophètiques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Isaïe", "Is"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Jérémie", "Jr"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Lamentations", "Lm"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Baruch", "Ba"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre de Jérémie", "1Jr"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Ézékiel", "Ez"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Daniel", "Dn"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Osée", "Os"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Joël", "Jl"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Amos", "Am"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Abdias", "Ab"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Jonas", "Jon"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Michée", "Mi"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Nahoum", "Na"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Habacuc", "Ha"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Sophonie", "So"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre d'Aggée", "Ag"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Zacharie", "Za"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre de Malachie", "Ml"))
        );

        BiblePart biblePart = new BiblePart("Psaumes", "psaumes");
        for (int i = 1; i<= 150; i++) {
            if (i == 9 || i == 113) {
                biblePart.addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Psaumes", "Psaume "+i+"A", "Ps", i+"A"));
                biblePart.addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Psaumes", "Psaume "+i+"B", "Ps", i+"B"));
            } else {
                biblePart.addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Livre des Psaumes", "Psaume "+i, "Ps", ""+i));
            }
        }
        this.addPart(biblePart);

        this.addPart(new BiblePart("Nouveau Testament", "nouveau")
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Évangiles"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon saint Matthieu", "Mt"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon saint Marc", "Mc"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon saint Luc", "Lc"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon saint Jean", "Jn"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Actes"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Actes des Apôtres", "Ac"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Lettres de saint Paul"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Romains", "Rm"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre aux Corinthiens", "1Co"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre aux Corinthiens", "2Co"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Galates", "Ga"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Ephésiens", "Ep"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Philippiens", "Ph"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Colossiens", "Col"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre aux Thessaloniciens", "1Th"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre aux Thessaloniciens", "2Th"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre à Timothée", "1Tm"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre à Timothée", "2Tm"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre à Tite", "Tt"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre à Philémon", "Phm"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre aux Hébreux", "He"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Lettres Catholiques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre de saint Jacques", "Jc"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre de saint Pierre", "1P"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre de saint Pierre", "2P"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première lettre de saint Jean", "1Jn"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième lettre de saint Jean", "2Jn"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Troisième lettre de saint Jean", "3Jn"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre de saint Jude", "Jude"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Apocalypse"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Apocalypse", "Ap"))
                );
    }

    public List<BiblePart> getParts() {
        return this.mParts;
    }

    public BibleBookList addPart(@NonNull BiblePart part) {
        this.mParts.add(part);
        return this;
    }
}