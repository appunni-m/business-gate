# Business Gate design and completion plan

Prepared 10 September 2026 against `408eb9d9226072f8fda2d341e1fddb3d72ddf8e4`, published build `102001` (`0.1.0-dev.20.1`). **Execution was authorized by the owner on 10 September 2026.** The baseline below is the planning snapshot; current results and remaining work are recorded in [the execution record](design-execution.md).

This is the next-work guide. The [original F01–F30 backlog](implementation-plan.md) retains the complete scope and historical defect descriptions; the [execution status](execution-status.md) records what has since changed. The [full design](design/Business_Gate_Full_PRD_Design.md) governs conflicts with the [companion design](design/Business_Gate_Complete_PRD_Design.md), subject to the project rules. Acceptance references keep their document prefix, such as `full:T089`; overlapping numbers are different cases.

The owner's 14 September 2026 acceptance decision supersedes independent sender-delivery requirements in the original designs: verify the native action and exact-number postcondition, then delegate message blocking to the connected app. The existing business can supply incoming notifications; a second sender account is not a prerequisite. See the [current design](name-visibility-design.md) and [verification record](connected-session-verification.md). Earlier baseline observations below remain historical.

**Incoming-session checkpoint · 14 September 2026:** the [foreground incoming route](incoming-notification-design.md) passed a fresh 17-assertion live test; native name controls and Stop passed 25 assertions. Automatic candidate capture is implemented. Multi-profile batches, physical qualification and the full pilot remain partial.

## 1. Outcome and verified starting point

The finished product must let a person manage exact-number choices in a single accessible Android page, explain whether each choice is saved and actually applied, run only qualified visible actions, and deliver signed APK updates through GitHub.

| Area | Established evidence | Still required |
| --- | --- | --- |
| Delivery | [Run 20](https://github.com/appunni-m/business-gate/actions/runs/34456095039) passed verification and delivery. Build `102001` downloaded without authentication; checksums, certificate, metadata, rolling bytes and rolling tag verified. | Automate those checks after publication; broaden failure recovery and retained evidence. See the [workflow review](workflow-review.md). |
| Signed update | CI passed 32 preparation and 18 verification checks on API 29 for the delivered update. | Repeat `101601` → `102001` locally on API 36. The most recent recorded local signed update is `101501` → `101601`; this planning pass installed nothing. |
| Local behavior | Existing core, storage, cancellation, restart and performance suites pass. Current local 24-case layout set has 264 assertions and 72 owned renderings. | TalkBack, keyboard/IME, all dialogs, actual saved-task restoration and the complete permission/lifecycle matrix. |
| Performance | Run 20: 10,000 records, completed query p95 60 ms; 50,000-record snapshot 1,114 ms. | Physical scrolling, memory/low-resource behavior and the complete capacity/recovery scenarios. Keep the 100 ms query and 10-second snapshot limits. |
| Connected actions | The packaged registry is empty; metadata correctly says actions are disabled. | Measured interface, receiver binding, consenting sender, physical block/unblock and interruption evidence. Installing both applications does not supply these prerequisites. |
| Completeness | All 96 full and 97 companion acceptance references are inventoried. | Complete scenario evidence and the 30-day pilot. An assertion count does not mean 193 scenarios have passed. |

### Constraints carried into every design

- Use Business Gate's original blue gate identity and platform controls. No external product names, package literals, logos or reconstructed identifiers in project content.
- Use Java 17 and Android framework APIs. The application has no network access, account system, runtime third-party SDK, subscription or analytics.
- A number is the identity. Display names, sales hints, list positions and restored focus never authorize a mutation.
- Keep local choices editable when connection, permission or compatibility is unavailable. Personal, uncertain, group and unsupported contexts do not acquire automatic blocking authority.
- Every connected mutation requires current receiver/number identity, explicit authority, current revisions, qualified structure, a finite session and visible Stop. An empty registry remains inactive.
- Use CLI and scoped owned-app instrumentation for engineering evidence. Preserve the receiver's data. Do not use browser/computer-use tools, coordinate input, gestures or message-tree traversal.

## 2. Proposed screen design

### 2.1 Information architecture

Keep one management page with a single `ListView`. Supporting flows use native dialogs or inline cards. There are no tabs, dashboard, chat bubbles or drawer.

The top-level menu will contain exactly four entries, matching the full design:

| Menu entry | Contents and next action |
| --- | --- |
| Enable a number | Country-code-inclusive number, optional local name, Cancel / Enable. |
| Settings & privacy | Optional discovery, sales hints and quiet reminder; permission shortcuts; consent withdrawal; local effort. |
| Compatibility & help | Current availability, selected installation, receiving-account review, unconnected-choice management and local diagnostics. |
| Clear local data | Explicit destructive confirmation. Existing connected-app blocks are unaffected. |

**Confirmed design gap:** the current overflow has seven entries. Move its three connection/receiver/unconnected-choice commands into Compatibility & help, with the same authority checks. Connection information is expanded only when useful; no unavailable action should appear to promise activation.

### 2.2 Layout wireframes

These diagrams specify hierarchy and interaction, not pixel screenshots. Fictional names and numbers below are design examples, not qualification data.

```text
PORTRAIT — local choices, connection unavailable
┌──────────────────────────────────────────┐
│ Business Gate                 Resume   ⋮ │  native toolbar
│ Blocking is unavailable in this build.   │  one truthful status
│ [ Search name or number              × ] │  pinned, clear only if nonempty
├──────────────────────────────────────────┤
│ Your choices are saved on this phone.    │  one relevant setup/issue card
│ [Check compatibility]                   │
│                                          │
│ ENABLED BY YOU · 1                       │  heading
│ Family Clinic                    [ ON ] │  row body expands; switch separate
│ +1 202 555 0101                          │
│ Enabled here · waiting for a check       │
│                                          │
│ NOT ENABLED · 1                         │
│ Parcel Offers                    [OFF ] │
│ +1 202 555 0102                          │
│ Block pending                           │
│                                          │
│ Review · 1                          +   │  collapsed unless explicitly opened
│ People · not auto-blocked            +   │
│ Only accounts found on this phone       │
│ are listed.                             │
└──────────────────────────────────────────┘
No Apply button while this build is unqualified.

LANDSCAPE / SHORT VIEWPORT
┌────────────────────────────────────────────────────┐
│ [Search…                              ×] Resume  ⋮ │  one fixed control row
├────────────────────────────────────────────────────┤
│ Business Gate                                      │  title and status scroll
│ Rule off. No new blocks will run.                   │  inside the SAME list
│ Relevant issue/setup card, then sections and rows   │
│ …expanded exact-number details and reachable action │
└────────────────────────────────────────────────────┘
Centred column ≤600 dp, measured after system/IME insets.

EXPANDED ROW — save failure
┌──────────────────────────────────────────┐
│ Family Clinic             [saved state] │
│ +1 202 555 0101                          │
│ Change not saved. Actions are paused.    │
│ Requested: Enabled                      │  requested and durable state distinct
│ Saved choice: Not enabled               │
│ Last checked: absolute date and time    │
│ [Retry save]                            │  explicit, number-bound command
└──────────────────────────────────────────┘
Storage card: [Check storage] checks health; it does not replay the choice.
```

Preserve the recent landscape fix. At 200% text, let row content and secondary actions wrap vertically; never reduce text to make a fixed-height row fit. If the IME leaves insufficient height, keep search and a scrollable content area reachable, and let Back dismiss the IME first. The exact fallback must be measured in WP2 before implementation is chosen.

The conditional Apply bar exists only for eligible pending work when prerequisites are satisfied. It sits above system navigation insets and is at least 48 dp high. During a connected session, a separate visible Stop control must remain reachable throughout the qualified route; a notification is not a substitute.

### 2.3 Visual specification

Preserve the current blue palette rather than introducing the source document's alternate enabled accent. This is an explicit branding resolution for the completion work.

| Token | Light | Dark |
| --- | --- | --- |
| Page | `#F7F8FA` | `#11161D` |
| Surface | `#FFFFFF` | `#19212B` |
| Main text | `#17212B` | `#F2F5F8` |
| Secondary text | `#536170` | `#B9C3CF` |
| Primary blue | `#2459D3` | `#A8C4FF` |
| Text on blue | `#FFFFFF` | `#11161D` |
| Blue-tinted surface | `#EDF2FF` | `#202F4B` |
| Divider | `#D9DFE7` | `#3B4655` |
| Recoverable/uncertain text | `#855100` | `#FFD28A` |
| Recoverable/uncertain surface | `#FFF1D6` | `#3B2C17` |
| Failure/destructive text | `#A32525` | `#FFB4AB` |
| Proposed focus outline | `#255DB1` | `#AAC7FF` |

Use Android system sans-serif: 22 sp toolbar, 16 sp medium row name, 14 sp body/action, 13 sp full number, 12 sp section/supporting text. Use 16 dp side margins, 8 dp related-item gaps and 24 dp section spacing. Search/cards have 12 dp corners; controls should use the full design's 8 dp corners after visual comparison. All interactive targets are at least 48 × 48 dp; account rows start at 88 dp at normal scale and grow with content. Use layout-start/end padding. Numbers remain left-to-right and full in expanded details and confirmations.

Current declared text-pair checks pass at a minimum 5.44:1. WP1/WP2 must also measure rendered control/focus contrast, disabled-state legibility and clipping. Use a 2 dp focus outline that does not change layout, accompanied by the native focus state. Blue, amber and red must always have a text/state cue. Animations are optional and disabled for reduced-motion/accessibility conditions; focus correctness is required even with no animation.

### 2.4 List and row behavior

| Section | Contents/order | Allowed row interaction |
| --- | --- | --- |
| Enabled by you | Exact ALLOW choices; sort by display search key, full number, stable ID. | Business switch; expand for details. Pre-enabled unverified numbers say verification is pending. |
| Not enabled | Confirmed businesses and explicitly manually denied rows; pending/error first, then relevant recency and full number. | Switch edits desired policy. OFF never implies verified block. |
| Review | Optional uncertain flags before neutral new senders; collapsed by default. | Keep / Block; Block opens exact-number confirmation. No automatic business switch. |
| People | Ordinary cached profiles and user-kept personal choices; collapsed. | Details and explicit review actions only; no routine automatic blocking. |

Only one row expands at a time. The row body never toggles the switch. Expanded details show full number, evidence label, absolute last-check time, desired policy, saved/pending state, last observed result and the appropriate action. No technical class names, fingerprints or raw internal error enums belong in routine copy.

Search stays local, across all four sections, with 150 ms debounce and stale-result rejection. Name normalization changes matching only; it cannot change number identity. Search temporarily exposes matching collapsed groups without overwriting their normal collapse preference. Clearing search restores the pre-search row/offset. Empty results say “No matching account on this phone” and offer Enable a number. Verify the 128-code-point query limit, long Unicode names and bidirectional presentation against the specification.

### 2.5 Status and action contract

The top status uses the highest applicable condition: storage uncertainty, unsupported connection/identity, required permission, setup incomplete, active session, paused rule, ready queue. Optional discovery denial is supporting copy and does not hide an active session's Stop. Recompute from current state after resume; do not replay a stale status from saved UI state.

| State | Proposed user-facing copy | Action and rule |
| --- | --- | --- |
| Loading | “Loading your choices…” | Do not briefly show an empty list or writable stale receiver data. |
| First run | “Choose the businesses you want to hear from.” | Set up / Choose numbers first. Cancel and Not now keep the rule off. |
| Empty registry | “Blocking is unavailable in this build. You can save choices here.” | Check compatibility. Resume opens the explanation; Apply is absent. |
| Rule paused, otherwise supported | “Rule off. No new blocks will run.” | Resume begins explicit activation review. |
| Screen access missing | “Blocking paused. Screen access is off.” | Open accessibility settings after the required disclosure. |
| Receiver unverified | “Verify the receiving account before applying choices.” | Review receiving account. Other receivers' choices remain separate. |
| Ready | “Rule on. Checks run in supported visible screens.” | No blanket protection claim. |
| Pending work | “Rule on · {n} actions waiting.” | Apply {n} pending only when the current route can act. |
| Running | “Applying your choices in the connected app.” | Visible Stop. No success until matching durable verification. |
| Saving a choice | “Saving your choice…” | Immediate ALLOW veto; no new connected actions while persistence is uncertain. |
| Save failed | “Could not save. No new blocks will run.” | Preserve the committed switch and show the unsaved requested choice with Retry save. |
| Storage unavailable | “Saved choices could not be loaded. Actions are paused.” | Check storage. Do not present an empty result as erased data. |
| Result uncertain | “Action sent; result not verified.” | Retry check obtains fresh evidence; it does not replay a click. |
| Cached verified block | “Blocked · last checked {date}.” | Details show absolute time and that this is a prior observation. |
| Saved ALLOW, observed block | “Enabled here; blocked in the connected app.” | Unblock now creates fresh explicit one-shot authority. |
| Optional hint | “Possible business · not blocked.” | Keep / Block; inference alone cannot create a job. |
| Ordinary person | “Not subject to automatic blocking.” | No automatic business toggle. |

Unsaved requested choices remain process-local. Copy must say they need to be entered again if the process closes before a successful save. Storage checks must not silently retry them, mint an unblock grant, clear a compatibility circuit or resume a session.

## 3. Focus, keyboard and dialog design

### 3.1 Focus identity and announcements

**Review candidate, not a reproduced defect:** `GateAdapter.getView()` removes and recreates child views during binding. Measure whether this loses input/accessibility focus or moves it to another sender before choosing a fix. Custom buttons also need an explicit rendered focus-state audit.

Logical row focus is `(local-data identity, receiving namespace, exact phone, control role)`. A database row ID alone is insufficient because reset can reuse it. Role means row details, business switch, Keep, Block, Retry save, Retry check or Unblock now. Presentation restoration never restores consent, a session, grant, callback or captured account command.

- After an unchanged row refresh or a successful section move, preserve the same number and role when it remains present.
- If the row is filtered out or deleted, move to its surviving section heading, or Search if no section remains. Announce the change once. Do not select a neighboring sender's action.
- When a role no longer applies, use that same row's details control. Reset/receiver changes discard row focus and use the current page heading or Search.
- A user focus move supersedes any delayed restoration. Activity stop, dialog changes and data-generation changes cancel restoration callbacks.
- Choose stable rebinding or guarded focus restoration only after a failing owned regression establishes what is needed. Always replace listeners with current revision-bound commands.

TalkBack order: section heading → one row summary containing name, full number and state → native switch or review actions → expanded detail/actions. Decorative initials are excluded. Avoid duplicate announcements from a parent summary and its children. Native checked state plus description must distinguish pending OFF from verified blocked OFF. A completed foreground user action gets one polite announcement; background observations do not produce repeated announcements. Input focus tests do not substitute for actual TalkBack traversal.

### 3.2 Back and IME priority

1. If an IME is visible, Back dismisses it and retains the query/form and current inline expansion.
2. With the IME closed, a supporting dialog cancels or closes; cancellation never runs its positive action.
3. On the management page, Back collapses expanded details first; another Back exits normally. It does not silently clear search before that sequence.
4. On a connected-app screen, Stop invalidates authority without navigating back over the user's screen.

Test API 29's fallback separately from API 33+ native Back dispatch. Do not infer IME visibility from a focused text field alone. Verify both gesture and three-button system navigation, hardware Tab/Shift-Tab/Enter/Space, keyboard-open rotation, and short landscape with 200% text. These are device cases still to run.

### 3.3 Dialog state matrix

Use native scrollable dialog content with reachable positive/negative buttons at 200% text. Returning from Android settings rechecks actual permissions; accepting a disclosure is not evidence that a permission is granted.

| Dialog/flow | Design details | Cancel, recreation and stale-state behavior |
| --- | --- | --- |
| Enable a number | Full number with country code; optional name; inline validation; Enable / Cancel. | Preserve only unsent form presentation in the same data/receiver identity after rotation. Never auto-submit. Reject stale receiver/reset/revision on submit. |
| Screen-access disclosure | Keep the full approved resource disclosure; Agree and open settings / Not now. | Store explicit consent/version only after the positive action succeeds. Settings return and process death do not activate the rule. |
| Notification discovery and sales hints | Separate opt-ins; Skip/Keep off supported; describe limited coverage and no automatic blocking from hints. | Permission alone grants no text-analysis consent. Turning either off immediately invalidates its work. |
| Activation / Apply / Retry check | Show effects, number/session scope, finite duration, visible Stop and any unavailable prerequisite. | Never restore a positive action or an active lease after recreation/process death. Reopen an explanation and require a new positive action with fresh facts. |
| Receiving-account review | Show the verified receiving identity; explain separate choices and no automatic transfer. | If the candidate changes or expires, dismiss/reject with a fresh-review explanation. |
| Manual Block | Full exact number and uncertainty explanation; Cancel / Block this number. | Cancel has no policy/job effect. Positive confirmation checks the current receiver, number and revisions. |
| Unblock now | Explain one-number effect and visible check. | A canceled/recreated confirmation cannot mint or reuse a grant. Existing ALLOW alone does not authorize perpetual unblocking. |
| Storage recovery | Check storage and Retry save are separate operations. | Show prior committed state plus unsaved request; no destructive fallback, automatic retry or hidden resume. |
| Clear local data | “Delete local choices and history?” Explain what is removed and that existing blocks remain. Red Delete local data / Cancel. | No delete on dismissal or rotation. After success clear search/details/focus and invalidate pending dialogs/callbacks. |
| Settings / help / diagnostics | Scrollable native content; diagnostics preview excludes names, numbers and message text. | Harmless presentation can reopen; dangerous actions require fresh review. Copy only after an explicit tap. |

Form restoration is not a new durable draft feature. Keep unsent form text only in owned instance state, fenced by the current data/receiver identity. Do not put receiver data or commands into external intents, notifications, logs or test reports.

## 4. Execution packages and exit gates

Priorities here describe the next development order. Original P0 safety requirements still gate every live mutation. Each package starts with a reproducible case, updates the relevant design/evidence record, then changes only the implementation needed to satisfy it.

| Package / priority | Concrete work and likely files | Dependencies | Evidence required to close it |
| --- | --- | --- | --- |
| WP0 · P0 baseline | Review this plan and workflow findings. Preserve build `102001` provenance; add per-case result records without marking unrun cases passed. `docs/acceptance-ledger.json`, status docs. | Owner execution instruction received on 10 September 2026. | Agreed design choices, complete F01–F30 mapping and an explicit unavailable live-qualification setup. |
| WP1 · P1 accessible rows | Reproduce focus loss/misrouting; implement current-identity rebinding or guarded restoration; focus outlines, headings, grouped labels and one-time announcements. `ui/MainActivity.java`, `ui/Ui.java`, owned instrumentation. | WP0 | Same-number/role focus across refresh, section move, recycling and filtering; stale restoration canceled after user move/reset/receiver change. Actual TalkBack traversal recorded separately. `full:T085–T090`. |
| WP2 · P1 responsive interaction | Complete IME/Back matrix, menu reduction, dialog layouts, full numbers, large text and control contrast; retain the compact landscape structure. `MainActivity`, `Ui`, resources, layout harness. | WP1 identity contract | API 29 and 36, both navigation modes, IME open/closed, all dialogs at narrow portrait and short landscape with 200% text; no clipped/unreachable actions. Existing 24-case matrix still passes. `full:T088–T090`, `full:T092`. |
| WP3 · P0 lifecycle and recovery | Setup/task restoration, stale confirmations, actual process death, permission revocation, service reconnect, lock/foreground transitions, storage failures and explicit retries. `MainActivity`, repository, services, debug harness. | WP1/WP2 | Durable choices preserved; canceled/expired commands never resume; inactive startup; no cross-receiver presentation or grants; storage unavailable is distinct from empty. `full:T054–T059`, `full:T073–T084`, `full:T092–T095`. |
| WP4 · P1 notifications and limits | Exercise own reminder permission/channel/cap/time behavior, consent withdrawal, no repeated prompts, capacity pressure and metadata retention. Complete privacy evidence. Repository, notification services, harness. | WP3 | Silent, useful, bounded reminders with no number/text payload; core UI/Stop independent of notification permission; 10k/50k limits unchanged; protected choices survive cache pressure. `full:T038–T044`, `full:T077`, `full:T081–T084`, `full:T087`, `full:T091`, `full:T095`. |
| WP5 · P1 delivery | Implement WF1–WF5 in the workflow review: exact post-publication verification, evidence manifest, stable action references, explicit queue contract and failure/retry coverage. Retain two-job signing boundary and version sequence. | Can proceed after WP0 independently of UI once execution is requested. | Offline failure tests plus a real main-branch run; anonymous APK/metadata verification; same-key upgrade from last release on API 29 and local API 36. `full:T083`, `full:T093`; F27–F29. |
| WP6 · P0 measured feasibility | Use the authorized receiver/business contact and a physical receiver for device qualification; measure allowed profile/control regions, receiving identity, signing/build environment, unread effects and interruptions. Qualification docs/registry validator. | Test inputs from owner; no invented evidence. | A measured compliant route or explicit unsupported outcome. Physical support requires sufficient evidence. F02/F03/F09/F16. |
| WP7 · P0 safe live operation | Finish real fact derivation, transition attribution, after-dispatch ALLOW compensation and verified block/unblock. Controller, repository, qualified adapter, accessibility service. | WP3 and WP6 | Exact native profile identity and positive postcondition; all stale identity/revision/Stop/timeout/call/overlay cases; no wrong number or unauthorized personal action; signed-artifact evidence. `full:T001–T028`, `full:T045–T064`. |
| WP8 · P1 full discovery journeys | Implement measured navigation and batches of at most five changes; separate discovery-only/read-affecting scan consent; notification provenance/candidate bridge; qualified hint consumption and review promotion. | WP6/WP7 route and authority evidence | Deduplication, exact-number tracking when rows move, bounded attempts/time/queue, no message-tree traversal, no implied coverage of hidden/unvisited profiles. F17–F22; full and companion cases from the ledger. |
| WP9 · P0 qualification / P2 product evidence | Run the applicable physical acceptance matrix against the final signed APK, device performance, upgrade/fault matrix, owner publication decisions and 30-day pilot. | WP1–WP8 | Each full/companion case has a linked result or explicit limitation. Support metadata matches actual qualified environments; pilot reports measured coverage, waiting and effort. F01/F16/F25–F30. |

Before connected actions exist, WP1–WP4 use synthetic Business Gate-owned fixtures to exercise states. Synthetic READY or RUNNING presentations never become release adapters or physical evidence. WP6 feasibility can be arranged while independent engineering proceeds; a failed feasibility result changes the supported scope, not the safety checks.

### 4.1 Regression matrix to implement

| Layer | Required dimensions / cases | Evidence artifact |
| --- | --- | --- |
| Pure and SQLite | Fresh/stale number, receiver and revisions; ALLOW before/after dispatch; rollback/commit/callback order; capacity; actual shipped schema. | Named scenario results tied to F IDs and document-qualified acceptance IDs. |
| Owned UI | Existing 24 theme/font/orientation/density cases; compact 720×1280 and normal phone baseline; all dialog families; focus/recycling; filtered switches; Unicode and full numbers. | Environment manifest, mode/assertion result, owned `View.draw` images and hashes. |
| Interaction | API 29/36, two navigation modes; IME visible/hidden; hardware keys; TalkBack; one action announcement; user moves focus before delayed result. | Per-scenario observed result. TalkBack pass requires TalkBack evidence, not only node properties. |
| Lifecycle | Rotation, Activity recreation, actual saved-task process death, force-stop, reboot, service reconnect, permission/channel revocation, receiver change, reset, failed save/recovery. | Durable state before/after and rejected stale commands; no unrelated app data. |
| Release | Signed predecessor → new APK, same certificate, version monotonicity, choice/label retention, paused startup, forged intents, probe cleanup, anonymous public bytes. | Signed-upgrade logs and version-specific artifact verification; do not uninstall to bypass an update failure. |
| Physical integration | Exact environment, scoped account consent, profile and confirmation structure, side effects, Stop/interruptions, fresh exact-number native postcondition. | Sanitized measured qualification record tied to APK hash. No raw messages, trees or invented success. |

If a navigation or TalkBack configuration is unavailable, record that exact matrix cell as unrun and continue independent cases. CLI-driven owned instrumentation remains the engineering approach; no broad UI dump or computer-use fallback is introduced.

### 4.2 Commands and checkpoints

These commands are the required execution gates. The initial planning pass ran no device commands; the execution record identifies actual later results. Set `JAVA_HOME` to JDK 17, `ANDROID_HOME` to the SDK, and `GATE_TEST_SERIAL` to the dedicated emulator.

```sh
scripts/test.sh
python3 scripts/check_wrapper.py
./gradlew :app:lintDebug :app:lintRelease :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:bundleRelease :release-probe:lintDebug :release-probe:assembleDebug
python3 scripts/audit_artifact.py app/build/outputs/apk/release/app-release-unsigned.apk
python3 scripts/check_brand.py app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/release/app-release-unsigned.apk app/build/outputs/bundle/release/app-release.aab release-probe/build/outputs/apk/debug/release-probe-debug.apk
actionlint .github/workflows/android.yml
scripts/device-tests.sh
scripts/ui-layout-tests.sh
```

Run focused regressions during development. Before each push, run the project-mandated pure/schema/brand, lint, debug/release and available-device checks; attach the new scenarios to the harness rather than leaving them as local one-offs. Document unavailable physical cases separately. Keep CI failure gates intact and never raise thresholds to hide a regression.

After a successful delivery, obtain the versioned APK, checksums and metadata anonymously, verify the APK certificate and rolling link, then perform the explicitly scheduled local signed update. A green workflow alone cannot close UI accessibility or physical qualification cases.

## 5. Complete backlog mapping

No original fix is dropped by the new sequence:

| Original fixes | Next packages | Remaining completion scope |
| --- | --- | --- |
| F01 | WP0, WP9 | Evidence completeness across both source documents. |
| F02–F03 | WP6 | Measured feasibility and compatibility contract. |
| F04–F05 | WP3, WP6, WP7 | Complete setup/receiver transitions and qualified activation. |
| F06–F07 | WP3, WP7 | Actual migration/fault/lifecycle and live cancellation boundaries. |
| F08–F14 | WP6, WP7 | Real guard derivation, parsing, final dispatch, transition handling, durability, reconciliation and compensation. |
| F15–F16 | WP2, WP3, WP7, WP9 | Accessible Stop, complete interruptions and final physical live proof. |
| F17–F20 | WP6, WP8 | Passive observation, bounded navigation/scans, provenance bridge and hint integration. |
| F21–F22 | WP4, WP8, WP9 | Quiet notices, effort accounting and pilot evidence. |
| F23–F24 | WP1–WP3 | Complete setup, recovery, state copy, accessibility and responsive design. |
| F25–F26 | WP3, WP4, WP9 | Resource/privacy boundaries and physical performance. |
| F27–F28 | WP5, WP9 | Accurate capability delivery, signed update and failure matrix. |
| F29 | WP5, WP9 | Current instructions; owner license and support/privacy destinations remain owner decisions. |
| F30 | WP9 | Physical attempt/delivery/coverage matrix and 30-day pilot. |

## 6. Decision gates and completion criteria

**Design checkpoint:** the proposed decisions are one page/four menu entries, the existing blue palette, the compact landscape toolbar, exact-number focus restoration, explicit unsaved-state copy, conservative dialog restoration and the Back order above. Implementation starts when the user asks to execute this plan. This checkpoint follows the user's current instruction; it is not a request to recreate signing credentials or add an approval service.

**Local-completion checkpoint:** WP1–WP4 and the local signed upgrade pass with scenario evidence, including actual TalkBack and IME cases. Record physical-only cells as unrun. This closes local engineering work, not the full product.

**Delivery checkpoint:** WP5 proves the actual public APK, exact source provenance and same-key update; a failed verify/install step cannot publish. Preserve the last successful versioned download when a newer build fails.

**Connected-action checkpoint:** WP6/WP7 establish a compliant measured route and physical evidence before enabling a distributed adapter. The receiver emulator currently has no consenting sender counterpart; a spare phone with a separate test number or an explicitly consenting participant can provide one. A physical receiver is still needed for final qualification. No messages are sent by this plan.

**Full-completion checkpoint:** WP8/WP9 complete discovery journeys, physical evidence, remaining applicable acceptance cases and the pilot. Until then, keep the product status partial and state exactly which environments and capabilities are supported.
