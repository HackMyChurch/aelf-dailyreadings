package co.epitre.aelf_lectures.lectures.data;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by jean-tiare on 12/05/17.
 */

public class Validator {
    public static final boolean isValidUrl(String candidate) {
        // Attempt connection
        try {
            new URL(candidate);
        } catch (MalformedURLException e) {
            return false;
        }

        return true;
    }
}
