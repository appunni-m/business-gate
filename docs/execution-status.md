# Completion plan execution status

Updated 10 September 2026. The [plan](implementation-plan.md) is **partially implemented, not complete**. Independently testable storage, authority, controller, UI and delivery defects are fixed. The production registry remains empty; connected-app actions remain unavailable.

The receiver is reported to be signed in on the existing API 36 emulator. A consenting sender and physical qualification device are not available. No receiver message content was inspected, no real message was sent, and no external block/unblock or physical qualification result is claimed. A spare phone with a second test number, or an explicitly consenting test participant, can supply the sender. Synthetic fixtures cannot establish real delivery effects.

## Recorded engineering checks

- Pure suite: 196,817 policy/controller assertions plus 310 owned structural assertions; 31 SQLite schema/migration checks; 193 document-qualified acceptance references validated. These counts do not establish 193 completed acceptance cases.
- Android harness: 183 persistence, permission, recovery and UI checks; 79 performance/capacity and timing-probe checks; 14 checks across visible-effort and entry/confirmation process-restart preparation and verification. Includes receiver isolation, stale verification, completed-job reinspection, Stop/reset/consent races, corruption-file preservation and rejected downgrade with choices intact.
- API 36 arm64 emulator performance: 10,000 synthetic records, 20 completed queries, recorded p95 10 ms and database plus WAL 3,594,376 bytes; 50,000-record snapshot plus search projection loaded in 3,134 ms in the latest local run. These are environment-specific emulator observations. The 50,000-record boundary retains a new explicit ALLOW choice by evicting optional cache only. Physical scrolling and phone performance remain unmeasured.
- The v1 migration fixture matches the signed predecessor's schema byte-for-byte. Build `100501` APK SHA-256: `3e850a5a10fcf503886bd06b56d2b96c81fa3b9141da62dc004781f3e4f50629`.
- Separate release probe: 21 preparation and 10 verification checks pass against signed build `100501` through reinstallation. This proves the probe baseline; the delivery job must pass predecessor-to-new-build upgrade on API 29 before publishing this change.
- Thirteen offline delivery tests cover drafts, immutable assets, interrupted rolling uploads, stale-version protection, compatibility rejection, missing signing input, certificate mismatch and partial signing failure. No real signing credential is read by these tests.
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
| F06 | Partial | Actual v1 migration preserves choices and revokes old bindings/grants; corruption preserves files; downgrade rejects safely. SQLite worker-death and quota/recovery checks pass locally; the Android WAL quota rollback and recovery regression passes locally. |
| F07 | Partial | Stop/ALLOW/data epochs and callback ownership; reset and consent/switch races tested. Full live dispatch cancellation matrix remains. |
| F08 | Partial | Sixteen named guards; each missing bit rejects pure dispatch. Android fact derivation and window behavior remain unqualified. |
| F09 | Partial | Exact paths, owned adversarial structural fixtures, a shared 250-node callback ceiling, acquired-node cleanup and 160-code-point label rejection implemented. Full measured control-region uniqueness and real fixtures remain. |
| F10 | Partial | Final dispatch evaluates current policy against the same acquired screen/control used for the click. Pure classification, identity, window, revision, ALLOW and guard replacement regressions pass. Real last-moment replacement tests remain. |
| F11 | Partial | Old callbacks fenced; 2.5-second transition and eight-second attempt deadlines added; intermediate events do not repeat entry. Own-action versus user-input attribution remains conservative and unfinished. |
| F12 | Partial | Completion waits for durable same-command acknowledgement; stale completion cannot consume newer ALLOW. Live postconditions remain unqualified. |
| F13 | Partial | Settled jobs requeue; bounded attempts/backoffs, explicit Retry, preserved grants and durable circuit recovery pass the CI repository suite. Actual entry/confirmation process-death checks pass locally; real external crash boundaries remain unqualified. |
| F14 | Partial | Immediate ALLOW veto, nonce/revision/expiry and ownership checks strengthened. Live after-dispatch compensation remains unfinished. |
| F15 | Partial | Stop geometry, finite lease and lock/lifecycle stops implemented. Reachability, TalkBack, interruptions and foreground matrix remain unrun. |
| F16 | Blocked | No real block/unblock, independent sender delivery check or physically qualified final APK. |
| F17 | Partial | Foreground observations can update the active namespace and queue work. Passive coverage and classification transitions need measured tests. |
| F18 | Blocked | One current profile per finite session is wired. Navigation, five-change batches and separately consented scans need implementation after measurement. |
| F19 | Blocked | Opaque bounded notification cache is scoped and cleared. Provenance/candidate consumer and discovery bridge remain unimplemented. |
| F20 | Partial | Bounded classifier, immediate option veto, global consent withdrawal and metadata cleanup. Qualified incoming-text and review-promotion integration remain unimplemented. |
| F21 | Partial | Reminder rechecks consent/options, namespace, permission/channel and 30-day cap. Device delivery/revocation/clock matrix remains. |
| F22 | Partial | Monotonic intervals, UTC splitting, union accounting and visible-only checkpoints implemented. Checkpoint process-death tests pass locally; scan/repair attention and pilot remain. |
| F23 | Partial | Runtime disarm, failed-job copy, receiver/local-choice management and readiness requests improved. Storage-error refresh no longer loops; committed choices remain searchable. Explicit non-destructive storage retry and the full setup/recovery state machine remain. |
| F24 | Partial | Stable IDs, stale-search invalidation, namespace result clearing, scrollable dialogs and plurals. Complete visual/TalkBack/layout matrix remains. |
| F25 | Partial | Write-time retention, account ceiling and optional-cache eviction; all-protected capacity rejection and continued editing of existing choices pass locally. Cached committed-snapshot search is implemented. Physical scrolling/performance and the CI rerun remain. |
| F26 | Partial | Artifact/runtime/permission/backup audit, reset/restart and corruption/downgrade checks. Full permission/reboot/disk-full/intent/privacy matrix remains. |
| F27 | Implemented for inactive distribution | Capability/support metadata derive from validated APK evidence. No nonempty measured qualification has been supplied. |
| F28 | Partial | Separate signed-upgrade probe, publisher-only key use, cleanup and offline failure regressions. New CI upgrade must pass; complete grant/namespace and fault matrix remains. |
| F29 | Implemented | Signing, installation, migration, capability and provenance documents aligned. Owner license and support/privacy endpoints remain explicit owner decisions. |
| F30 | Blocked | Physical attempt/delivery/coverage matrix and 30-day pilot have not started. No product-rate or monthly-effort target is claimed. |

## Continuing recovery implementation

Commit `715a4d9` passed the [recovery branch's Android verification](https://github.com/appunni-m/business-gate/actions/runs/34437281467). It adds durable attempt counts, a three-attempt limit, 30-second/five-minute backoffs, independent 2.5-second transition and eight-second attempt deadlines, and persisted uncertain-result reasons. Explicit Retry reopens inspection without creating an unblock grant. Structural/identity anomalies persist a circuit that requires an explicit, observe-only compatibility check. A targeted Retry or Unblock now keeps the requested number pinned; an unblock-only session preserves the paused business rule. This proves engineering behavior on the CI emulator, not live connected-app qualification.

Visible effort now checkpoints approximately every five seconds while management or session work is active. Metric writes do not reload account lists. Reset invalidates queued old metrics and clears pre-reset intervals; receiving-account switches do not erase already measured global effort. No timer resumes on process startup. Android scheduling and uncommitted writes can still lose the latest duration during a crash, so the displayed totals remain a local lower bound. The process-death checkpoint tests pass on the local API 36 emulator.

The first checkpoint CI run passed 164 core Android assertions but timed out at the 50,000-record snapshot callback. The account projection now reads only required columns by fixed index. The next CI run exposed an ineffective quota fixture: Android WAL connection pooling meant the quota needed to be set inside the writer transaction. With that fixture corrected, all eight harness stages pass locally, including the production migration method under a real SQLite page quota, preserved ALLOW after rollback and successful migration after capacity returns. The snapshot timeout remains ten seconds; the measured local load is 3,098 ms. CI must repeat these checks before delivery.

The local emulator restarted after host disk space became available; its saved receiver account data is preserved. The owned debug harness is isolated from the signed app and receiver application. Pure tests, lint, debug/release builds, bundle, probe build, wrapper, actionlint, source/artifact branding and release audit pass. Build `100701` previously passed GitHub verification/delivery and a local signed predecessor upgrade; recovery commit `1569b33` passed GitHub verification but delivery stopped during the added external-intent test against the predecessor. The previous download remains unchanged.

The next integration milestone is F02: measure a safe one-to-one profile and confirmation route with verified receiver identity and an identified consenting sender. If that route cannot establish fresh authority and avoid unwanted side effects, it must remain unsupported. A guessed compatibility row cannot complete this work.

## Structural dispatch and delivery follow-up

The next patch shares a 250-node acquisition budget across each service callback, caches exact path prefixes within a single inspection and releases all acquired nodes when that inspection ends. No node is retained for an asynchronous journal callback. Owned structural fixtures test hidden/foreign/replaced ancestors, changed child counts, unexpected control labels, bounded text and budget exhaustion without walking unrelated subtrees. The service's final click now derives its frame and checks the latest policy using the same acquired screen. The production registry remains empty; this does not qualify a real interface or prove arbitrary control-region uniqueness.

The new signed-upgrade intent probe passed locally against build `100701`, but the API 29 CI run failed after launching the replacement Activity. The probe now waits for a different Business Gate window and verifies that each input value is applied, addressing a suspected Activity-recreation race. The revised probe passes 32 preparation and 18 verification assertions locally against the signed build `100701`; all eight debug harness stages also pass on the final structural build. The API 29 rerun must establish whether this correction resolves the CI failure; no passing new signed upgrade is claimed yet.

## Storage, search and capacity follow-up

The structural CI run passed its 171 core Android assertions but failed the 100 ms completed-query target; its public error did not include the measured value. The reporter now exposes bounded numeric metrics for either outcome. Search now uses an immutable normalized projection of the last committed records on the serial worker, replacing repeated SQLite scans for every query. Writes and namespace changes replace the projection before their UI notification; it also keeps saved choices searchable while storage is unavailable. The latency target and ten-second snapshot timeout are unchanged.

The final local harness passes 183 core assertions, 59 performance/capacity assertions and all six effort/restart preparation and verification stages. It makes the account table unavailable to prove that the UI does not loop on read errors, saved exact-number choices remain searchable, and failed ALLOW writes retain their immediate veto without a false success callback. At 50,000 protected records, a new insertion is rejected visibly, no protected choice is evicted, existing choices remain editable and actions stay disarmed. A full non-destructive storage retry flow remains unfinished. The signed APK rerun must pass before a new download replaces build `100701`.

## Search projection cost correction

Commit `689d6dc` passed 183 CI core Android assertions and the 10,000-record query target at 57 ms p95, but its 50,000-record snapshot exceeded ten seconds. The snapshot was normalizing every account name again while building its search cache. The correction reads the already persisted `search_key` in the account projection and reuses it in the immutable cache. No schema change or raised timeout is required. The next exact-commit CI run must verify the correction before signed delivery.

The stored-key CI run passed 183 core assertions but measured 168 ms query p95. Search now uses direct arrays and string-index matching instead of per-record iterator/wrapper calls, and returns the existing immutable list for an empty query. The native harness adds test-only serial-worker timing probes to separate queue delay, worker wall/CPU time and full callback latency. Locally, all stages pass: query p95 10 ms, worker CPU/wall p95 8 ms, queue p95 0 ms, snapshot 3,134 ms. The 79 performance/capacity checks include 20 timing-probe completions. No threshold is raised; CI qualification is still pending.
