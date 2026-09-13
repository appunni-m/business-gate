# Implementation and verification

Updated 13 September 2026. This remains an Android development implementation with **connected-app actions disabled**. The compatibility registry is empty. Building, installing, or passing synthetic tests does not establish physical compatibility.

## Changes implemented from the completion plan

The [business-name visibility design](name-visibility-design.md) defines the current requested behavior and unresolved platform dependency. The earlier [completion plan](implementation-plan.md) records the retained implementation foundation. The [acceptance ledger](acceptance-ledger.json) keeps the full design's 96 cases separate from the companion's 97 cases. Engineering coverage is explicitly distinguished from a full acceptance pass.

| Area | Current implementation | Verification boundary |
| --- | --- | --- |
| Identity and choices | Original blue gate identity, native one-page UI, exact-number choices and independent receiver namespaces | Pure policy and actual Android database/UI tests; live receiver extraction unqualified |
| Migration | Transactional upgrades from actual shipped schema versions 1 and 2 to version 3; number choices preserved; no automatic name grants | Actual Android helper upgrades, interruption/rollback and SQLite schema checks |
| Business names | Canonical exact-name permission policy, scoped durable enable/disable choices, native forms, failed-save and stale-write handling | Pure policy plus native database/UI/restart tests; no notification or entry enforcement is wired |
| Cancellation | Separate data and authority generations; immediate Stop/ALLOW veto; callbacks cannot rearm an old lease or repopulate a reset namespace | Deterministic writer-held Android tests and pure callback regressions |
| Action state | Durable verification acknowledgement, action/identity/revision/grant matching, idempotent observation completion, settled block-job reconciliation | Pure controller and Android repository tests; external dispatch/postconditions unqualified |
| Compatibility | Bounded exact-environment registry, signing-history checks, reviewed evidence and artifact hashes, structural ancestor validation | Source/artifact gates; no nonempty production contract is qualified |
| Execution boundary | Named guard observations, specific entry/confirmation dispatch, live control/identity geometry checks, finite visible sessions, fenced deadlines | Compiles and passes lint; physical window/interaction behavior remains unrun |
| Connection UI | System installation picker, explicit receiver review, readiness-based activation and current-profile session request | Inactive with the empty registry; complete live journey remains unqualified |
| Optional features | Bounded pure hint classifier, notification-cache cleanup, reminder permission/consent recheck, separate management/session effort intervals with UTC day splitting | Pure timing/hint tests; notification provenance, delivery races, and physical attention measurements remain incomplete |
| UI and capacity | Stable search/list IDs, immediate invalidation of old search results, scrollable custom dialogs, failed-job copy, bounded accounts with optional-cache eviction before rejecting a new explicit choice | Native UI and large-database harness; physical accessibility/device matrix remains unrun |
| Delivery | Signed GitHub APKs, certificate pinning, immutable versioned releases, rolling download, source provenance, and a separate upgrade probe using its own test key | Existing public delivery verified; new workflow checks must pass on the corresponding source commit |

The initial installed-release activation test used version `0.1.0-dev.5.1` (`100501`). Startup and screen-access binding passed; Resume displayed compatibility help. No block/unblock was performed. That result remains the baseline, not evidence that the new live path is qualified.

## Run the checks

Use Java 17 and the SDK configuration in the [README](../README.md). Before pushing an implementation change:

```sh
scripts/test.sh
python3 scripts/check_wrapper.py
./gradlew :app:lintDebug :app:lintRelease :app:assembleDebug \
  :app:assembleDebugAndroidTest :app:assembleRelease :app:bundleRelease \
  :release-probe:lintDebug :release-probe:assembleDebug
python3 scripts/audit_artifact.py app/build/outputs/apk/release/app-release-unsigned.apk
python3 scripts/check_brand.py app/build/outputs/apk/debug/app-debug.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/bundle/release/app-release.aab \
  release-probe/build/outputs/apk/debug/release-probe-debug.apk
```

On an isolated emulator, set `GATE_TEST_SERIAL` and run `scripts/device-tests.sh`. The harness resets only the debug application and uses synthetic records. It exercises persistence, namespaces, cancellation, migration, search/UI, 10,000-record queries, the 50,000-account boundary, and actual process termination/restart. If the installed debug APK has a higher version code, build with an appropriate `-PgateVersionCode`; do not delete saved data to avoid an upgrade check.

The separate `release-probe` module is not a dependency of the application. `scripts/release-upgrade-test.sh` installs the probe on a dedicated emulator, seeds a fictional ALLOW choice through the previous signed app's own UI, installs the new signed APK with `adb install -r`, and verifies the choice. It removes the probe afterward. It never inspects the connected app or receives publisher credentials.

Recovery now counts entry attempts durably, enforces later-opportunity backoffs, and records uncertainty without replacing a newer command. Explicit Retry preserves an existing unblock nonce and its expiry; an unblock-only session does not turn on the business rule. An identity/structure circuit survives reload and requires a separate observe-only compatibility check. The Android harness verifies these repository transitions. Real event attribution and external compensation still require measured integration work.

Effort tracking checkpoints only while management or a session is visible. It does not schedule an alarm or resume a previous interval after restart. Checkpoints update only the metric table and remain a lower bound when the process or a write is interrupted. The native harness separately terminates the process at entry/confirmation recovery boundaries and during an observed effort interval.

Screen inspection now owns and releases its root and every acquired descendant. Exact path prefixes share a cache within one inspection, and all inspections in a service callback share a 250-node ceiling. Unrelated subtrees are never searched. The final dispatch gate checks the latest durable policy against the same freshly acquired screen that supplies the action node; a changed personal/unknown/group context, receiver, number, window, revision or guard cancels the action. Owned structural tests establish these engineering boundaries without qualifying a connected interface.

Search runs on the serial worker against an immutable projection of the last committed records, reusing the normalized search keys already stored with each account. Publication replaces that projection before notifying the UI. Queries preserve exact-number matching, literal wildcard text and accent folding without a fresh SQLite scan. During storage failure the projection remains searchable and cannot trigger a database-error refresh loop. The all-protected 50,000-account case rejects a new record without evicting durable choices; saved choices remain editable and actions remain disarmed.

## Remaining implementation and evidence

The integration is still incomplete. Measured receiver/profile/dialog contracts, reliable own-action versus user-interaction handling, full navigation/batching/scanning, notification provenance and scoring integration, safe recovery/compensation across real external transitions, and physical block/unblock outcomes are not qualified. Some of these still require implementation once the actual interface is measured; JSON metadata alone cannot complete them.

The service conservatively stops on click/scroll events. No optimistic exception for its own clicks is enabled. A target whose event ordering cannot meet the safe contract remains unsupported. Current sessions operate on a visible profile; qualified navigation and multi-profile batches are unfinished.

TalkBack behavior, the complete physical OS/OEM/layout matrix, transfer/corruption recovery, real sender delivery/read receipts, discovery coverage, and the monthly attention pilot remain unrun. See [qualification](qualification.md). Do not claim a completed blocker, discovery percentage, or one-minute monthly effort from the engineering suite.

GitHub delivery is already release-signed. Local Gradle release outputs remain unsigned. The configured repository signing secret does not need to be recreated. No store sale/publication has occurred, and the owner has not selected an open-source license or final public support/privacy endpoints.
