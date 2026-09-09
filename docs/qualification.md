# Qualifying a connected-app integration

The production registry in `app/src/main/assets/adapters/compatibility.json` is empty. Adding an untested recipe is not a way to enable the product. No real connected-app actions were executed during this implementation.

## Inputs that must be measured

Use dedicated, consenting receiver and sender accounts with synthetic names and no unrelated conversations. Do not capture personal chat dumps. Qualification must use the actual production screen-access declaration, including the false accessibility-tool classification.

Each exact environment needs:

1. A verified official installation and SHA-256 identity fingerprints, exact long version code, signing certificates/lineage, OS API, locale, layout, font/display scale, navigation mode and device model.
2. A visible authoritative receiver binding, distinct from the sender's exact number. Receiver change must disarm the integration; no choice transfer between accounts, profiles, clones or installations.
3. Complete profile signatures with one-to-one context, exact-number field, authoritative business marker, explicit regular-profile signature, and current blocked/unblocked state. Missing marker alone is not evidence of a regular account.
4. Exact bounded root-to-node paths, resource suffixes, class names, expected structural labels and exclusions for report/delete or other side effects. Global text search, coordinate taps, fuzzy selectors and reading message subtrees are prohibited.
5. Unique Block/Unblock entry and confirmation controls, fresh positive postconditions and a continuous identity chain. This adapter implementation requires receiver and sender identity on every changing screen. A name-only dialog needs a separately implemented and proven binding mechanism; it is unsupported today.
6. Evidence that entry/navigation preserves unread state and read receipts, or a separate, user-authorized visible-scan route. No such navigation or historical scan is enabled in this version.
7. Measured interruption behavior: user taps/scrolls, overlay placement, lock, app switch, IME/system windows, missing fields, exact-build changes, slow UI, simultaneous policy changes and action uncertainty.
8. Fixture hashes, test video, reviewer, capture date and passing physical-device cases. Production capture/recording endpoints must not be added.

Use opaque fingerprints for external package identity; do not put restricted product names or literal package names in the repository. This is a naming constraint, not a reason to disguise a product identifier in executable code. The generic adapter receives an actual framework package name only at runtime and checks a reviewed digest.

## Adapter contract

`AdapterRegistry` only loads bundled data. Each row requires `id`, `packageSha256`, `versionCode`, `signingSha256`, `api`, `locale`, `physicallyQualified`, `evidenceSha256`, `reviewer`, `interruptionSafe`, `readStatePreserving`, and three `screens`.

Each screen role is `PROFILE`, `BLOCK_DIALOG`, or `UNBLOCK_DIALOG`. A screen has `signature`, `phone`, `receiver`, `business`, `regular`, `blocked`, `unblocked`, `blockControl`, `unblockControl` and nonempty `forbiddenControls`. A path contains `children` (bounded direct-child indices, maximum depth 12), `resourceSuffix`, `className` and `expectedText`. This restrictive format must be tested against actual structure; it is not assumed sufficient for any external interface.

Missing, conflicting or unreadable evidence yields no action. Every dispatch reacquires nodes and validates revisions and the finite session after durable intent storage. A successful click return value is not success. Only a fresh same-identity blocked/unblocked postcondition completes the job.

## Remaining implementation before enabling an adapter

These are explicit unfinished integration tasks, not assertions that a JSON row alone makes the app operational:

- Connect a chosen qualified installation to the service filter and persist a reviewed receiver/installation namespace; the current distribution intentionally has no arming path.
- Derive individual safety guards from measured device/window/receiver/overlay facts. Replace the inaccessible provisional service harness before production qualification; the pure evaluator does not establish those facts.
- Measure and wire user-started batch navigation, scan scope/read-state consent, initial discovery coverage, and the five-mutation/25-second batch cap. The current controller implements one exact-number operation per session.
- Qualify notification provenance, one-to-one candidate identity and latest incoming text extraction before connecting the optional classifier to real notification content.
- Prove live Stop and interaction ordering on real devices. The current conservative service aborts on click/scroll events and may also abort on its own framework click events. No optimistic timing exception is approved.
- Complete physical recovery/compensation, receiver changes, backup transfer, large database and accessibility tests. A target app update or unexpected dialog must keep the integration inactive.

## Paid-release gates

The release owner must supply signing credentials, final owned app identity, merchant pricing, support contact, public privacy URL, distribution approval and integration review. The 30-day attention target needs a consented cohort study that includes decision time, visible automation waiting, compatibility incidents and repair time. Current local timers are a lower bound, not evidence for a one-minute claim.

Any wrong-person/allowed-number block, false success, report/delete/send side effect, receiver confusion or permission bypass prevents release. Do not sell an unavailable blocking promise or replace it with notification muting.
