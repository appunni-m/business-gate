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
| Incoming notifications stop after the native block | Native Block result is proved; no independent sender-side attempt was observed | Incoming delivery test remains unrun |
| No first-preview exposure is guaranteed | No supported pre-display route | Explicitly not claimed |
| Public APK delivery follows successful main builds | `.github/workflows/android.yml` verifies, signs, installs/updates, publishes and verifies release assets | Existing pipeline; each new run must be checked separately |

The bundled [contract evidence](../app/src/main/assets/adapters/evidence/business-profile-v1.json) records the tested local APK hash, shared route hashes, environment, final state, and limitations. Test-account numbers and the literal selected package identifier are omitted from repository artifacts.

## Testing boundaries

The account-free emulator runs the synthetic native harness. The receiver emulator runs only the explicitly opted-in `live-business` mode; that mode never resets data, inserts fixture accounts, injects a receiver binding, or sends a message. It reads the receiver through the actual route and uses normal repository and native UI commands.

The live mode tests one user-authorized number. It is not invoked by CI, where no real account or sender exists. CI's synthetic device checks must not be described as connected-app qualification.

A newly opened blocked profile has a different measured row count from the immediate post-block screen. Both variants are covered by the route. The helper's early trials did not subscribe to click events; those trials remain historical and do not prove interruption behavior. The main-app result and the updated receipt regression suite cover the implemented event handling.

Physical qualification, an independent incoming-message attempt, notification binding/dismissal, and automatic incoming processing remain pending. Do not promote the experimental contract to general device support or mark those checks as passed.
