package io.github.appunnim.businessgate.policy;

import io.github.appunnim.businessgate.policy.Model.Binding;
import io.github.appunnim.businessgate.policy.Model.Choice;
import io.github.appunnim.businessgate.policy.Model.Kind;
import io.github.appunnim.businessgate.policy.NameVisibilityPolicy.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Synthetic policy requirements; no notification dismissal or pre-display protection is claimed. */
public final class NameVisibilitySuite {
    private static int checks;
    private static final Binding BINDING = new Binding("owned-installation", "a".repeat(64), "owned-profile", "owned-receiver", "owned-contract");
    private static final Scope SCOPE = new Scope(1, BINDING);
    private static final String PHONE = "+12025550100";
    private static void check(boolean condition) { checks++; if (!condition) throw new AssertionError("name visibility check " + checks); }
    private static void reject(Runnable work) {
        try { work.run(); } catch (IllegalArgumentException | UnsupportedOperationException expected) { checks++; return; }
        throw new AssertionError("missing rejection");
    }
    private static Snapshot rules(Set<String> names, Map<String, Choice> numbers) { return new Snapshot(SCOPE, 7, names, numbers); }
    private static Observation observed(String phone, String name, Kind kind) { return new Observation(SCOPE, 7, true, phone, name, kind); }
    private static void expect(Snapshot policy, Observation observation, Visibility visibility, Reason reason) {
        Decision decision = NameVisibilityPolicy.evaluate(policy, observation);
        check(decision.visibility() == visibility); check(decision.reason() == reason); check(decision.revision() == policy.revision());
    }
    public static void main(String[] args) {
        Snapshot defaults = rules(Set.of(), Map.of()), enabled = rules(Set.of("Harbor Clinic"), Map.of());
        expect(defaults, observed(PHONE, "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.HIDE, Reason.BUSINESS_NOT_ENABLED);
        for (String phone : new String[]{PHONE, "+12025550101"})
            expect(enabled, observed(phone, "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.SHOW, Reason.BUSINESS_NAME_ENABLED);
        for (String name : new String[]{"harbor Clinic", "Harbor  Clinic", "Harbor Clinic.", "Harbor Clinics", "Ｈarbor Clinic"})
            expect(enabled, observed(PHONE, name, Kind.BUSINESS_CONFIRMED), Visibility.HIDE, Reason.BUSINESS_NOT_ENABLED);
        expect(enabled, observed(PHONE, " Harbor Clinic\u00a0", Kind.BUSINESS_CONFIRMED), Visibility.SHOW, Reason.BUSINESS_NAME_ENABLED);
        expect(defaults, observed(PHONE, "Harbor Clinic", Kind.REGULAR_PROFILE_OBSERVED), Visibility.SHOW, Reason.PERSONAL_ACCOUNT);
        for (Kind kind : new Kind[]{Kind.UNKNOWN, Kind.AMBIGUOUS, null})
            expect(enabled, observed(PHONE, "Harbor Clinic", kind), Visibility.UNRESOLVED, Reason.CLASSIFICATION_UNAVAILABLE);
        expect(enabled, observed(PHONE, "Harbor Clinic", Kind.NON_DIRECT), Visibility.UNRESOLVED, Reason.NON_DIRECT_CONTEXT);
        expect(rules(Set.of(), Map.of(PHONE, Choice.ALLOW)), observed(PHONE, "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.SHOW, Reason.EXACT_NUMBER_ENABLED);
        expect(rules(Set.of("Harbor Clinic"), Map.of(PHONE, Choice.DENY_MANUAL)), observed(PHONE, "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.HIDE, Reason.EXACT_NUMBER_DENIED);
        expect(rules(Set.of("Harbor Clinic"), Map.of(PHONE, Choice.DENY_MANUAL)), observed("+12025550101", "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.SHOW, Reason.BUSINESS_NAME_ENABLED);
        expect(rules(Set.of(), Map.of(PHONE, Choice.DENY_MANUAL)), observed(PHONE, "", Kind.UNKNOWN), Visibility.HIDE, Reason.EXACT_NUMBER_DENIED);
        expect(rules(Set.of(), Map.of(PHONE, Choice.ALLOW)), observed(PHONE, "", Kind.NON_DIRECT), Visibility.UNRESOLVED, Reason.NON_DIRECT_CONTEXT);
        for (long revision : new long[]{6, 8})
            expect(enabled, new Observation(SCOPE, revision, true, PHONE, "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.UNRESOLVED, Reason.REVISION_CHANGED);
        expect(enabled, new Observation(SCOPE, 7, false, PHONE, "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.UNRESOLVED, Reason.IDENTITY_UNVERIFIED);
        expect(enabled, null, Visibility.UNRESOLVED, Reason.SCOPE_CHANGED);
        for (Scope scope : new Scope[]{new Scope(2, BINDING), new Scope(1, new Binding("other-installation", "a".repeat(64), "owned-profile", "owned-receiver", "owned-contract")), new Scope(1, new Binding("owned-installation", "a".repeat(64), "owned-profile", "other-receiver", "owned-contract")), new Scope(1, Binding.empty())})
            expect(enabled, new Observation(scope, 7, true, PHONE, "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.UNRESOLVED, Reason.SCOPE_CHANGED);
        Snapshot unbound = new Snapshot(new Scope(1, Binding.empty()), 7, Set.of("Harbor Clinic"), Map.of());
        expect(unbound, observed(PHONE, "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.UNRESOLVED, Reason.UNBOUND_SCOPE);
        for (String phone : new String[]{null, "", "12025550100", "+12025550100 ext 1"})
            expect(enabled, observed(phone, "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.UNRESOLVED, Reason.NUMBER_UNAVAILABLE);
        for (String name : new String[]{null, "", " ", "a".repeat(121), "Harbor\u200b Clinic", "Harbor\nClinic", "\ud800", "\udc00"}) {
            reject(() -> NameVisibilityPolicy.nameKey(name));
            expect(enabled, observed(PHONE, name, Kind.BUSINESS_CONFIRMED), Visibility.UNRESOLVED, Reason.NAME_UNAVAILABLE);
        }
        check(NameVisibilityPolicy.nameKey("E\u0301clair").equals("Éclair"));
        check(!NameVisibilityPolicy.nameKey("Éclair").equals(NameVisibilityPolicy.nameKey("Eclair")));
        check(NameVisibilityPolicy.nameKey("🌳".repeat(120)).codePointCount(0, 240) == 120);
        reject(() -> NameVisibilityPolicy.nameKey("🌳".repeat(121)));
        reject(() -> new Scope(0, BINDING));
        reject(() -> new Snapshot(SCOPE, -1, Set.of(), Map.of()));
        reject(() -> rules(Set.of(), Map.of(PHONE, Choice.DEFAULT)));
        reject(() -> rules(Set.of(), Map.of("+1 202 555 0100", Choice.ALLOW)));
        HashSet<String> mutableNames = new HashSet<>(Set.of("Harbor Clinic"));
        HashMap<String, Choice> mutableNumbers = new HashMap<>();
        Snapshot immutable = rules(mutableNames, mutableNumbers);
        mutableNames.clear(); mutableNumbers.put(PHONE, Choice.DENY_MANUAL);
        expect(immutable, observed(PHONE, "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.SHOW, Reason.BUSINESS_NAME_ENABLED);
        reject(() -> immutable.enabledNames().clear()); reject(() -> immutable.numberOverrides().put(PHONE, Choice.ALLOW));
        Snapshot revoked = new Snapshot(SCOPE, 8, Set.of(), Map.of());
        expect(revoked, observed(PHONE, "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.UNRESOLVED, Reason.REVISION_CHANGED);
        expect(revoked, new Observation(SCOPE, 8, true, PHONE, "Harbor Clinic", Kind.BUSINESS_CONFIRMED), Visibility.HIDE, Reason.BUSINESS_NOT_ENABLED);
        System.out.println("PASS " + checks + " name-visibility policy assertions; enforcement remains unverified");
    }
}
