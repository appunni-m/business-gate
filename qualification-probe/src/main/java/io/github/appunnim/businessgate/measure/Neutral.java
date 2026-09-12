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
}
