# Business-name blocking: accepted design and implementation

The accepted behavior is eventual native blocking. A first message or preview may appear. Existing chats remain, and opening a chat to inspect its profile may mark messages as read. The earlier requirement to prevent every first-frame preview is superseded.

Business Gate uses the connected app's native Block and Unblock actions. A successful native block is recorded only after the same full number and business name are re-read and the opposite action label confirms the resulting state. Notification handling is separately exercised through [incoming sessions](incoming-notification-design.md) and an account-free Android cancellation fixture.

On 14 September 2026 the owner removed independent sender-side delivery testing from the acceptance requirements. Business Gate must verify its native action and exact-number postcondition; subsequent message blocking is delegated to the connected app. A second sender account is not a prerequisite for this work. The omitted delivery test is not recorded as passed.

## User flow

1. Grant screen access after reviewing the disclosure, select the installation, and start a visible receiving-account check.
2. Business Gate opens the measured Settings and own-profile controls. Review the full receiving-account number in Business Gate before connecting it. Choices from another receiving account remain separate.
3. Choose **Inspect business number** and enter the full international number. This starts one finite session, opens that contact through Android's public contact intent, and inspects the business profile. It does not compose or send a message.
4. A confirmed business without permission is blocked. Personal, ambiguous, group, changed, and unsupported profiles stop the route.
5. The main list shows the verified business name and exact number. The business-name switch opens an explicit confirmation. Enabling a name creates scoped Unblock jobs for known matching businesses; disabling it creates Block jobs. Exact-number overrides take precedence and remain available in the details.
6. Apply a pending choice in another visible session. **Stop** revokes the session immediately. Every session rechecks the receiving account; no background activity takeover occurs.

The current Apply action handles one pending account per session. With notification access and discovery enabled, incoming candidates are captured automatically. Choose **Review incoming conversations** to start a visible profile/receiver check and apply the current rule. A paused rule requires **Resume and inspect**. No activity launches on notification arrival. Existing native blocks remain effective when Business Gate's session ends.

## Current compatibility

The experimental contract is limited to the measured Android 36 emulator, English UI, portrait orientation, 100% font size, 420 dpi, and the exact selected installation version and signing history recorded in [compatibility.json](../app/src/main/assets/adapters/compatibility.json). A different installation, device, display configuration, or work profile cannot use this contract. An empty registry disables all connected actions.

The fully qualified physical-adapter registry remains empty. Experimental support is declared separately and never sets physical qualification to true. [Bundled evidence](../app/src/main/assets/adapters/evidence/business-profile-v1.json) binds the contract, shared route sources, and the actual native test result.

## Identity and policy

Permission names use NFC composition and trim edge whitespace. Case, accents, punctuation, internal spacing, and compatibility characters remain significant. Invalid or hidden formatting is rejected. Search normalization does not grant permission.

Database version 4 stores verified `business_name` separately from an optional local `name` label. Migration preserves exact-number and business-name choices, pauses execution, and does not promote labels into verified identity. Name-choice writes increment revisions, stop current authority, and create per-number jobs only for confirmed matching businesses using the default rule. Number-specific Allow and Deny choices are preserved.

A name grant belongs to one receiving namespace. Unblock requires a current explicit nonce with a finite lifetime. Pending writes, changed bindings, failed storage, stale revisions, or a Stop invalidate dispatch. A cached row is sufficient to display a preference; the action route must obtain fresh native identity before acting.

## Native route

The session checks the receiver through Settings, opens the exact contact using `ACTION_SENDTO` with an `smsto` URI and a runtime-selected package, and opens only the measured profile header. Message-list descendants are not inspected.

Two profile variants were measured: 24 rows with the action at row 22, and a reopened blocked profile with 23 rows and the action at row 21. Both require the same measured resource identities and classes. The route uses semantic scrolling and exact control actions, never coordinate taps or gestures.

Block opens a reason form. The route selects **Other**, requires **Report** to remain unchecked, requires optional feedback to remain empty, journals the final intent, and presses Block. Unblock is a direct profile action and is journaled before that click; it has no second confirmation screen.

The route distinguishes its own click receipts using the framework action, original window, control class, and a bounded event timestamp. An available source must also match its resource identity. Outstanding receipts can arrive out of order and are consumed only once. A deferred zero-action event additionally requires the same retained Android node within 250 ms. Unmatched input, changed windows, and stale receipts stop execution; a receipt never grants new action authority. Temporary incomplete frames are retried only within the measured transition deadline; no action occurs while focus or identity is missing.

The visible Stop control moves away from the next control before dispatch. Higher windows covering the action prevent dispatch. Journals and positive postconditions persist separately, so an interrupted action remains unverified rather than becoming an optimistic success.

## Verification and remaining work

The actual app passed a receiver-bound Block → name Enable → Unblock → name Disable → Block sequence, including its main-list switches and visible Stop control: [25 live assertions](../app/src/main/assets/adapters/evidence/business-profile-v1-live.txt). The final observed state was blocked; no messages were sent.

| Capability | Evidence status |
| --- | --- |
| Receiver binding and exact business identity | Verified on the measured emulator |
| Native Block, direct Unblock, and re-block | Verified through Business Gate |
| Main-list name controls and Stop | Verified through the actual native UI |
| Saved choices, migrations, recovery, and policy guards | Automated regression suites |
| Independent incoming attempt after blocking | Not required by owner decision on 14 September 2026; not performed or claimed as passed |
| Incoming candidate → exact profile/receiver → native Block → notification absent | 17 live assertions on the measured emulator; connected app removed the notification on opening |
| Business Gate cancellation and preservation rules | Actual Android listener/cancellation fixture with fictional identities |
| Automatic incoming candidate capture | Implemented; processing requires one visible session per conversation |
| Multi-account batching | Pending |
| Physical devices and other installations/configurations | Not qualified |

The owner-authorized greeting test on 14 September produced a message notification whose title matched the saved business identity 3.681 seconds after returning to Home. That initial observation was followed by a fresh reply and a complete 17-assertion incoming session. See the [incoming evidence and boundaries](incoming-notification-design.md).

A supported cloud blocking API requires a separately provisioned business-platform account and credentials. No supported API controlling the ordinary signed-in account was established. The implemented route uses Android framework APIs and adds no network permission or runtime SDK.
