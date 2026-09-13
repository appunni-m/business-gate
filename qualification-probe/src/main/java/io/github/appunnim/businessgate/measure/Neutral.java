package io.github.appunnim.businessgate.measure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Set;

/** Output policy fingerprints are never reconstructed as runtime identifiers. */
public final class Neutral {
    private Neutral() {}
    private static final Set<String> RESTRICTED = Set.of("ec8202b6f9fb16f9e26b66367afa4e037752f3c09a18cefab426165e06a424b1");
    public static String digest(byte[] bytes) {
        try {
            StringBuilder out = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) out.append(String.format(Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException("DIGEST_UNAVAILABLE"); }
    }
    public static String digest(String value) { return digest(value.getBytes(StandardCharsets.UTF_8)); }
    public static boolean safeIdentifier(String value) {
        if (value == null || value.length() > 200 || !value.matches("[A-Za-z0-9_.$-]+")) return false;
        String lower = value.toLowerCase(Locale.ROOT);
        for (int i = 0; i + 8 <= lower.length(); i++) if (RESTRICTED.contains(digest(lower.substring(i, i + 8)))) return false;
        return true;
    }
    /** Syntax parity with the production strict international-number parser; no value is exported. */
    public static boolean internationalPhoneSyntax(String value) {
        return value != null && value.length() <= 80 && value.replaceAll("[ ()-]", "").matches("\\+[1-9][0-9]{6,14}");
    }
    /** Edge whitespace is presentation-only; this never supplies a phone identity. */
    public static String navigationTitleDigest(String value) { return digest(value.strip()); }
    public static boolean phoneMatches(String value, String expected) {
        return expected != null && expected.matches("\\+[1-9][0-9]{6,14}") && internationalPhoneSyntax(value)
            && value.replaceAll("[ ()-]", "").equals(expected);
    }
    /** Only used for an explicitly authorized test-chat capture, never an automatic event. */
    public static boolean safeTestText(String value) {
        if (value == null || value.length() > 320) return false;
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        String compact = normalized.replaceAll("[^\\p{L}\\p{N}]", "");
        for (String candidate : new String[]{normalized, compact})
            for (int i = 0; i + 8 <= candidate.length(); i++)
                if (RESTRICTED.contains(digest(candidate.substring(i, i + 8)))) return false;
        return true;
    }
    public static String redactTestText(String value) {
        if (value == null || value.length() > 320) return "[redacted]";
        StringBuilder out = new StringBuilder();
        for (String token : value.split("(?<=\\s)|(?=\\s)")) out.append(safeTestText(token) ? token : "[redacted]");
        String result = out.toString();
        return safeTestText(result) ? result : "[redacted]";
    }
    public static boolean safeNavigationLabel(String value) {
        if (value == null || value.length() > 40 || !value.matches("[\\p{L}\\p{M} _'’-]{1,40}")) return false;
        String lower = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        for (String candidate : new String[]{lower, lower.replaceAll("[ _'’-]", "")})
            for (int i = 0; i + 8 <= candidate.length(); i++)
                if (RESTRICTED.contains(digest(candidate.substring(i, i + 8)))) return false;
        return true;
    }
}
