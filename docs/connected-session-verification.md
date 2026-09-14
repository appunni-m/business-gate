# Connected session verification

The accepted contract and user journey are in [business-name blocking](name-visibility-design.md). This report distinguishes executed native behavior from remaining coverage.

## Evidence ledger

| Claim | Source | Status and scope |
| --- | --- | --- |
| Business Gate applies a native block and records only a positive result | Bundled [live test](../app/src/main/assets/adapters/evidence/business-profile-v1-live.txt), `MeasuredSession`, `BusinessProfileRoute` | Proved on the declared emulator installation |
| Enabling a verified business name grants and performs Unblock | Actual main-list switch, dialog, scoped nonce, and native postcondition in the same live test | Proved; exact-number preference stays Default |
| Disabling that name reapplies Block | Actual main-list switch and new native Block result | Proved on the same account |
| Stop revokes a live session and prevents delayed replay | Actual Stop button and subsequent unchanged blocked state | Proved before navigation; not a full physical interruption matrix |
| The first preview can appear and existing chats remain | Accepted product scope and native action design | Declared limitation |
| Incoming delivery after a native block | Business Gate verifies its native action; the connected app handles subsequent message blocking | Independent sender-side delivery testing removed from acceptance by the owner on 14 September 2026; not claimed as tested |
| No first-preview exposure is guaranteed | No supported pre-display route | Explicitly not claimed |
| Public APK delivery follows successful main builds | `.github/workflows/android.yml` verifies, signs, installs/updates, publishes and verifies release assets | Existing pipeline; each new run must be checked separately |

The bundled [contract evidence](../app/src/main/assets/adapters/evidence/business-profile-v1.json) records the tested local APK hash, shared route hashes, environment, final state, and limitations. Test-account numbers and the literal selected package identifier are omitted from repository artifacts.

## Testing boundaries

The account-free emulator runs the synthetic native harness. The receiver emulator runs only the explicitly opted-in `live-business`, `live-incoming` and `live-filter` modes; these modes do not reset data, insert fixture accounts, inject a receiver binding, or send a message. It reads the receiver through the actual route and uses normal repository and native UI commands.

The live mode tests one user-authorized number. It is not invoked by CI, where no real account or sender exists. CI's synthetic device checks must not be described as connected-app qualification.

A newly opened blocked profile has a different measured row count from the immediate post-block screen. Both variants are covered by the route. The helper's early trials did not subscribe to click events; those trials remain historical and do not prove interruption behavior. The main-app result and the updated receipt regression suite cover the implemented event handling.

Physical qualification remains pending. Consented displayed-name dismissal and a finite foreground cleanup batch are now implemented; see the newer evidence below. Incoming candidate capture and explicitly started identity/native-action sessions are implemented; the evidence below separates notification removal by the connected app from Business Gate cancellation. Independent sender-side delivery testing is no longer a required milestone. Do not promote the experimental contract to general device support or mark omitted or unrun checks as passed.

## Incoming notification observation · 14 September 2026

The owner authorized one greeting to the existing business followed by leaving the chat. The exact recipient and approved draft were verified before one acknowledged Send. Home was requested at `01:24:21.501163 UTC`; a matching business message notification was posted at `01:24:25.182 UTC`, 3.681 seconds later. The app remained in the background and was not force-stopped. A fresh native profile check showed the business was already unblocked, so no unblock action was needed.

The notification title matched Business Gate's saved business identity for the exact authorized number, and its metadata identified a one-to-one message conversation. This is a controlled incoming example, not production sender/receiver-binding authority. Names and message bodies were omitted from the local report at `output/notification-hi/incoming-test-result.json`; notification dismissal was not performed. This initial observation did not itself authorize further messages. The owner subsequently authorized additional greetings to the same business for incoming testing; independent blocked-delivery testing remains outside scope.

## Complete incoming session · 14 September 2026

A fresh authorized greeting was acknowledged, then Home was requested without force-stopping the connected app. The resulting incoming candidate passed [17 live assertions](measurements/incoming-session-api36.txt): automatic capture/replay, visible session, fresh exact business and receiver checks, current policy, native Block postcondition, preserved exact-number choice and receiving namespace, and selected-notification absence. [The JSON record](measurements/incoming-session-api36.json) binds the actual APK and source hashes.

The outcome was `VERIFIED_NOTIFICATION_REMOVED_BY_CONNECTED_APP`: opening the conversation cleared its notification. Business Gate cancellation itself is covered by the separate [Android fixture](measurements/incoming-notification-fixture.json), including callback acknowledgment, unrelated notifications, policy exclusions, replacement and Stop. Neither result is presented as the other.

The full native sequence was rerun after fixing fresh-list inspection and bounded click receipt handling. The receipt regression exercises reordered events and rejects a duplicate after consumption. See [the incoming design](incoming-notification-design.md) for measured alternatives, accepted read effects and supported environments.

## Displayed sender filtering · 14 September 2026

The [live name-filter record](measurements/display-sender-live-api36.json) passed 22 assertions: automatic direct business notification cancellation acknowledged by Android, preservation of the authorized personal notification, no background navigation during dismissal, automatic cleanup after opening Business Gate, and a fresh exact native Block postcondition. The shared summary retained a business preview before cleanup, so complete preview suppression is explicitly not claimed.

The [owned fixture](measurements/display-sender-fixture-api36.json) covers two matching names plus a personal sender, groups, whitelist and exact-number exclusions, durable history, listener route loss, two-item batch advancement through a synthetic host completion, visible Stop, and reset while unbound. The native Block/Unblock/name-control/Stop session was independently rerun with 25 passing assertions after the foreground and bounded verification changes. Physical qualification and independent blocked-delivery trials are not represented as passed.
