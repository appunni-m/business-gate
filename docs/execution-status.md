# Completion plan execution status

Updated 10 September 2026. The [plan](implementation-plan.md) is **partially implemented, not complete**. Independently testable storage, authority, controller, UI and delivery defects are fixed. The production registry remains empty; connected-app actions remain unavailable.

The receiver is reported to be signed in on the existing API 36 emulator. A consenting sender and physical qualification device are not available. No receiver message content was inspected, no real message was sent, and no external block/unblock or physical qualification result is claimed. A spare phone with a second test number, or an explicitly consenting test participant, can supply the sender. Synthetic fixtures cannot establish real delivery effects.

## Recorded engineering checks

- Pure suite: 196,756 assertions; 21 SQLite schema/migration checks; 193 document-qualified acceptance references validated. These counts do not establish 193 completed acceptance cases.
- Android harness: 80 persistence, permission, recovery and UI checks; 50 performance/capacity checks; four actual process-restart checks. Includes receiver isolation, stale verification, completed-job reinspection, Stop/reset/consent races, corruption-file preservation and rejected downgrade with choices intact.
- API 36 arm64 emulator performance: 10,000 synthetic records, 20 completed queries, recorded p95 20 ms and database plus WAL 3,800,376 bytes. The 50,000-record boundary retains a new explicit ALLOW choice by evicting optional cache only. Physical scrolling and phone performance remain unmeasured.
- The v1 migration fixture matches the signed predecessor's schema byte-for-byte. Build `100501` APK SHA-256: `3e850a5a10fcf503886bd06b56d2b96c81fa3b9141da62dc004781f3e4f50629`.
- Separate release probe: 21 preparation and 10 verification checks pass against signed build `100501` through reinstallation. This proves the probe baseline; the delivery job must pass predecessor-to-new-build upgrade on API 29 before publishing this change.
- Ten offline delivery tests cover drafts, immutable assets, interrupted rolling uploads, stale-version protection, compatibility rejection, missing signing input, certificate mismatch and partial signing failure. No real signing credential is read by these tests.
- Android lint, debug/release APKs, bundle, probe build, wrapper, actionlint, source/artifact branding and release audit are required before pushing; CI repeats them on its exact commit and audits the signed distributable.

Local command output is retained under `/tmp/business-gate/` during execution. It is not physical evidence. GitHub Actions retains logs and reports according to [delivery.md](delivery.md). The [acceptance ledger](acceptance-ledger.json) records engineering coverage without assigning unrun physical cases a pass.

## Disposition of every fix

“Partial” means code is implemented but the full exit condition is not met. “Blocked” identifies missing measurements or test inputs, not a successfully qualified route.

| Fix | Status | Implemented and remaining work |
| --- | --- | --- |
| F01 | Partial | All 96 full and 97 companion cases inventoried with references. Complete scenario artifacts and physical results remain unrun. |
| F02 | Blocked | Baseline incompatibility recorded. Receiver/sender route, dialogs, unread effects and physical environment still need measurement. |
| F03 | Partial | Bounded registry, device/build/signing-history and evidence-hash validation. Nonempty measured fixtures and environment rejection matrix remain. |
| F04 | Partial | Installation picker, receiver review, readiness token and finite profile-session dispatch wired. Supported activation journey needs F02/F03. |
| F05 | Partial | Receiver/installation isolation, generation-bound writes and unconnected choices tested. Real account/clone/restore transitions remain unqualified. |
| F06 | Partial | Actual v1 migration preserves choices and revokes old bindings/grants; corruption preserves files; downgrade rejects safely. Disk-full and interrupted migration matrix remain. |
| F07 | Partial | Stop/ALLOW/data epochs and callback ownership; reset and consent/switch races tested. Full live dispatch cancellation matrix remains. |
| F08 | Partial | Sixteen named guards; each missing bit rejects pure dispatch. Android fact derivation and window behavior remain unqualified. |
| F09 | Partial | Bounded paths, ancestors, class/text/identity and visible forbidden-control checks. Adversarial structural fixtures, total callback node budget and measured fixtures remain. |
| F10 | Partial | Specific entry/confirmation control and renewed identity/revision/window/policy checks. Real last-moment replacement tests remain. |
| F11 | Partial | Old failure callbacks/deadlines fenced; intermediate events do not repeat entry. Own-action versus user-input attribution remains conservative and unfinished. |
| F12 | Partial | Completion waits for durable same-command acknowledgement; stale completion cannot consume newer ALLOW. Live postconditions remain unqualified. |
| F13 | Partial | Settled jobs requeue from fresh evidence; idempotence and actual restart reinspection tested. Bounded Retry, uncertain circuit recovery and every crash boundary remain. |
| F14 | Partial | Immediate ALLOW veto, nonce/revision/expiry and ownership checks strengthened. Live after-dispatch compensation remains unfinished. |
| F15 | Partial | Stop geometry, finite lease and lock/lifecycle stops implemented. Reachability, TalkBack, interruptions and foreground matrix remain unrun. |
| F16 | Blocked | No real block/unblock, independent sender delivery check or physically qualified final APK. |
| F17 | Partial | Foreground observations can update the active namespace and queue work. Passive coverage and classification transitions need measured tests. |
| F18 | Blocked | One current profile per finite session is wired. Navigation, five-change batches and separately consented scans need implementation after measurement. |
| F19 | Blocked | Opaque bounded notification cache is scoped and cleared. Provenance/candidate consumer and discovery bridge remain unimplemented. |
| F20 | Partial | Bounded classifier, immediate option veto, global consent withdrawal and metadata cleanup. Qualified incoming-text and review-promotion integration remain unimplemented. |
| F21 | Partial | Reminder rechecks consent/options, namespace, permission/channel and 30-day cap. Device delivery/revocation/clock matrix remains. |
| F22 | Partial | Management/session monotonic intervals, UTC splitting and union accounting. Process-loss persistence, scan/repair attention and pilot remain. |
| F23 | Partial | Runtime disarm, failed-job copy, receiver/local-choice management and readiness requests improved. Full setup/retry/recovery state machine remains. |
| F24 | Partial | Stable IDs, stale-search invalidation, namespace result clearing, scrollable dialogs and plurals. Complete visual/TalkBack/layout matrix remains. |
| F25 | Partial | Write-time retention, account ceiling and optional-cache eviction; emulator query/capacity checks pass. Physical scrolling and all-protected capacity evidence remain. |
| F26 | Partial | Artifact/runtime/permission/backup audit, reset/restart and corruption/downgrade checks. Full permission/reboot/disk-full/intent/privacy matrix remains. |
| F27 | Implemented for inactive distribution | Capability/support metadata derive from validated APK evidence. No nonempty measured qualification has been supplied. |
| F28 | Partial | Separate signed-upgrade probe, publisher-only key use, cleanup and offline failure regressions. New CI upgrade must pass; complete grant/namespace and fault matrix remains. |
| F29 | Implemented | Signing, installation, migration, capability and provenance documents aligned. Owner license and support/privacy endpoints remain explicit owner decisions. |
| F30 | Blocked | Physical attempt/delivery/coverage matrix and 30-day pilot have not started. No product-rate or monthly-effort target is claimed. |

The next integration milestone is F02: measure a safe one-to-one profile and confirmation route with verified receiver identity and an identified consenting sender. If that route cannot establish fresh authority and avoid unwanted side effects, it must remain unsupported. A guessed compatibility row cannot complete this work.
