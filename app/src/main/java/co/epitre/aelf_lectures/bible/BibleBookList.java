package co.epitre.aelf_lectures.bible;

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
        this
                .addPart(new BiblePart("Ancien Testament")
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Pentateuque"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "La Genèse"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "L'Exode"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Lévitique"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Les Nombres"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Deutéronome"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Historiques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre de Josué"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre des Juges"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre de Ruth"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier Livre de Samuel"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Livre de Samuel"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier Livre des Rois"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Livre des Rois"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier Livre des Chroniques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Livre des Chroniques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre d'Esdras"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre de Néhémie"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Tobie"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Judith"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Esther"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier Livre des Maccabées"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Livre des Maccabées"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Poètiques et Sapientiaux"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Job"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Les Proverbes"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "L'Écclésiaste (Qohélet)"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Cantique des Cantiques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre de la Sagesse"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "L'Écclésiastique (Siracide)"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Livres Prophètiques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Isaïe"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Jérémie"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Les Lamentations"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Le Livre de Baruch"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Ézéchiel"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Daniel"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Osée"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Joël"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Amos"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Abdias"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Jonas"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Michée"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Nahum"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Habaquq"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Sophonie"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Aggée"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Zacharie"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Malachie"))
                )
                .addPart(new BiblePart("Nouveau Testament")
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Évangiles"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon Saint Matthieu"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon Saint Marc"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon Saint Luc"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Évangile selon Saint Jean"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Actes"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Les Actes des Apôtres"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Épitres de Saint Paul"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Aux Romains"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première aux Corinthiens"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième aux Corinthiens"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Aux Galates"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Aux Éphésiens"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Aux Philippiens"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Aux Colossiens"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première aux Théssaloniciens"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième aux Théssaloniciens"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Première à Timothée"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième à Timothée"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "À Tite"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "À Philémon"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Épîtres Catholiques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Épître aux Hébreux"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Épître de Saint Jacques"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier Épître de Saint Pierre"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Épître de Saint Pierre"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Premier Épître de Saint Jean"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Deuxième Épître de Saint Jean"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Troisième Épître de Saint Jean"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Épître de Saint Jude"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.SECTION, "Apocalypse"))
                        .addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "L'Apocalypse"))
                );

        BiblePart biblePart = new BiblePart("Psaumes");
        for (int i = 1; i<= 150; i++) {
            if (i == 9 || i == 113) {
                biblePart.addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Psaume "+i+"A"));
                biblePart.addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Psaume "+i+"B"));
            } else {
                biblePart.addBibleBookEntry(new BibleBookEntry(BibleBookEntryType.BOOK, "Psaume "+i));
            }
        }
        this.addPart(biblePart);
    }

    public List<BiblePart> getParts() {
        return this.mParts;
    }

    public BibleBookList addPart(@NonNull BiblePart part) {
        this.mParts.add(part);
        return this;
    }
}