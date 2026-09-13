# Completion plan execution status

Updated 13 September 2026. The [plan](implementation-plan.md) is **partially implemented, not complete**. Independent fixes have been implemented in storage, authority, controller, UI and delivery code; each remaining acceptance gap is listed below. The production registry remains empty; connected-app actions remain unavailable.

The owner authorized the [design and completion plan](completion-design-plan.md) on 10 September 2026. The [execution record](design-execution.md) tracks the current row-focus, dialog/lifecycle, IME/Back and delivery work. The [workflow review](workflow-review.md) remains the read-only baseline review of successful run 20 and build `102001`; it is not a claim that later changes have already passed CI.

The receiver’s own profile and accessible phone field were measured on the persistent API 36 emulator on 12 September; see the [receiver follow-up](design-execution.md#receiver-profile-measurement-follow-up--12-september-2026). The field is not yet bound by a qualified production adapter. The owner subsequently supplied an exact business number and authorized two outgoing tests and inspection of that chat. Both tests displayed Delivered, with matching welcome/support responses; see the [sanitized live test](measurements/outgoing-test-api36.json) and [measurement scope](measurement-tool.md). A controlled sender for timed blocked/unblocked delivery trials and a physical qualification device remain unavailable. No external block/unblock or physical qualification result is claimed.

## Recorded engineering checks

This subsection preserves the earlier committed baseline. Current design work, expanded test counts and the subsequent release checkpoint are tracked in [the execution record](design-execution.md).

- Pure suite: 196,833 policy/controller assertions plus 310 owned structural assertions; 31 SQLite schema/migration checks; 28 declared text contrast pairs (minimum 5.44:1); 193 document-qualified acceptance references validated. These counts do not establish 193 completed acceptance cases.
- Android harness: 253 persistence, permission, recovery and UI checks; 20 Activity/search/anchor restoration checks; 32 committed-reset, rollback and receiver-publication checks; 80 performance/capacity and timing-probe checks; 20 checks across visible-effort and entry/confirmation process-restart preparation and verification. Includes receiver isolation, stale verification, completed-job reinspection, Stop/reset/consent races, corruption-file preservation and rejected downgrade with choices intact.
- API 36 arm64 emulator performance: 10,000 synthetic records, 20 completed queries, recorded p95 19 ms and database plus WAL 3,812,736 bytes; 50,000-record snapshot plus search projection loaded in 3,399 ms in the compact-header follow-up. These are environment-specific emulator observations. The 50,000-record boundary retains a new explicit ALLOW choice by evicting optional cache only. Physical scrolling and phone performance remain unmeasured.
- The v1 migration fixture matches the signed predecessor's schema byte-for-byte. Build `100501` APK SHA-256: `3e850a5a10fcf503886bd06b56d2b96c81fa3b9141da62dc004781f3e4f50629`.
- Separate release probe: 32 preparation and 18 verification checks pass for the actual signed `101501` → `101601` upgrade on API 29 CI and the API 36 local emulator. The delivery job repeats this check before publishing each new build.
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
| F21 | Partial | Actual owned notification denial/grant, silent/private delivery, blocked channel, 30-day/future-clock boundaries and consent withdrawal are exercised in the debug harness. Qualified incoming-notification provenance and full repeated-prompt/physical behavior remain. |
| F22 | Partial | Monotonic intervals, UTC splitting, union accounting and visible-only checkpoints implemented. Checkpoint process-death tests pass locally; scan/repair attention and pilot remain. |
| F23 | Partial | Runtime disarm, failed-job copy, receiver/local-choice management and readiness requests improved. Storage-error refresh no longer loops; committed choices remain searchable. Explicit non-destructive storage checks and revision-bound unsaved-choice retry are implemented and covered by owned fault regressions. The full setup/recovery state matrix remains. |
| F24 | Partial | Reproduced and fixed row focus loss, compact viewports, clipped dialogs, missing native Back dispatch and repeated/background announcements. Native focus, actual TalkBack Tab traversal, IME/Back, stale dialog callbacks and saved-task process death have measured engineering coverage. The expanded 24-case and compact two-navigation suites retain full-text and target-size requirements. See the execution record for current results; complete speech/reading order and physical accessibility remain unverified. |
| F25 | Partial | Write-time retention, account ceiling and optional-cache eviction; all-protected capacity rejection and continued editing of existing choices pass locally. Cached committed-snapshot search is implemented. The published baseline passed CI query/capacity limits. Physical scrolling/performance remain unmeasured. |
| F26 | Partial | Artifact/runtime/permission/backup audit, reset/restart and corruption/downgrade checks. Committed-reset, rollback and receiver-publication faults clear uncertain projections without discarding durable choices. Actual owned screen-service enable/revoke/reconnect preserves choices, invalidates authority and restores emulator settings. Full live permission/reboot/intent/privacy qualification remains. |
| F27 | Implemented for inactive distribution | Capability/support metadata derive from validated APK evidence. No nonempty measured qualification has been supplied. |
| F28 | Partial | Separate signed-upgrade probe, publisher-only key use, cleanup and offline failure regressions. Actual signed predecessor upgrades pass on API 29 and API 36; complete grant/namespace and fault matrix remains. |
| F29 | Implemented | Signing, installation, migration, capability and provenance documents aligned. Owner license and support/privacy endpoints remain explicit owner decisions. |
| F30 | Blocked | Physical attempt/delivery/coverage matrix and 30-day pilot have not started. No product-rate or monthly-effort target is claimed. |

## Earlier published baseline and signed-upgrade record

Build `101601` (`0.1.0-dev.16.1`) from commit `52823d48dd5dd0c7cd7931197ad5785de88e6af9` passed [GitHub verification and delivery](https://github.com/appunni-m/business-gate/actions/runs/34445510013). Its [versioned APK](https://github.com/appunni-m/business-gate/releases/download/build-101601/business-gate.apk) and rolling download were independently downloaded and byte-compared. Checksums, source metadata, SDK baseline, rolling version/hash and pinned signing certificate matched. APK SHA-256: `5c8860d44366172f7e348446c790e693d7518f9f22bdb65e9bd8599d28346f38`.

The actual signed `101501` → `101601` upgrade passed 32 preparation and 18 verification assertions on the existing API 36 emulator. The exact number, label and ALLOW choice survived; an external intent could not replace the choice or activate the rule. Installed version `101601` was independently checked and the separate probe removed. CI passed the same 50 signed-upgrade assertions on API 29. The local upgrade used only the probe's Business Gate controls, without a general UI dump. This establishes signed installation and upgrade behavior, not connected-app qualification.

The published baseline passed 251 core checks, 79 performance/capacity and timing-probe checks, and six effort/restart preparation and verification stages. CI measured completed query p95 65 ms, worker CPU p95 3 ms, worker wall p95 60 ms, queue p95 1 ms, and a 50,000-record snapshot in 9,137 ms. Both original thresholds passed. Earlier failed runs remain in Actions; they exposed Activity replacement timing, normalization cost and allocation overhead. No query or snapshot threshold was increased.

## Storage recovery and exact-choice follow-up

The current follow-up adds **Check storage** and separate, explicit **Retry save** actions. A storage check uses SQLite integrity/reference checks, pauses actions, invalidates previous global revisions and reconciles uncertain jobs to reinspection. It does not delete choices, replay failed choices, mint or extend an unblock grant, clear the compatibility circuit, or resume a session.

Pending user choices are bounded and kept by receiving namespace and exact phone, including a failed new number that has no database row. The UI distinguishes saving, unsaved and durable choices. An explicit retry checks the original number, receiving namespace, sequence and account revision. A newer choice replaces an older pending choice; late success/failure callbacks cannot overwrite it. Reset clears pending intent, and returning to another receiver exposes only that receiver's pending choices. Open number-entry forms and row commands are rejected after reset, receiving-account change or revision change.

Unsaved choices are **process-local**, not durable. If the process dies before a database write succeeds, those unsaved changes must be entered again. Every startup remains paused. The UI must not describe an unsaved choice as preserved on disk. Storage recovery cannot reconstruct corrupted data; it preserves the files and reports failure when the database remains unusable.

Owned emulator regressions make the account table unavailable, attempt existing/new ALLOW writes, restore storage, and verify separate explicit retries. They cover unchanged grant timestamps during storage checks, stale/forged retry payloads, newer conflicting choices, reused row IDs after reset, receiver changes, queued recovery invalidated by reset, and persistence of the compatibility circuit. These tests do not inspect receiver content or establish a real external block/unblock result.

## UI restoration and layout follow-up

A native regression reproduced search surviving Activity recreation while the expanded exact-number details were lost. Another case showed that fixed controls at 200% text in landscape left less than a 48 dp list viewport. The fix restores presentation only after committed results arrive and only when its persistent local-data identity and receiving namespace match. Anchors include the row identity and exact phone, so row movement and resetting reused database IDs cannot restore another sender's details. Reset replaces the local-data identity. This token provides presentation isolation; it grants no action authority.

Landscape keeps the toolbar in one row; the centred column is bounded by the available width after insets. The bottom Apply action is present only for eligible pending work with the required connection and local prerequisites. Names and numbers retain their full values. The owned 24-case matrix covers light/dark, 100/150/200% text, portrait/landscape and standard/125% density; it checks horizontal overflow, list space and reaching an expanded action through the single list. Its images are drawn from the debug app's own views using fictional data, not captured from the receiver application. See [UI verification](ui-verification.md) for scope and remaining checks.

The first UI follow-up [CI run](https://github.com/appunni-m/business-gate/actions/runs/34449903447) failed before layout tests and delivery. Its legacy empty-search assertion assumed the top row remained visible. The same failure was reproduced at 1080×1920 and corrected to check the restored row and offset. Both local screen sizes now pass 253 core checks. The complete 24-case layout matrix also passes at 1080×1920 with 252 assertions and 72 validated owned renderings. This failed run did not replace the published baseline.

The next [CI run](https://github.com/appunni-m/business-gate/actions/runs/34451758306) passed all 253 core assertions, then exposed an off-screen empty-state assumption in the `ui` mode. This was reproduced at 1080×1920 and 525 dpi and corrected by scrolling to the owned row. Its 20 checks pass locally; delivery was skipped. The published baseline remains unchanged.

A separate fault reproduced a committed reset leaving six deleted records in cached snapshot/search results. The writer now invalidates prior presentation at reset and receiver/installation boundaries, and publishes snapshot, search and identity together. Failed boundary reads show unavailable data; explicit recovery loads only the durable current namespace. See [storage-boundary evidence](storage-boundary-verification.md).

[Run 19](https://github.com/appunni-m/business-gate/actions/runs/34453034270) passed all ten native harness modes, including 32 storage-boundary checks, then failed a short-landscape layout at 150% text and larger display scale. Its performance limits passed (query p95 91 ms; 50,000-record snapshot 2,457 ms). Delivery was skipped. The layout defect was reproduced locally and the landscape header now keeps search/Pause/menu in one fixed row, with title and status in the single scrollable list. All 24 configurations pass at 720×1280 with 360/450 dpi. The 12 landscape cases additionally check title/status return after recycling; the current case set has 264 assertions and 72 validated owned renderings. See [UI verification](ui-verification.md).

## Next completion work

F02 still needs a controlled sender for blocked/unblocked delivery trials and a physical receiver device to measure a safe one-to-one profile/confirmation route, fresh receiving identity, interruption behavior, unread effects and independently observed delivery. A spare phone with a separate test number, or a consenting participant's separate account, can supply the sender. The business welcome responses establish a working conversation before blocking; they do not establish delivery behavior while blocked. A guessed compatibility entry cannot complete this work.

Independent UI/layout/accessibility and lifecycle work can continue while that setup is arranged. Measured navigation/batching/scanning, notification provenance and hint integration, after-dispatch compensation, physical performance and the pilot remain unfinished. The complete plan must not be labeled complete on the strength of these engineering checks.
