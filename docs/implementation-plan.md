# Business Gate completion and fixes plan

Prepared 10 September 2026 against commit `092ce8f` and installed release `0.1.0-dev.5.1` (`100501`). This is an implementation plan, not a claim that the listed fixes or qualification tests have passed.

## Outcome and current baseline

The required outcome is a complete native Business Gate experience that applies exact-number choices through a measured, supported connected-app interface, verifies the result, and publishes installable signed APKs through GitHub.

The installed-release test established that Business Gate launches and its screen-access service connects, but **Resume opens compatibility help and no blocking session can start**. The installed registry is empty. The source also has an inert service filter, an activation call that always passes `false`, and no UI caller for the qualified-session method. Installing the connected app alone cannot resolve these code gaps. No real block/unblock was performed.

Already implemented and to preserve:

- Original blue gate identity, native management UI, local exact-number choices, pure policy checks, SQLite constraints, and conservative inactive startup.
- Java 17 and Android framework runtime, no application Internet permission, backup exclusions, and source/artifact branding gates.
- GitHub build, lint, instrumentation, signing, certificate verification, checksums, versioned prereleases, and a rolling downloadable APK. Signed installation and an update between two development builds were previously verified.

The existing CD pipeline is a working foundation. It does not establish live blocking, data migration correctness, physical compatibility, or the design's attention and discovery targets.

## Scope and priority

The [full design](design/Business_Gate_Full_PRD_Design.md) governs conflicts with the [companion design](design/Business_Gate_Complete_PRD_Design.md). Their acceptance IDs overlap with different meanings; traceability must identify the document as well as the ID.

- **P0:** required before enabling connected-app mutations in a distributed build. Controlled qualification trials follow the same identity, authority, Stop, and dispatch protections.
- **P1:** required for the complete designed product and reliable delivery; optional features may remain explicitly unavailable until qualified.
- **P2:** evidence, maintenance, and any separately requested commercial launch work. These are not prerequisites for the existing development APK download.

All work keeps Business Gate branding throughout source, resources, fixtures, documentation, artifacts, and commit messages. Use runtime installation selection and reviewed identity fingerprints; never embed or reconstruct a restricted product or package identifier. Preserve exact-number authority, local-only operation, and an empty inactive registry when qualification is absent. Use CLI tools and scoped Android instrumentation; no browser or computer-use tools are required.

## Dependency order

| Milestone | Work | Exit condition |
| --- | --- | --- |
| 1. Establish evidence and feasibility | F01–F03; start F29 | Reproducible baseline and a measured interface contract, or an explicit unsupported result. No invented adapter entries. |
| 2. Build identity and execution foundations | F04–F15, F26 | Namespace, authority, cancellation, dispatch, and recovery tests pass with synthetic owned fixtures. Live actions remain unavailable without qualification. |
| 3. Prove one complete live operation | F16 | One exact-number block and explicit unblock succeed on the measured environment, with independently checked results and interruption cases. |
| 4. Complete the product journeys | F17–F25 | Discovery, bounded sessions, optional hints/reminders, truthful statuses, design states, and performance are implemented and tested. |
| 5. Qualify and deliver the final artifact | F01, F16, F26–F29 | Full applicable acceptance matrix passes; the signed artifact installs, preserves choices on upgrade, and publishes with accurate support metadata. |
| 6. Validate product claims and maintain support | F30 | Measured coverage/effort results and a maintenance process; commercial publication only if separately pursued. |

Read-only feasibility measurements and independent storage/controller fixes can progress together. The first successful live operation is a milestone, not permission to skip the remaining safety matrix.

## Fix backlog

### F01 — Build a requirement and evidence ledger · P1

Map every full-design acceptance case `T001–T096`, its governing requirement, and applicable companion additions to an implementation location, test, artifact, environment, and result. Label results as implemented, synthetic-only, emulator-verified, physically qualified, failed, or unrun. Preserve the installed-release activation failure as a regression case. Distinguish confirmed source defects from hypotheses needing device evidence.

**Done when:** every requirement has a disposition and an evidence link; an unsupported or skipped case cannot be counted as a success. Large pure-test assertion counts are not substituted for end-to-end coverage.

### F02 — Establish whether a safe live route is feasible · P0

Measure the installed build using dedicated consenting receiver/sender accounts and synthetic test data. Establish the exact receiving account, sender-number field, one-to-one profile, business/regular signatures, block/unblock controls, confirmation behavior, postconditions, and navigation effects. The receiver and test sender have not yet been verified. Emulator measurements support engineering; physical measurements are still required for production qualification.

The current adapter requires receiver and sender identity on every screen. Determine whether real dialogs satisfy that contract. If they do not, implement and prove continuous identity binding, or mark the route unsupported. Determine whether entry preserves unread state and read receipts. Capture only the necessary permitted structure; no message-tree traversal or unrelated conversation dumps.

**Done when:** measured evidence supports a feasible complete route, including interruptions and side effects. If no compliant route exists, report that limitation and revisit scope rather than bypassing guards.

### F03 — Complete compatibility validation · P0

In [AdapterRegistry](../app/src/main/java/io/github/appunnim/businessgate/automation/AdapterRegistry.java), validate the entire bundled contract: unique IDs/environments, bounded fields, certificate and signing-history rules, exact build, actual connected-app UI language, device/layout/display conditions, and evidence inventory. `Locale.getDefault()` alone does not establish the connected app's language. A syntactically valid evidence digest or `physicallyQualified` flag does not establish qualification.

**Done when:** each supported environment resolves to exactly one reviewed adapter; malformed, duplicated, unknown, changed, or insufficiently evidenced environments remain inactive. Updates and server-side layout variations have explicit rejection tests.

### F04 — Wire connection, readiness, and activation · P0

Replace the boolean-only activation boundary in [MainActivity](../app/src/main/java/io/github/appunnim/businessgate/ui/MainActivity.java) and [GateRepository](../app/src/main/java/io/github/appunnim/businessgate/data/GateRepository.java) with a validated readiness result bound to the selected installation, receiver, consent, revisions, and service generation. Connect the selected qualified runtime package to the service's narrow event filter. Validate Android package visibility and installation selection without broad package scanning. Wire Resume, Apply pending, and supported current-profile entry to real session dispatch.

**Done when:** a qualified ready environment can start a finite visible session; every missing prerequisite produces its specific status. Adding JSON or replacing `false` with `true` alone cannot activate the engine.

### F05 — Implement durable receiver and installation isolation · P0

Remove hardcoded namespace `1` assumptions from repository reads, writes, snapshots, jobs, and observations. Bind the active namespace to a verified receiver and installation/profile identity. Carry that binding into policy evaluation, action plans, and final dispatch. Account switching, reinstall/restore, clones, and unsupported work profiles must invalidate authority without transferring choices. Define explicit handling for locally saved choices created before any receiver was bound.

**Done when:** the same sender number in two receiving namespaces has independent choices; stale work from namespace A cannot observe into, arm, or mutate namespace B. Previously unbound choices never silently acquire connected-account authority.

### F06 — Add migration from the actual shipped database · P0

[GateDbHelper](../app/src/main/java/io/github/appunnim/businessgate/data/GateDbHelper.java) currently refuses all upgrades. Schema version 1 has now shipped, so use its actual database and predecessor APK when adding the namespace/job changes. Implement transactional versioned migrations, preserve ALLOW and manual choices, invalidate obsolete grants/bindings, and handle interrupted migration, disk exhaustion, and unsupported downgrade without deleting data.

**Done when:** a populated version-1 installation upgrades with choices intact and conservative authority; failures disarm with a recoverable explanation. Fresh-install tests alone do not satisfy this item.

### F07 — Close asynchronous cancellation and arming races · P0

An old `arm()` success callback can currently set `disarmed=false` after Stop, reset, or consent withdrawal. Observations are checked before entering the writer queue and can become stale before their transaction runs. Give all asynchronous commands and callbacks an operation epoch plus expected namespace/revisions; revalidate inside transactions and before callbacks affect runtime state. Centralize immediate Stop, ALLOW, receiver-change, and consent-revocation vetoes.

**Done when:** delayed success/failure callbacks and queued writes cannot rearm, repopulate reset state, or revive old authority. Test each cancellation point with a deliberately blocked writer and deterministic callback order.

### F08 — Derive every safety guard independently · P0

In [GateAccessibilityService](../app/src/main/java/io/github/appunnim/businessgate/service/GateAccessibilityService.java), replace `safe ? ALL_GUARDS : 0` and optimistic completeness/uniqueness values with individually established facts. Cover connection, official identity, exact environment, foreground/interactive/unlocked state, receiver/profile binding, windows, overlay, immediate vetoes, circuit state, lease, and durable storage. Policy must compare evidence with the expected binding, not merely require nonempty receiver/adapter fields.

**Done when:** withholding any required fact prevents every dispatch, and the reason is observable without exposing personal data. Pure evaluator tests and Android fact-derivation tests both pass.

### F09 — Make structural parsing and side-effect checks complete · P0

Strengthen [QualifiedAdapter](../app/src/main/java/io/github/appunnim/businessgate/automation/QualifiedAdapter.java): validate qualified ancestors/context, unique eligible controls, full identity fields, screen role, and fresh state within explicit node/depth bounds. Distinguish visible forbidden controls and checked side-effect options from enabled actionable controls; a disabled report/delete element must not disappear from safety inspection merely because it is disabled. Keep unsupported combined actions inactive unless a separately measured safe route exists.

**Done when:** zero/duplicate controls, name-only ambiguous dialogs, checked side-effect options, wrong ancestors, partial profiles, and adversarial labels yield no action. Add owned structural fixtures and measured sanitized fixtures without shipping test adapters in release.

### F10 — Bind the final click to the authorized control · P0

The service's `click(control, checked)` currently ignores `control` and uses the latest reading's inferred action. Dispatch the specific entry or confirmation control selected by the controller. After durable intent and immediately before `performAction`, reacquire and validate the screen role, window generation, exact sender/receiver, adapter environment, policy revisions, lease, Stop/ALLOW state, and complete guard set.

**Done when:** a changed profile, dialog, control, policy, or environment between inspection and dispatch prevents the click. Entry and confirmation cannot be substituted for each other or redirected by a state change.

### F11 — Correct event ordering, deadlines, and stale callbacks · P0

The service currently stops on every click/scroll event, including potentially its own action events. The controller also aborts on an intermediate profile event while awaiting a dialog. Define measured transition expectations and bounded waiting; do not introduce a blanket interval that ignores user input. Fence journal failure callbacks, which currently stop without checking their captured generation. Cancel or generation-bind every deadline, including controller-originated stop paths, so an old timeout cannot stop a new session.

**Done when:** realistic repeated, delayed, reentrant, intermediate, and out-of-order events cannot repeat actions or affect a later session. User interaction still stops mutation promptly, including where the narrow package filter affects interruption delivery.

### F12 — Make verification completion durable and acknowledged · P0

The controller marks COMPLETE and hides the overlay before `repository.verified()` confirms its database transaction. Return an explicit committed/stale/failed result from verification. Match the full action identity, namespace, revisions, generation, action, and unblock grant when completing a job. Separate entry dispatch, accepted confirmation, uncertain outcome, and verified postcondition; the current `sent` flag is set even before a failed entry click.

**Done when:** only a committed same-identity postcondition produces completed UI state or ownership evidence. Failed/stale writes remain truthful and inactive; late results cannot finish a newer job.

### F13 — Repair job reconciliation, retries, and crash recovery · P0

`observe()` uses `INSERT OR IGNORE`, so an existing completed/canceled job can prevent a newly needed action from being queued. Complete reinspection and reconciliation for fresh observations, classification changes, external unblock, already-satisfied states, and bounded retries. Do not let an uncorrelated observation complete an in-flight command. Handle live already-blocked/already-unblocked profiles without a repeat click, consuming only matching explicit unblock authority. Persist uncertainty and define a safe circuit recovery path.

**Done when:** actual process death at each intent/dispatch/verification boundary resumes with inspection only. No blind replay, permanently stuck completed job, false success, or silent infinite retry remains.

### F14 — Complete ALLOW races and authorized compensation · P0

Implement the full distinction between durable ALLOW preference, an explicit one-shot unblock command, and proof of a block dispatched by the current operation. ALLOW before final dispatch vetoes the block immediately. ALLOW after dispatch requires truthful transient state and verified, explicitly authorized compensation when a safe session is possible. Preserve the seven-day grant expiry and cancellation rules; ALLOW alone must not undo a later manual external block.

**Done when:** both sides of the dispatch boundary, process interruption, grant replacement/expiry, and uncertain ownership are covered. Compensation never guesses identity, assumes a successful prior block, or starts a background takeover.

### F15 — Prove visible Stop and foreground ownership · P0

In [SessionOverlay](../app/src/main/java/io/github/appunnim/businessgate/support/SessionOverlay.java), prove the Stop control is visible, reachable, and clear of target controls on every supported layout. An attached overlay is insufficient evidence of safe placement. Measure system/IME windows, TalkBack, rotation, calls, app switches, locks, and overlay loss. Cleanup may only affect a still-owned verified transition; never navigate over the user's new screen.

**Done when:** Stop and every required interruption prevent subsequent mutation. Overlay failure or uncertain foreground ownership ends the lease; no coordinates, gestures, or saved-screen continuation are introduced.

### F16 — Add live integration and actual-release qualification · P0

After F02–F15, prove the first controlled block and explicit unblock on the actual route. Independently verify sender delivery behavior and receiver block state; a click return or local label is insufficient. Exercise identity confusion, ALLOW, personal/unknown/group contexts, manual external blocks, wrong builds, side effects, unread/read-receipt changes, and interruptions. Verify the compiled signed APK as well as the debug/controller implementation.

**Done when:** recorded physical evidence, fixtures, reviewed environment metadata, and applicable safety cases support each advertised configuration. Test tools use disposable test signing and remain outside release; publisher credentials remain confined to the release-signing process. The first happy-path pass does not qualify all configurations.

### F17 — Connect passive discovery and account classification · P1

The service currently has no complete receiving-account bootstrap or working passive observation route. Wire eligible foreground profile observations to the active namespace, with authoritative regular/business classification, optional names, observed state, first-seen/type-change handling, and natural rechecks. Queue policy work only from valid evidence. Preserve ordinary personal observations without unnecessary review prompts or forced chat opening.

**Done when:** unknown/personal contexts remain untouched, personal-to-business changes use fresh evidence, explicit exact-number choices persist, and unsupported/muted/locked senders are reported as coverage limitations.

### F18 — Implement bounded batches and separately consented scans · P1

Connect [SessionBudget](../app/src/main/java/io/github/appunnim/businessgate/policy/SessionBudget.java) to live execution. Apply pending choices through qualified navigation with exact-number deduplication, five-mutation/25-second batch limits, truthful remaining work, and no automatic background continuation. Implement a distinct visible scan journey with its disclosure and 100-profile/120-second/no-progress limits only where the route can be qualified. Automatic discovery must preserve read state; read-affecting scans require explicit scope-specific consent.

**Done when:** reordering rows, repeated profiles, unsupported pages, interrupted navigation, and exhausted budgets stop safely without duplicating actions. No full-inbox rescan, read-state restoration trick, or silent expansion of scan scope occurs.

### F19 — Qualify the notification-to-candidate bridge · P1

[GateNotificationListener](../app/src/main/java/io/github/appunnim/businessgate/service/GateNotificationListener.java) currently stores opaque keys in memory without a downstream discovery consumer. Establish actual provenance, supported one-to-one candidate binding, repost/removal deduplication, and queued reinspection. A notification or display name cannot establish business classification or mutation authority. Check the selected namespace/installation before any extras access; handle grouped, redacted, unrelated, work-profile, and unavailable notifications conservatively.

**Done when:** candidates reach the supported discovery path without opening chats in the background or inspecting unsupported content. The feature remains optional and its actual coverage is measured.

### F20 — Integrate optional hints and complete consent cleanup · P1

Connect the existing pure English hint classifier only to a qualified latest-incoming-text contract. Enforce bounded input before unnecessary copying; retain reason bits rather than message text. Apply new/known/kept/regular/forwarded/OTP exclusions and review promotion caps. Clear pending in-memory hints and unreviewed feature metadata promptly on feature disable, consent withdrawal, reset, receiver change, and service disconnect. Preserve explicit choices.

**Done when:** the full positive/negative corpus and integration tests pass; repeated notifications do not create repeated work, and sales hints never confer block authority or falsify account type.

### F21 — Fix reminder permission and consent races · P1

[QuietReminder](../app/src/main/java/io/github/appunnim/businessgate/support/QuietReminder.java) checks preferences and permission before an asynchronous reservation but not again immediately before notification dispatch. Recheck current consent, namespace, useful review state, OS/channel permission, and cancellation generation in the callback; handle revocation without a crash. Define conservative reservation behavior after failed delivery, clock changes, and retries.

**Done when:** at most one useful silent reminder per 30 days, no reminder after revocation/reset, no empty or repeated notices, and no dependency of core UI/Stop on notification permission. Never alter another application's notifications.

### F22 — Measure all imposed attention intervals · P1

The current [AttentionClock](../app/src/main/java/io/github/appunnim/businessgate/policy/AttentionClock.java) is only driven by the management Activity; live session occupancy is not wired. Instrument visible automation/scanning/waiting and compatibility-repair journeys, preserving interval union rather than double counting. Split intervals across day boundaries, handle process loss and monotonic/wall-clock changes, and report local blind spots honestly.

**Done when:** tests cover overlapping UI/session intervals and interrupted persistence, and the effort view shows measured components accurately. The monthly target remains unproven until F30.

### F23 — Complete truthful setup, status, and recovery journeys · P1

Drive the UI from one current readiness/execution state rather than registry presence plus persistent enabled/paused flags. A runtime emergency stop must not leave a “Rule on” claim. Distinguish unsupported, waiting, failed, reinspect, blocked, unblocked, and local preference; `FAILED` jobs currently fall through to ordinary pending copy. Complete exception review before activation, optional permission skip/revoke paths, Retry check, Apply pending, Unblock now, diagnostics, and actionable compatibility repair.

**Done when:** each design state has a real transition and test, including interrupted setup, canceled confirmation, failed storage, expired unblock command, and service loss. Messages never promise an unverified result.

### F24 — Finish visual, accessibility, and navigation parity · P1

Audit the complete native UI against the full design's copy, spacing, state, and interaction contracts. Preserve the original blue identity. Complete resource/plural usage, theme contrast, touch targets, TalkBack focus/announcements, search/scroll anchors, row movement/recycling, keyboard/Back behavior, rotation, and process restoration. Expand beyond the existing screenshots to all important dialogs and long-content states.

**Done when:** small/landscape viewports, 100/150/200% font scale, larger display scale, both themes, API 29 and newer insets/navigation, and TalkBack pass without hidden actions, clipped text, unwanted toggle replay, or noisy status announcements.

### F25 — Bound storage and verify large-list performance · P1

Repository publication currently reloads all accounts; search scans matching rows and UI work rebuilds lists. Measure 10,000 records and the 50,000-account boundary, then use bounded queries, suitable indexes, lookup structures, and targeted updates where traces justify them. Apply retention during long-running use, not only initialization. Existing explicit-number insertion bypasses the observation cap; define safe capacity handling that preserves existing choices and allows users to protect numbers without silently dropping intent or growing without bound.

**Done when:** p95 completed-query time after debounce meets the design's 100 ms target on the declared phone, scrolling is usable, writes do not block the main thread, and storage/caches/history stay bounded. Never prune durable choices to meet a benchmark; unresolved capacity conflicts require an explicit product decision.

### F26 — Expand privacy, lifecycle, and authority-boundary tests · P0

Test disk-full/corruption handling, reset/restore, permission revocation, force-stop/reboot, exported Activity intents, and service reconnect against the actual implementation. External intents may open management UI but cannot grant or execute mutations. Audit built APKs and logs after hint/action tests for message text, OTPs, names/numbers in diagnostics, raw trees, screenshots, recording hooks, test classes, unwanted permissions, and runtime SDKs. Keep synthetic fixtures in test-only boundaries and test cleanup narrowly scoped.

**Done when:** no unauthorized entry point, silent resurrection, destructive recovery, personal-content artifact leak, network runtime, or test adapter reaches the distributable. Branding checks also cover generated artifacts; visual identity needs visual review in addition to text scanning.

### F27 — Extend CD to validate qualified releases · P1

Preserve [.github/workflows/android.yml](../.github/workflows/android.yml) and its working signing/download path. [audit_artifact.py](../scripts/audit_artifact.py) currently requires an empty registry, while [release_metadata.py](../scripts/release_metadata.py) and [publish_release.py](../scripts/publish_release.py) always state that blocking is disabled. Introduce explicit unqualified and qualified distribution validation. A qualified artifact must carry matching reviewed evidence and supported-environment metadata; derive capability statements from validated artifact contents.

**Done when:** CI rejects a nonempty unqualified registry, stale/missing evidence, fake adapter, or misleading notes. Unqualified development builds remain downloadable with honest limitations. A qualified build cannot be produced by merely removing the empty-registry assertion.

### F28 — Strengthen signed-upgrade and publication verification · P1

The signed smoke test proves installation/rendering, not preservation of seeded choices or migration semantics. Add a reproducible isolated-emulator flow that populates the previous signed APK, upgrades with `adb install -r`, and verifies choices, grants, namespaces, inactive startup, and schema outcomes through a test-only external harness. Add fault-injection tests for signing failure, mismatched certificates, interrupted drafts/uploads, rolling APK/tag/notes consistency, reruns, and older queued versions. Verify published asset bytes against the build metadata.

**Done when:** failed checks cannot publish an apparently successful artifact; retries recover without changing an already published versioned APK, and the rolling download identifies the correct verified build. Reuse the configured signing secret only in its intended CD job.

### F29 — Correct documentation and delivery ownership · P1

[implementation.md](implementation.md) still says the release is unsigned, and [qualification.md](qualification.md) still lists supplying signing credentials as unfinished. Update these against [delivery.md](delivery.md), the actual pipeline, and F01. Document supported environments, unsupported behavior, exact-number/first-message limits, qualification provenance, data/migration behavior, installation/update instructions, and development versus qualified releases. Keep the source design distinguishable from verified implementation evidence.

**Done when:** README, help, qualification documents, release notes, and APK metadata agree. No request to recreate the already configured secret remains. Record unresolved owner choices such as a repository license and public support/privacy endpoints without inventing them.

### F30 — Validate claims and plan ongoing support · P2

Run the consented physical/device and 30-day pilot matrix before claiming the design's product targets: zero observed unauthorized/wrong-number blocks, at least 99% verified intended outcomes across 1,000 controlled eligible attempts, at least 90% supported-pilot business discovery, and honest attention/coverage reporting including high-volume and unsupported cases. Report first-message and eligible-session latency separately; include visible waiting and repair effort.

Define response to incompatible updates and safety incidents: stop promotion, document affected environments, and issue a corrected higher-version signed update after requalification. Offline architecture provides no remote kill switch. Any paid store launch is a separate owner milestone covering identity, license, support/privacy publication, current platform/terms review, declarations, pricing, and approval. GitHub APK delivery does not need to wait for that separate launch.

**Done when:** support claims are backed by recorded measurements and maintenance ownership. If targets fail, revise the supported scope or claims explicitly; do not relabel the unfinished capability as complete.

## Acceptance coverage map

These ranges refer exclusively to the full design. F01 must separately record companion additions and superseded conflicts.

| Full-design cases | Required coverage | Principal fixes |
| --- | --- | --- |
| T001–T014 | Exact identity, business/personal/group classification, independent numbers | F02–F05, F08–F10, F16 |
| T015–T024 | Cache/type changes, manual choices, external changes, protected contexts | F05, F13–F14, F17 |
| T025–T037 | Optional hints, supported text/provenance, abstention | F19–F20 |
| T038–T044 | Keep/manual Block/cancel, review bounds and revocation | F07, F14, F20–F21, F23 |
| T045–T054 | Lock/foreground/build/structure/side-effect and interruption guards | F03, F08–F11, F15–F16 |
| T055–T064 | Stop/ALLOW ordering, storage failure, crash, verification, idempotence | F07, F10–F16 |
| T065–T072 | Session budgets, receiver change, read-state-safe routes, grant expiry | F05, F11, F14, F17–F18 |
| T073–T080 | Namespace/schema/revision isolation, migration, recovery, reset | F05–F07, F12–F13, F26, F28 |
| T081–T084 | Notification boundaries, retained content, runtime permissions | F19–F21, F26–F27 |
| T085–T090 | Search/order/state retention, scale, accessibility, themes | F23–F25 |
| T091–T092 | Quiet behavior and interrupted setup | F21–F23 |
| T093–T096 | Release isolation, external authority, clocks, measured pilot | F14, F22, F26–F30 |

## Verification and release gates

For each implementation change, add the relevant behavioral regression at the layer where the defect occurs. Reuse existing tests; do not count implementation-mirroring assertions as independent proof.

Before any implementation push, run the project-required pure suite, schema and brand gates, wrapper check, Android lint, debug/release builds, release artifact audit, and available device harness. Run targeted controller/adapter/repository tests for the changed behavior and validate workflow changes with actionlint. Record unavailable physical qualification separately.

Use four distinct evidence layers:

1. **Pure/SQLite tests:** rule matrix, revisions, sessions, event sequences, cancellation, authority, and constraints.
2. **Android emulator tests:** real service/window/repository behavior against owned synthetic fixtures, API baseline/current behavior, permission lifecycle, process death, and actual signed installation/upgrades.
3. **Controlled connected-app physical tests:** measured profiles/dialogs, receiver/sender identity, real block/unblock outcomes, unread/read receipts, interrupts, device/layout variants, and side-effect absence.
4. **Pilot and distribution verification:** coverage/effort targets, supported-device performance, final APK provenance, published downloads, and support maintenance.

A wrong-number/allowed-number/unauthorized personal block, false success, receiver mix-up, report/delete/send side effect, consent bypass, or lost durable choice blocks promotion. Empty or unsupported compatibility remains inactive. Completion means the required user journeys work in the declared supported environments and their evidence matches the shipped artifact.

## Inputs still needed during execution

Code, storage, controller, UI, documentation, and synthetic-test fixes can begin now. Live qualification additionally needs a verified receiving account, an explicitly identified consenting test sender, a supported physical test device, and measured screen/environment evidence. Installing the connected app does not establish those identities or measurements. No new signing secret, login to read the public repository, or browser automation is needed for the engineering work.
