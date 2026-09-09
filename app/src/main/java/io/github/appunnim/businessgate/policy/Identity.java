package io.github.appunnim.businessgate.policy;

import java.text.Normalizer;
import java.util.Locale;

/** Display normalization never supplies action identity. */
public final class Identity {
    private Identity() {}
    public static String canonicalPhone(String raw) {
        if (raw == null || raw.length() > 80) throw new IllegalArgumentException("INVALID_NUMBER");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == ' ' || c == '(' || c == ')' || c == '-') continue;
            if ((c >= '0' && c <= '9') || (c == '+' && out.length() == 0)) out.append(c);
            else throw new IllegalArgumentException("INVALID_NUMBER");
        }
        if (!out.toString().matches("\\+[1-9][0-9]{6,14}")) throw new IllegalArgumentException("INVALID_NUMBER");
        return out.toString();
    }
    public static String label(String input) {
        if (input == null) return "";
        StringBuilder out = new StringBuilder();
        input.codePoints().filter(c -> !Character.isISOControl(c) && Character.getType(c) != Character.FORMAT)
            .limit(120).forEach(out::appendCodePoint);
        return out.toString().trim();
    }
    public static String searchKey(String input) {
        return Normalizer.normalize(label(input), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
    public static String query(String input) {
        if (input == null) return "";
        return input.codePoints().limit(128).collect(StringBuilder::new, StringBuilder::appendCodePoint,
            StringBuilder::append).toString().trim();
    }
    public static String likeLiteral(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
