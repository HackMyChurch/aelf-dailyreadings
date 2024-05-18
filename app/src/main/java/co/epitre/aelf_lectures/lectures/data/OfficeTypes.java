package co.epitre.aelf_lectures.lectures.data;

import androidx.annotation.NonNull;

import co.epitre.aelf_lectures.R;

/**
 * "What to sync" constants
 */

public enum OfficeTypes {
    MESSE(0, R.id.nav_mass, "lectures_messe"),
    LECTURES(1, R.id.nav_lectures, "lectures_lectures"),
    LAUDES(2, R.id.nav_laudes, "lectures_laudes"),
    TIERCE(3, R.id.nav_tierce, "lectures_tierce"),
    SEXTE(4, R.id.nav_sexte, "lectures_sexte"),
    NONE(5, R.id.nav_none, "lectures_none"),
    VEPRES(6, R.id.nav_vepres, "lectures_vepres"),
    COMPLIES(7, R.id.nav_complies, "lectures_complies"),
    INFORMATIONS(8, R.id.nav_information, "lectures_informations");

    private String name = "";
    private int position = 0; // FIXME: remove field
    private int menu_id;

    OfficeTypes(int position, int menu_id, String name) {
        this.menu_id = menu_id;
        this.position = position;
        this.name = name;
    }

    public static OfficeTypes fromMenuId(int menu_id) {
        for (OfficeTypes what : OfficeTypes.values()) {
            if (what.menu_id == menu_id) {
                return what;
            }
        }
        return null;
    }

    public String apiName() {
        if (this.position == 0) {
            return "messes";
        }
        return this.urlName();
    }

    public String urlName() {
        return this.name.split("_")[1];
    }

    public String actionBarName() {
        if (this.position == 6) {
            return "Vêpres";
        }

        String name = this.name.split("_")[1];
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public String prettyName() {
        if (this.position == 0) {
            return "de la Messe";
        }

        String name = this.urlName();

        if (name.charAt(name.length() - 1) == 's') {
            return "de l'office des " + name;
        } else {
            return "de l'office de " + name;
        }
    }

    // FIXME: remove
    public int getPosition() {
        return position;
    }

    public int getMenuId() {
        return menu_id;
    }

    @NonNull
    public String toString() {
        return name;
    }

}
