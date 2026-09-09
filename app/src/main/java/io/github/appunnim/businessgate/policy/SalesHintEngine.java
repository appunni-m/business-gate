package io.github.appunnim.businessgate.policy;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Ephemeral text in, reason bits out. This class cannot change policy or create jobs. */
public final class SalesHintEngine {
    public static final int VERSION = 1;
    private static final String[][] PHRASES = {
        {"sales team", "sales executive", "property consultant", "loan agent", "authorised dealer", "authorized dealer"},
        {"special offer", "limited offer", "discount", "pre approved loan", "new project launch", "exclusive deal"},
        {"book now", "buy now", "schedule a demo", "reply yes", "call for details", "contact for price", "book a site visit"},
        {"price list", "our catalogue", "our catalog", "available units", "wholesale price", "bulk orders"},
        {"reply stop", "unsubscribe", "opt out"}, {}, {}, {"limited time", "last few units", "offer ends", "today only"}
    };
    private static final int[] WEIGHTS = {3, 2, 2, 2, 2, 1, 1, 1};
    private static final Pattern OTP = Pattern.compile("\\b(otp|verification code|security code|one time password|one time code)\\b");
    private static final Pattern CODE = Pattern.compile("\\b[0-9]{3,10}\\b");
    private static final Pattern MONEY = Pattern.compile("(?:\\b(?:inr|rs|usd)\\.?\\s*[0-9]|[$₹]\\s*[0-9])");
    private static final Pattern URL = Pattern.compile("\\bhttps?://[^\\s]{1,256}");
    public record Hint(int bits, int score, boolean promoted) {}
    public Hint score(CharSequence input, boolean consent, boolean allowed, boolean freshRegular, boolean forwarded) {
        if (!consent || allowed || freshRegular || forwarded || input == null) return new Hint(0, 0, false);
        String bounded = input.toString().codePoints().limit(2048).collect(StringBuilder::new,
            StringBuilder::appendCodePoint, StringBuilder::append).toString();
        if (bounded.codePoints().anyMatch(c -> Character.isLetter(c) && Character.UnicodeScript.of(c) != Character.UnicodeScript.LATIN))
            return new Hint(0, 0, false);
        String text = Normalizer.normalize(bounded, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
            .replaceAll("\\p{Cf}", "").replace('-', ' ').replaceAll("\\s+", " ");
        if (OTP.matcher(text).find() && CODE.matcher(text).find()) return new Hint(0, 0, false);
        int bits = 0, score = 0;
        for (int group = 0; group < PHRASES.length; group++) {
            boolean match = group == 5 ? MONEY.matcher(text).find() : group == 6 && URL.matcher(text).find();
            for (String phrase : PHRASES[group]) {
                if (Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(phrase) + "(?![\\p{L}\\p{N}])").matcher(text).find()) { match = true; break; }
            }
            if (match) { bits |= 1 << group; score += WEIGHTS[group]; }
        }
        boolean promoted = score >= 5 && Integer.bitCount(bits) >= 3 && (bits & 0b1011) != 0 && (bits & 0b10100) != 0;
        return new Hint(promoted ? bits : 0, score, promoted);
    }
    public static String reasons(int bits) {
        String[] labels = {"Seller wording", "Commercial offer", "Request to buy", "Catalog wording", "Opt-out wording", "Price wording", "Web link", "Time-limited wording"};
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (int i = 0; i < labels.length && count < 2; i++) if ((bits & (1 << i)) != 0) {
            if (count++ > 0) out.append(" · "); out.append(labels[i]);
        }
        return out.toString();
    }
}
