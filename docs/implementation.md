# Implementation and verification

Version 0.1.0, 9 September 2026. This is an Android development implementation. A successful build, synthetic fixture or pure-rule assertion does not qualify an external application's interface.

## What is implemented

| Design area | Implementation | Evidence or boundary |
| --- | --- | --- |
| Independent identity | Original blue gate vector, native system typography, day/night palette, neutral project vocabulary | Source and APK/AAB name gate; visual inspection |
| One-page management | Native Activity and ListView; stable IDs; enabled-first groups; expandable details; review/people groups; local search; exact-number form; pending footer | Android UI harness and emulator screenshots |
| Local identity and choices | Conservative full international-number parser, normalized display search, DEFAULT/ALLOW/DENY_MANUAL choices | Pure identity/cross-product suite; actual SQLite/Android tests |
| Personal safety | Separate current classification, review hint, desired choice and observed state; unknown/personal default abstention; manual confirmation | Pure policy matrix and Android classification-isolation checks |
| Immediate choices | Main-thread per-command ALLOW veto while serial SQLite transaction commits; durable revisions; same-number mutation only | Controller journal-race tests; Android same-name isolation and nonce tests |
| Persistence and recovery | Schema constraints, WAL, one writer, one current job per account, non-destructive version refusal, installation marker, unfinished intent recovery | SQLite constraint suite; actual Android process stop and restart |
| Privacy and lifecycle | No Internet permission or runtime SDKs, bounded data, backup exclusions, explicit reset and consent withdrawal, inactive startup | Merged manifest and APK inventory audit; Android permission checks |
| Pure hint classifier | Default-off bounded English phrase groups, script/OTP/forward/kept/fresh-profile exclusions; reason bits only | Positive and negative golden cases; no live notification binding yet |
| Optional discovery | Permission disclosure, package-first listener, bounded in-memory opaque hint cache | Compiled production service; real provenance/coverage not qualified |
| Reminders and effort | Consent/permission-gated silent reminder, 30-day reservation, identity-free notification; local interval-union timing and effort dialog | Pure interval-union tests; reminder delivery/device timing still needs testing |
| Action core | Guard conjunction, per-step journal-before-dispatch, fresh reinspection, revision and identity cancellation, explicit one-shot unblock, verified-only success | Synthetic controller race, Stop, ambiguity and postcondition tests |
| Bounded sessions | One-operation controller deadline; pure batch/scan budget with deduplication, five mutation/25-second and 100 profile/120-second limits, no-progress stop | Pure budget tests; real navigation is not connected |
| Generic adapter boundary | Bundled exact-build/signing/locale registry and bounded structural field/control paths | Empty registry is enforced in artifact audit; no measured external fixture supplied |
| Release engineering | Pinned checksummed wrapper, debug APK, optimized unsigned release APK/AAB, lint and CI workflow | Local successful builds, wrapper check and actual DEX/manifest inspection |

## Verification run

The following commands were executed locally on macOS arm64 with JDK 17 and SDK 36/Build Tools 35.0.0:

```sh
scripts/test.sh
python3 scripts/check_wrapper.py
./gradlew :app:lintDebug :app:lintRelease :app:assembleDebug \
  :app:assembleDebugAndroidTest :app:assembleRelease :app:bundleRelease
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
scripts/device-tests.sh
python3 scripts/audit_artifact.py app/build/outputs/apk/release/app-release-unsigned.apk
python3 scripts/check_brand.py app/build/outputs/apk/debug/app-debug.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/bundle/release/app-release.aab
```

The pure suite currently exercises 196,721 assertions, including all 65,536 combinations of 16 required guards for each of automatic business block, explicit manual block and one-shot unblock. This is assertion coverage of the supplied contexts, not line coverage or evidence that the Android environment supplies truthful guard values. The schema suite exercises 19 constraint/recovery checks.

The native harness ran on an isolated Android API 36 arm64 emulator. Its `all` mode passed 29 persistence/permission/UI assertions; `prepare-recovery` and `verify-recovery` each passed two checks and used an actual process termination between runs. A startup synchronization issue and a test assumption about an offscreen row were corrected in the harness. No personal data or external test accounts were used.

The release runtime dependency graph reports **No dependencies**. The artifact audit inventories actual DEX definitions against the R8 mapping, allows app classes and compiler-generated record support, rejects bundled native libraries and the test harness, checks the exact permission set and backup flag, and requires the empty production registry. The app is not release-signed.

Visual review covers setup, the saved-choice/compatibility state, light and dark modes, and a 320 dp viewport at 200% font scale. Large text uses a separate toolbar action row and a short visible search hint while retaining the full accessibility description. TalkBack traversal, all OS/OEM combinations, 50,000-row performance, power-loss corruption, real device transfer and a real monthly-attention study remain unverified.

## Work that remains before real blocking

The integration registry is empty because the required input measurements are absent. The production service subscribes to an inert package and never arms. The UI stores local decisions but reports the unavailable integration. The current generic service adapter is a provisional engineering harness; measured receiver namespace binding, per-guard device evidence, qualified navigation/batching, notification provenance, full interruption/compensation behavior and real block/unblock flows still need implementation and physical validation. See [qualification](qualification.md).

Public distribution additionally needs the owner's signing setup, final identity/license choices, privacy/support endpoints, policy approval, integration review and paid-listing configuration. No store submission, paid sale, real block, unblock, message read, or external-account mutation was performed.

The design's one-minute monthly attention, broad discovery coverage and long-term blocking claims are **unmeasured**. Preserve these as release criteria, not marketing promises.
