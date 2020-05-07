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
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "La Genèse", "Gn"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "L'Exode", "Ex"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Lévitique", "Lv"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Les Nombres", "Nb"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Deutéronome", "Dt"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Historiques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre de Josué", "Jos"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre des Juges", "Jg"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre de Ruth", "Rt"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier Livre de Samuel", "1S"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Livre de Samuel", "2S"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier Livre des Rois", "1R"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Livre des Rois", "2R"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier Livre des Chroniques", "1Ch"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Livre des Chroniques", "2Ch"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre d'Esdras", "Esd"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre de Néhémie", "Ne"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Tobie", "Tb"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Judith", "Jdt"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Esther", "Est"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier Livre des Martyrs d'Israël", "1M"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Livre des Martyrs d'Israël", "2M"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Poètiques et Sapientiaux"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Job", "Jb"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Les Proverbes", "Pr"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "L'Écclésiaste (Qohélet)", "Qo"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Cantique des Cantiques", "Ct"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre de la Sagesse", "Sg"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "L'Écclésiastique (Siracide)", "Si"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Prophètiques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Isaïe", "Is"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Jérémie", "Jr"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Les Lamentations", "Lm"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Baruch", "Ba"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Lettre de Jérémie", "1Jr"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Ézéchiel", "Ez"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Daniel", "Dn"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Osée", "Os"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Joël", "Jl"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Amos", "Am"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Abdias", "Ab"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Jonas", "Jon"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Michée", "Mi"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Nahum", "Na"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Habaquq", "Ha"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Sophonie", "So"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Aggée", "Ag"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Zacharie", "Za"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Malachie", "Ml"))
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
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon Saint Matthieu", "Mt"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon Saint Marc", "Mc"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon Saint Luc", "Lc"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon Saint Jean", "Jn"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Actes"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Les Actes des Apôtres", "Ac"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Épitres de Saint Paul"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Aux Romains", "Rm"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première aux Corinthiens", "1Co"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième aux Corinthiens", "2Co"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Aux Galates", "Ga"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Aux Éphésiens", "Ep"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Aux Philippiens", "Ph"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Aux Colossiens", "Col"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première aux Théssaloniciens", "1Th"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième aux Théssaloniciens", "2Th"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première à Timothée", "1Tm"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième à Timothée", "2Tm"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "À Tite", "Tt"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "À Philémon", "Phm"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Épîtres Catholiques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Épître aux Hébreux", "He"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Épître de Saint Jacques", "Jc"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier Épître de Saint Pierre", "1P"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Épître de Saint Pierre", "2P"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier Épître de Saint Jean", "1Jn"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Épître de Saint Jean", "2Jn"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Troisième Épître de Saint Jean", "3Jn"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Épître de Saint Jude", "Jude"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Apocalypse"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "L'Apocalypse", "Ap"))
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