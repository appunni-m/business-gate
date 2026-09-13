# Business-name visibility design

Prepared 13 September 2026 against `f754052`. Governing objective: block business notifications and entry to business messages unless the user explicitly enables the business by name. The full objective remains open. Successful message delivery, a green build, a stored preference, a dismissed notification or a late overlay is not completion evidence.

## Correction to the earlier design

The earlier PRDs explicitly excluded notification suppression and name-based permission inheritance. Those exclusions conflict with the current owner objective and no longer define the requested product. Existing exact-number choices must survive, personal/uncertain/group contexts cannot be automatically blocked, and restricted external branding remains prohibited. No compatibility record or physical result may be invented. An empty registry must continue to disable connected actions.

The working interpretation of “app entry” is entry to individual business conversations, including inbox previews and direct entry routes. A clarification is pending because gating the entire connected application would have different effects on personal conversations. Do not silently use whole-app suspension as a substitute for selective filtering.

## Final behavior and acceptance evidence

| ID | Required behavior | Evidence required for completion |
| --- | --- | --- |
| NV01 | A confirmed business that has never been enabled by name is hidden by default. | A real incoming message produces no business notification or visible business message content on every supported route. |
| NV02 | Explicitly enabling its name permits that business in the selected receiving account. | Enable in the shipped UI, restart the process, receive a real message and open the enabled conversation. |
| NV03 | Turning the name off removes permission for subsequent notifications and entry. | New events and direct-entry attempts use the committed current revision; stale callbacks cannot reveal content. |
| NV04 | Names supply the user-facing permission key; spelling matches are exact after canonical Unicode composition and edge-space removal. | Tests for case, punctuation, internal whitespace, accents, format/control characters, renamed businesses and same-name businesses. No fuzzy or accent-stripped allow inheritance. |
| NV05 | Existing exact-number ALLOW and manual DENY choices survive and take precedence. | Upgrade from the actual shipped database; conflicting name/number choices evaluated and preserved across reset/restart/account changes. |
| NV06 | Rules cannot cross receiving accounts, installations or Android profiles. | Matching and mismatching verified scope tests, including account switches and stale writes. |
| NV07 | Personal, uncertain and group contexts receive no automatic block. | Authoritative business classification is checked independently of its name. A name, message wording or bot flag cannot establish business status. Missing evidence is an explicit coverage gap. |
| NV08 | Neither the first notification nor a first frame, inbox preview, search result, direct link, existing conversation or recent-task preview exposes an unenabled business message. | Tests at the actual pre-display boundary of the final platform/integration, including cold starts, grouped notifications, lock screen, permission loss and service/process death. |
| NV09 | Local-only Java 17/framework implementation, Business Gate branding, retained choices and installable GitHub APK delivery. | Source/artifact audits, migration/lifecycle tests, signed installation/upgrade and public artifact verification. |

The existing test-chat exception authorizes inspection of that exact number only. It does not authorize a general production message recorder, unsolicited sends or unrelated chat inspection.

## Platform findings

The public [`NotificationListenerService`](https://developer.android.com/reference/android/service/notification/NotificationListenerService) receives posted notifications and can request their dismissal. It is not an approval callback that runs before posting. Removal latency cannot prove that a heads-up or lock-screen preview was never visible.

The installed API 36 public `android.jar` contains `NotificationListenerService` but does not contain `NotificationAssistantService`. The [Android 16 framework source](https://github.com/aosp-mirror/platform_frameworks_base/blob/android16-release/core/java/android/service/notification/NotificationAssistantService.java) marks the latter as hidden/system API. Do not compile hidden stubs, use reflection or treat a shell-enabled system service as an ordinary APK capability.

An [`AccessibilityService`](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) observes exposed UI and invokes exposed actions. An event-triggered overlay is not evidence of filtering before the other application's first frame. [`DevicePolicyManager.setPackagesSuspended`](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#setPackagesSuspended(android.content.ComponentName,%20java.lang.String[],%20boolean)) operates on packages and requires device/profile-owner or delegated authority; it does not selectively hide business conversations.

These findings leave NV08 unproved for an ordinary companion APK. The final architecture needs a supported pre-display integration in the connected application or an authorized platform extension with access to authoritative business/conversation identity. This is a platform dependency, not a reason to mark delayed removal as equivalent. No device ownership, application suspension, root access or platform change is authorized merely by this document.

## Shared policy and stored choices

Use a framework-independent visibility policy for both surfaces. Its output states what must be shown or hidden; it never reports that enforcement occurred. The eventual notification and entry integrations must independently attest support and verify the outcome.

A policy snapshot contains the receiving scope, current revision, explicitly enabled business names and the existing exact-number overrides. Observations must match that scope and revision and provide a verified one-to-one sender identity. Check explicit number choices first. For a confirmed business without a number override, permit only an explicitly enabled name; otherwise require hiding. Personal accounts remain visible. Unknown, ambiguous, non-direct, stale or incomplete observations report unresolved coverage and cannot authorize automatic action.

Names are bounded to 120 Unicode code points. Normalize canonical composition (NFC) and trim edge spacing. Preserve case, accents, punctuation, internal spacing and compatibility characters. Reject embedded control/format characters and malformed surrogate pairs instead of silently converting them into another enabled name. Search normalization remains separate from permission matching.

Do not migrate optional local contact labels into enabled business-name permissions: those labels were not affirmative name-based permission. A new durable name-choice table must have a receiving-namespace foreign key, exact canonical name key, explicit enabled state and revision. Migration preserves all number choices and creates no name grants. The writer publishes both kinds of choices atomically. Pending writes, failures and account changes invalidate stale enforcement work.

## UI details

The main list is organized by observed business name. Each name has one clearly labeled Enable switch. Details show the exact observed spelling, receiving-account scope, associated verified numbers and any number-specific override. Unknown senders go to an unresolved-coverage section rather than being presented as confirmed businesses.

“Enable a business name” uses a native text field, the exact name preview, Cancel and Enable. It explains that name permission applies to confirmed businesses with this exact spelling in the selected receiving account. Number exceptions remain reachable from the name's details. Show saving, saved, failed and retry states without changing permission optimistically.

The protection status separately reports notification filtering and conversation entry. It must say unavailable when no supported enforcement route exists. Only a verified active route may show protection as active. The blocked entry presentation uses the original blue gate identity with the business name, Enable this business, Back and Stop; it contains no message preview. A late presentation cannot pass NV08.

## Execution order

1. Implement and test the shared name-based policy against NV01–NV07, keeping policy decisions separate from enforcement results.
2. Add transactional durable name choices, actual-schema migration, reset/account isolation and native Enable controls. Preserve exact-number overrides.
3. Establish the supported pre-display integration/platform contract and measure authoritative business identity and every entry surface. Keep NV08 open until this dependency is satisfied.
4. Connect both notification and entry enforcement to the same committed policy snapshot. Revalidate scope/revision immediately before action; test enable/revoke and interruption races.
5. Run the complete visibility matrix on the final installed artifact, then all project gates and signed GitHub delivery. Mark this objective complete only when NV01–NV09 have direct supporting evidence.

A prototype that only dismisses posted notifications or covers an already opened conversation is partial engineering evidence. It must not replace the requested end state.


## Implementation checkpoint · 13 September 2026

The shared name policy, schema-3 migration and scoped durable name choices are implemented. Settings & privacy now opens native business-name management and enable/remove-permission forms. Optional number labels create no name grants. Exact-number choices take precedence in the shared policy. Pending writes, failures, reset and receiver changes invalidate policy snapshots; recovery does not replay a failed enable.

Validation passed 147 name-policy assertions, 43 schema/migration checks, all existing pure gates, debug/release lint and builds, and 14 API 36 native modes with 581 assertions. The native run includes actual v1/v2 helper upgrades, the native name form, failed saves, stale/reset races, receiver separation and a name permission surviving actual process death. The final artifact audit found 159 mapped app/compiler classes, no runtime SDK/native library or test harness, and no enabled compatibility contract. These results verify the new local policy/storage/UI work only.

Notification dismissal and conversation-entry enforcement are not wired. The business-name list is currently a settings surface; the complete main-list redesign remains open. NV08, real-message enable/deny trials, a supported integration contract, physical qualification and signed delivery of these changes remain incomplete. The complete objective is still active.
