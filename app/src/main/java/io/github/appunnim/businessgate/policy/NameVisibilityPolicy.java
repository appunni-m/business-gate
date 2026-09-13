package io.github.appunnim.businessgate.policy;

import io.github.appunnim.businessgate.policy.Model.Binding;
import io.github.appunnim.businessgate.policy.Model.Choice;
import io.github.appunnim.businessgate.policy.Model.Kind;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Desired visibility only. A HIDE decision is neither dispatch authority nor an enforcement result. */
public final class NameVisibilityPolicy {
    private NameVisibilityPolicy() {}
    public enum Visibility { SHOW, HIDE, UNRESOLVED }
    public enum Reason {
        EXACT_NUMBER_ENABLED, EXACT_NUMBER_DENIED, BUSINESS_NAME_ENABLED, BUSINESS_NOT_ENABLED,
        PERSONAL_ACCOUNT, UNBOUND_SCOPE, SCOPE_CHANGED, REVISION_CHANGED, IDENTITY_UNVERIFIED,
        NON_DIRECT_CONTEXT, NUMBER_UNAVAILABLE, CLASSIFICATION_UNAVAILABLE, NAME_UNAVAILABLE
    }
    public record Decision(Visibility visibility, Reason reason, long revision) {}
    public record Scope(long namespace, Binding binding) {
        public Scope { if (namespace < 1) throw new IllegalArgumentException("INVALID_NAMESPACE"); Objects.requireNonNull(binding); }
    }
    public record Snapshot(Scope scope, long revision, Set<String> enabledNames, Map<String, Choice> numberOverrides) {
        public Snapshot {
            Objects.requireNonNull(scope);
            if (revision < 0 || enabledNames == null || numberOverrides == null || enabledNames.size() > 50_000 || numberOverrides.size() > 50_000)
                throw new IllegalArgumentException("INVALID_POLICY_SNAPSHOT");
            Set<String> names = new HashSet<>();
            for (String name : enabledNames) names.add(nameKey(name));
            Map<String, Choice> numbers = new HashMap<>();
            for (var entry : numberOverrides.entrySet()) {
                if (!Identity.canonicalPhone(entry.getKey()).equals(entry.getKey()) || (entry.getValue() != Choice.ALLOW && entry.getValue() != Choice.DENY_MANUAL))
                    throw new IllegalArgumentException("INVALID_NUMBER_OVERRIDE");
                numbers.put(entry.getKey(), entry.getValue());
            }
            enabledNames = Set.copyOf(names); numberOverrides = Map.copyOf(numbers);
        }
    }
    /** Identity and classification must be supplied by a separately verified integration, never inferred from a name. */
    public record Observation(Scope scope, long policyRevision, boolean identityVerified, String phone, String businessName, Kind kind) {}

    public static Decision evaluate(Snapshot policy, Observation observation) {
        Objects.requireNonNull(policy);
        long revision = policy.revision();
        if (!policy.scope().binding().bound()) return unresolved(Reason.UNBOUND_SCOPE, revision);
        if (observation == null || !policy.scope().equals(observation.scope())) return unresolved(Reason.SCOPE_CHANGED, revision);
        if (observation.policyRevision() != revision) return unresolved(Reason.REVISION_CHANGED, revision);
        if (!observation.identityVerified()) return unresolved(Reason.IDENTITY_UNVERIFIED, revision);
        if (observation.kind() == Kind.NON_DIRECT) return unresolved(Reason.NON_DIRECT_CONTEXT, revision);
        String phone;
        try { phone = Identity.canonicalPhone(observation.phone()); }
        catch (IllegalArgumentException invalid) { return unresolved(Reason.NUMBER_UNAVAILABLE, revision); }
        Choice exact = policy.numberOverrides().get(phone);
        if (exact == Choice.ALLOW) return new Decision(Visibility.SHOW, Reason.EXACT_NUMBER_ENABLED, revision);
        if (exact == Choice.DENY_MANUAL) return new Decision(Visibility.HIDE, Reason.EXACT_NUMBER_DENIED, revision);
        if (observation.kind() == Kind.REGULAR_PROFILE_OBSERVED) return new Decision(Visibility.SHOW, Reason.PERSONAL_ACCOUNT, revision);
        if (observation.kind() != Kind.BUSINESS_CONFIRMED) return unresolved(Reason.CLASSIFICATION_UNAVAILABLE, revision);
        String name;
        try { name = nameKey(observation.businessName()); }
        catch (IllegalArgumentException invalid) { return unresolved(Reason.NAME_UNAVAILABLE, revision); }
        return policy.enabledNames().contains(name)
            ? new Decision(Visibility.SHOW, Reason.BUSINESS_NAME_ENABLED, revision)
            : new Decision(Visibility.HIDE, Reason.BUSINESS_NOT_ENABLED, revision);
    }
    private static Decision unresolved(Reason reason, long revision) { return new Decision(Visibility.UNRESOLVED, reason, revision); }

    /** Exact permission spelling; deliberately independent of the accent-insensitive search key. */
    public static String nameKey(String raw) {
        if (raw == null || raw.length() > 512) throw new IllegalArgumentException("INVALID_BUSINESS_NAME");
        for (int offset = 0; offset < raw.length();) {
            int point = raw.codePointAt(offset), type = Character.getType(point);
            if (Character.isISOControl(point) || type == Character.FORMAT || type == Character.SURROGATE)
                throw new IllegalArgumentException("INVALID_BUSINESS_NAME");
            offset += Character.charCount(point);
        }
        String composed = Normalizer.normalize(raw, Normalizer.Form.NFC);
        int start = 0, end = composed.length();
        while (start < end && edgeSpace(composed.codePointAt(start))) start += Character.charCount(composed.codePointAt(start));
        while (end > start && edgeSpace(composed.codePointBefore(end))) end -= Character.charCount(composed.codePointBefore(end));
        String key = composed.substring(start, end);
        int length = key.codePointCount(0, key.length());
        if (length < 1 || length > 120) throw new IllegalArgumentException("INVALID_BUSINESS_NAME");
        return key;
    }
    private static boolean edgeSpace(int point) { return Character.isWhitespace(point) || Character.isSpaceChar(point); }
}
