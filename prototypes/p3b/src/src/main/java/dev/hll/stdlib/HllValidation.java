package dev.hll.stdlib;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * hll.validation — libreria di validazione per tipi nominali.
 * Ogni metodo restituisce un Predicate<String> usabile come vincolo where.
 */
public class HllValidation {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private static final Pattern URL_PATTERN =
            Pattern.compile("^https?://[a-zA-Z0-9.\\-]+(/.*)?$");

    public static Predicate<String> email() {
        return s -> s != null && EMAIL_PATTERN.matcher(s).matches();
    }

    public static Predicate<String> url() {
        return s -> s != null && URL_PATTERN.matcher(s).matches();
    }

    public static Predicate<String> max_length(int max) {
        return s -> s != null && s.length() <= max;
    }

    public static Predicate<String> min_length(int min) {
        return s -> s != null && s.length() >= min;
    }

    public static Predicate<String> matches(String regex) {
        Pattern p = Pattern.compile(regex);
        return s -> s != null && p.matcher(s).matches();
    }

    public static Predicate<String> not_blank() {
        return s -> s != null && !s.isBlank();
    }

    public static Predicate<String> starts_with(String prefix) {
        return s -> s != null && s.startsWith(prefix);
    }
}
