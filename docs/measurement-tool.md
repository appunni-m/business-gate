# Existing-account integration measurements

The signed development APK still has no qualified connected-app adapter. This separate developer tool obtains missing installation and selected-node measurements; it neither blocks accounts nor promotes a contract. A successful capture is an observation, not proof of a working blocking route, account binding, sender delivery, or physical qualification.

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

This module has its own application ID and test key, with no dependency from the main application. The release artifact audit rejects measurement classes in the app's mapping. Both external entry points, an Activity for the owned self-test and a receiver for captures, require Android’s `DUMP` permission, available to the authorized development shell. Private report files are retrieved through `run-as` and then removed. No recorder, probe, runtime import, or experimental adapter is added to the distributed app.

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

Root and `structure` modes do not read text or content descriptions. `node` mode exports structural metadata, ancestor child counts, text-presence metadata, and a fixed set of recognized control labels. It can verify number syntax on the specifically measured own-profile phone field without exporting its value. Account numbers, profile names, message text, full resource identifiers and external custom class names are not exported. Custom classes are represented by a digest; restricted resource suffixes are redacted. A missing resource ID is recorded as missing rather than invented. This can expose incompatibilities in the existing parser that need measured implementation changes.

On an emulator, `--enable-emulator-service` temporarily adds only the measurement service and restores the exact accessibility settings afterward. It does not suppress other services. Physical-device consent must be granted in Android settings. Physical capture has not been tested by this work.

## Test and promotion boundaries

`scripts/test.sh` runs the selected-path/lifetime suite and report/signature regressions. Run the device self-test only on an account-free emulator because it opens a synthetic owned screen:

```sh
python3 scripts/measure-installation.py self-test \
  --serial emulator-5576 --enable-emulator-service \
  --output output/measurements/probe-self-test-01.json
```

Current observed records are under `output/measurements/`. The [sanitized receiver-profile observation](measurements/receiver-profile-api36.json) retains the measured environment, explicit ancestor path, field suffix, text-presence and number-syntax result. No phone value or phone digest is retained. The profile was opened through its exposed navigation button; Android separately reported `ProfileInfoActivity`. Each capture restores the exact accessibility settings. These are API 36 emulator observations, not physical qualification or official-source certification.

The existing production parser accepts the measured phone syntax, but its current screen recipes require receiver identity on the business profile and both confirmation screens. An own-profile field alone does not satisfy that contract. No receiver binding, business classification, block/unblock, delivery result or registry entry has been created.

## Explicit navigation measurements

`navigation-label` is limited to an established options-menu surface with the observed root/list/row/title shape. It exports one title of at most 40 characters, restricted to letters, marks and a small separator set. Number-like values, package identifiers, multiline content and restricted branding are rejected. This identified the actual Settings entry after the fixed-label probe failed to recognize its suffix. It does not add a generic text reader or classify an arbitrary screen.

`focus-tab` and `focus-overflow` can request input focus only on the previously measured navigation shapes. `focus-profile` was rejected by the actual profile button; that failed observation is retained. `open-profile` can activate only the measured own-profile navigation button at path `0,4`, beneath its measured settings container. It validates the selected version, signer and API before acting, plus the foreground, unlocked device, node ownership, exact resource/class/ancestor structure, deadline and visible Stop. It cannot save profile edits or invoke an account block. Repeating it after the profile opened failed with `PATH_UNAVAILABLE` and performed no action.

These modes are explicit navigation operations, so they are not described as read-only. Reports distinguish `inputFocusRequested` and `navigationAction`; the legacy `mutationPerformed=false` field means no **account** mutation. A successful navigation dispatch is not proof that the destination opened; the destination is measured separately. No coordinate, gesture, message-tree traversal or private-component launch bypass is used.

## Remaining route work

The receiver page has now been reached and measured without a sender. The next live target must be an exact existing business number authorized by the owner. Before any block/unblock attempt, measure that profile, classification, entry/confirmation controls, receiver continuity, side effects and postconditions. A random incoming number cannot establish business status. Ask for the later incoming message only once the qualified flow is ready. Physical and acceptance evidence remains necessary before distribution can enable an adapter.
