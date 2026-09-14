# Existing-account integration measurements

Connected native actions are experimental on the measured emulator; physical devices remain unqualified. This separate developer tool collects installation, selected-node and structured notification measurements. Its explicit block/unblock trial modes require exact-number authorization. A capture alone does not qualify a route or promote a compatibility contract. See [connected-session verification](connected-session-verification.md) for demonstrated app behavior.

The owner asked to work with existing accounts first and will supply a later incoming test message. A separate sender is not required to begin profile and confirmation measurements. Every real mutation still needs an exact selected number, explicit authority, and a verified route.

## Preserve the test account

The original account emulator was configured under `/tmp/business-gate/avd/BusinessGateTest.avd`. On 12 September 2026 that directory was missing, and no surviving copy was found in the searched locations. Its prior sign-in could not be restored. The replacement now uses `~/.local/share/business-gate/android/avd/BusinessGateIntegration.avd`, outside the repository and temporary directories. The original app APK was signature-verified before reinstalling it. Subsequent normal navigation reached the receiver’s own profile. The measured phone field is present and has strict international-number syntax; its value is not exported. This establishes the observed own-profile surface, not a production receiver binding.

Set `JAVA_HOME` to Java 17 and `ANDROID_HOME` to the installed SDK, then use:

```sh
python3 scripts/emulator-session.py status
python3 scripts/emulator-session.py start
```

`create` is an explicit first-use operation that rejects existing descriptors or data. `start` reuses the persistent emulator and rejects a different emulator occupying its port. Neither command wipes data. Keep signed-in accounts out of disposable harness emulators.

## Separate developer APK

```sh
./gradlew :qualification-probe:assembleDebug :qualification-probe:lintDebug
adb -s emulator-5574 install -r qualification-probe/build/outputs/apk/debug/qualification-probe-debug.apk
```

This module has its own application ID and test key, with no dependency from the main application. The release artifact audit rejects measurement classes in the app's mapping. Both external entry points, an Activity for the owned self-test and a receiver for captures, require Android’s `DUMP` permission, available to the authorized development shell. Private report files are retrieved through `run-as` and then removed. The measurement APK adds no recorder, probe or runtime import to the distributed app. The app's separately reviewed experimental contract is described in connected-session verification.

The service declares ordinary window-content retrieval, resource IDs and interactive-window access, with `isAccessibilityTool=false` on API 31+, matching the relevant production capability restrictions. It cannot perform gestures. Capture uses a protected asynchronous broadcast and the normal service lifecycle. It does not launch a probe Activity over the observed screen. The owned self-test alone uses the Activity and synthetic fixture. The initial instrumentation attempt left a dead system binding on the isolated API 29 emulator; a reboot of that account-free emulator cleared it. The current self-test passes through the ordinary lifecycle.

The Android [service API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) and [service-info API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo) define those capabilities. Tests must use the actual production declarations before interpreting a field as accessible in release.

## Obtain measurements

The selected installation is derived at runtime from an APK path supplied by the operator. The wrapper verifies that APK, then compares its version and Android-specific signing certificate with the actual installed package. Certificate rotation ranges are respected; an ambiguous or unmatched identity fails. The literal external package identifier is not written to reports or project files.

```sh
python3 scripts/measure-installation.py environment \
  --serial emulator-5574 --target-apk /outside-project/selected.apk \
  --output output/measurements/installation-01.json
```

`root` mode is a narrower initial feasibility check: it obtains only the selected installation's active window root, reads no text and acquires no children. Its screen classification is explicitly `unclassified`; root metadata alone cannot establish login or profile identity. It can reveal whether the ordinary service declaration can see a root and whether that root supplies a resource ID.

For node mode, the operator must first open the account's own identity page or a specific one-to-one profile/confirmation screen and establish the non-message path to inspect. The `surface` argument is an operator attestation, not independently verified screen classification. Do not use path selection to explore conversations, message containers, arbitrary text, or unreviewed subtrees.

```sh
python3 scripts/measure-installation.py node \
  --serial emulator-5574 --target-apk /outside-project/selected.apk \
  --surface receiver --path 0,1 --enable-emulator-service \
  --output output/measurements/receiver-field-01.json
```

The example path is syntax only, not a selector or measurement for any external application. Actual paths must come from the selected screen. A root-only capture uses an empty path. A capture visits at most the root plus 12 explicitly selected children; it never searches siblings or recursively walks a tree. Root and path nodes must belong to the selected installation and be visible. The selected root must be the active focused window, with an interactive unlocked device and visible Stop. Capture tasks expire within eight seconds; the service also has a 15-second fallback Stop timer. Stop cancels capture and clears the package filter.

Root and `structure` modes do not read text or content descriptions. `node` mode exports structural metadata, ancestor child counts, text-presence metadata, and a fixed set of recognized control labels. It can verify number syntax on the specifically measured own-profile phone field without exporting its value. Ordinary node captures do not export account numbers, profile names, message text, full resource identifiers or external custom class names. The separately authorized `chat-node` exception below can retain bounded test-chat text in ignored local output. Custom classes are represented by a digest; restricted resource suffixes are redacted. A missing resource ID is recorded as missing rather than invented. This can expose incompatibilities in the existing parser that need measured implementation changes.

On an emulator, `--enable-emulator-service` temporarily adds only the measurement service and restores the exact accessibility settings afterward. It does not suppress other services. Physical-device consent must be granted in Android settings. Physical capture has not been tested by this work.

## Test and promotion boundaries

`scripts/test.sh` runs the selected-path/lifetime suite and report/signature regressions. Run the device self-test only on an account-free emulator because it opens a synthetic owned screen:

```sh
python3 scripts/measure-installation.py self-test \
  --serial emulator-5576 --enable-emulator-service \
  --output output/measurements/probe-self-test-01.json
```

Current observed records are under `output/measurements/`. The [sanitized receiver-profile observation](measurements/receiver-profile-api36.json) retains the measured environment, explicit ancestor path, field suffix, text-presence and number-syntax result. No phone value or phone digest is retained. The profile was opened through its exposed navigation button; Android separately reported `ProfileInfoActivity`. Each capture restores the exact accessibility settings. These are API 36 emulator observations, not physical qualification or official-source certification.

The existing production parser accepts the measured phone syntax, but its current screen recipes require receiver identity on the business profile and both confirmation screens. An own-profile field alone does not satisfy that contract. Those initial captures did not create a receiver binding, business classification, native result or registry entry. The subsequently implemented measured session is documented separately.

## Explicit navigation measurements

`navigation-label` is limited to an established options-menu surface with the observed root/list/row/title shape. It exports one title of at most 40 characters, restricted to letters, marks and a small separator set. Number-like values, package identifiers, multiline content and restricted branding are rejected. This identified the actual Settings entry after the fixed-label probe failed to recognize its suffix. It does not add a generic text reader or classify an arbitrary screen.

`focus-tab` and `focus-overflow` can request input focus only on the previously measured navigation shapes. `focus-profile` was rejected by the actual profile button; that failed observation is retained. `open-profile` can activate only the measured own-profile navigation button at path `0,4`, beneath its measured settings container. It validates the selected version, signer and API before acting, plus the foreground, unlocked device, node ownership, exact resource/class/ancestor structure, deadline and visible Stop. It cannot save profile edits or invoke an account block. Repeating it after the profile opened failed with `PATH_UNAVAILABLE` and performed no action.

These modes are explicit navigation operations, so they are not described as read-only. Reports distinguish `inputFocusRequested` and `navigationAction`; these navigation records have `mutationPerformed=false`. The separately authorized outgoing-message mode below sets that field to true. A successful navigation dispatch is not proof that the destination opened; the destination is measured separately. No coordinate, gesture or private-component launch bypass is used. The separately authorized test-chat exception below does not change the production restriction against message-tree traversal.

## Initial route prerequisites · historical

The receiver page has now been reached and measured without a sender. The next live target must be an exact existing business number authorized by the owner. Before any block/unblock attempt, measure that profile, classification, entry/confirmation controls, receiver continuity, side effects and postconditions. A random incoming number cannot establish business status. Ask for the later incoming message only once the qualified flow is ready. Physical and acceptance evidence remains necessary before distribution can enable an adapter.


## Owner-authorized outgoing test · 13 September 2026

The owner supplied an exact business number and authorized one message asking for a test reply. The installed app’s declared message-entry URL opened that number with the approved draft. Only the contact header, profile identity fields and composer/send controls were inspected; the message list’s descendants were not read.

The new `open-contact` command opens the measured conversation header. `node --expected-phone` compares an established profile field with an operator-supplied canonical number without exporting either value. The observed profile phone matched. Its `business_title` and `business_subtitle` fields occupied paths `0,1` and `0,2`; the profile list was observed with 17, 18 and 21 children. The developer check now validates the exact identity prefix while bounding the list to 3–64 children; unrelated detail rows do not change that prefix. The title contained two extra edge spaces compared with the chat header. Navigation continuity strips only edge whitespace; exact phone comparison and draft hashing remain unchanged. These are measured developer observations, not a qualified production adapter or proof of business classification across other layouts.

`send-draft` is an explicit outgoing-message mutation in this isolated tool. It requires the expected phone and approved draft digest at runtime, a matching profile check from the same process within 60 seconds, the matching chat-title digest, the measured composer/send structure, verified installation version/signer/API, foreground unlocked device, a finite session and visible Stop. It compares only the explicit header and composer fields, consumes the profile check before clicking, and never automatically repeats an uncertain attempt. Title continuity supplements the preceding exact-number check; it is not independently sufficient identity authority for production blocking. Process loss or Stop discards the temporary check. Reports contain comparisons and dispatch acknowledgment, not phone values, title values, draft text or delivery claims. Successful send records set `mutationPerformed=true`; failed send records retain an unconfirmed outcome rather than claiming no mutation.

The first send command rejected the different title before dispatch. A later profile check rejected the additional list row before any send attempt. After measuring both differences, exactly one send action was acknowledged. The composer subsequently no longer contained the approved draft. Its accessibility text had seven characters and did not mark itself as hint text, so this record does not claim a verified empty composer or delivery state. At that stage, notification queries had not established a matching sender. The later explicitly authorized exact-chat check below verified delivery indicators and matching welcome/support responses. No block/unblock was attempted and the production registry remains empty.


### Exact-chat inspection exception

On 13 September the owner explicitly allowed inspection of anything in the supplied exact-number test chat. This is a session-specific exception to the project rule against message-tree traversal. It does not authorize inspection of another chat, background collection or a production message reader.

`chat-node` requires a canonical `--expected-phone`, a profile check from the same process within 60 seconds, the matching measured chat header, the active focused selected installation, current version/signer/API, an unlocked interactive device and visible Stop. The path must begin with the measured message list at `4,1`; each call visits only that explicit bounded path, plus the header identity fields. It performs no accessibility actions. Only the selected node's text and description can be exported, each bounded to 320 UTF-16 units and checked for restricted branding. Local records stay under ignored `output/measurements/`; private content must not be committed. The receiver check does not establish independent production receiver continuity or blocking authority.

The owner authorized one repeat message using a distinct reply marker. Exactly one additional Send action was acknowledged at 12:17:41 UTC. The emulator had validated Internet connectivity, notification permission and a synchronized clock. Those checks do not prove message delivery. Delivery and reply evidence are recorded separately from dispatch; production compatibility remains disabled pending full route and physical qualification.


The final [sanitized outgoing-test record](measurements/outgoing-test-api36.json) records both outgoing tests with the exposed **Delivered** status. The repeat message and a following welcome/support response both displayed 5:47 PM; the earlier pair displayed 8:20 AM. The two responses contained the same text, rather than the requested unique reply marker. This is a receiving-account UI observation; the business's sender-side receipt was not independently inspected. No extra message was sent to follow a support link. That outgoing-test record did not test Block/Unblock or blocked delivery. Later native and incoming results are linked at the top of this document.

The final local verification passed all pure/schema/brand gates, 75 bounded helper checks, 19 report regressions, debug/release lint and builds, the wrapper/workflow checks and release artifact audit. The account-free API 36 native harness passed 14 modes and 532 assertions; the helper's separate lifecycle self-test passed four assertions. Main application code and the empty compatibility registry were unchanged. Physical qualification remains unrun.


The follow-up profile capture measured 21 rows, with the identity prefix and business contact controls visible. Attempting the next off-screen row was rejected with `PATH_UNAVAILABLE`; no fallback click or guessed selector was used. The block/unblock entry and confirmation route, continuous receiving-account binding and side effects still need qualification. The welcome responses resolve the earlier uncertainty about ordinary message reception, not those blocking prerequisites.
## Structured notification identity measurement

The separate developer probe can inspect public notification metadata without
opening a conversation or exporting message bodies, names or raw identities:

```sh
python3 scripts/measure-notifications.py \
  --serial "$GATE_TEST_SERIAL" --target-apk "$GATE_SELECTED_APK" \
  --expected-phone "$GATE_TEST_BUSINESS" --expected-receiver "$GATE_TEST_RECEIVER" \
  --enable-emulator-listener --output output/notification-metadata.json
```

Build the qualification probe first. Set the four variables to the authorized
test setup. The script installs the probe without clearing data, verifies its
APK and the selected installation, enables only its listener on an emulator,
and restores the original notification-listener setting. Physical devices must
grant access in Android settings. Reports contain bounded shape, digest and
expectation-match fields. This command never promotes compatibility evidence.
