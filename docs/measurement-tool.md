# Existing-account integration measurements

The signed development APK still has no qualified connected-app adapter. This separate developer tool obtains missing installation and selected-node measurements; it neither blocks accounts nor promotes a contract. A successful capture is an observation, not proof of a working blocking route, account binding, sender delivery, or physical qualification.

The owner asked to work with existing accounts first and will supply a later incoming test message. A separate sender is not required to begin profile and confirmation measurements. Every real mutation still needs an exact selected number, explicit authority, and a verified route.

## Preserve the test account

The original account emulator was configured under `/tmp/business-gate/avd/BusinessGateTest.avd`. On 12 September 2026 that directory was missing, and no surviving copy was found in the searched locations. Its prior sign-in could not be restored. The replacement now uses `~/.local/share/business-gate/android/avd/BusinessGateIntegration.avd`, outside the repository and temporary directories. The original app APK was signature-verified before reinstalling it. The replacement needs account sign-in; installation metadata alone cannot establish that sign-in has happened.

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

This module has its own application ID and test key, with no dependency from the main application. The release artifact audit rejects measurement classes in the app's mapping. Its only external Activity entry requires Android's `DUMP` permission, available to the authorized development shell. Private report files are retrieved through `run-as` and then removed. No recorder, probe, runtime import, or experimental adapter is added to the distributed app.

The service declares ordinary window-content retrieval, resource IDs and interactive-window access, with `isAccessibilityTool=false` on API 31+, matching the relevant production capability restrictions. It cannot perform gestures. Capture uses the normal Activity/service lifecycle rather than instrumenting and restarting its own accessibility-service process. The initial instrumentation attempt left a dead system binding on the isolated API 29 emulator; a reboot of that account-free emulator cleared it. The current self-test passes through the ordinary lifecycle.

The Android [service API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) and [service-info API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo) define those capabilities. Tests must use the actual production declarations before interpreting a field as accessible in release.

## Obtain measurements

The selected installation is derived at runtime from an APK path supplied by the operator. The wrapper verifies that APK, then compares its version and Android-specific signing certificate with the actual installed package. Certificate rotation ranges are respected; an ambiguous or unmatched identity fails. The literal external package identifier is not written to reports or project files.

```sh
python3 scripts/measure-installation.py environment \
  --serial emulator-5574 --target-apk /outside-project/selected.apk \
  --output output/measurements/installation-01.json
```

`root` mode is a narrower initial feasibility check: it obtains only the selected installation's active window root, reads no text and acquires no children. Its screen classification is explicitly `unclassified`; it cannot establish login or profile identity. It can reveal whether the ordinary service declaration can see a root and whether that root supplies a resource ID.

For node mode, the operator must first open the account's own identity page or a specific one-to-one profile/confirmation screen and establish the non-message path to inspect. The `surface` argument is an operator attestation, not independently verified screen classification. Do not use path selection to explore conversations, message containers, arbitrary text, or unreviewed subtrees.

```sh
python3 scripts/measure-installation.py node \
  --serial emulator-5574 --target-apk /outside-project/selected.apk \
  --surface receiver --path 0,1 --enable-emulator-service \
  --output output/measurements/receiver-field-01.json
```

The example path is syntax only, not a selector or measurement for any external application. Actual paths must come from the selected screen. A root-only capture uses an empty path. A capture visits at most the root plus 12 explicitly selected children; it never searches siblings or recursively walks a tree. Root and path nodes must belong to the selected installation and be visible. The selected root must be the active focused window, with an interactive unlocked device and visible Stop. Captures expire within 15 seconds. Stop cancels capture and clears the package filter.

Reports contain structural metadata, ancestor child counts and a small fixed set of recognized control labels. Unknown text, account numbers, names, message text, full resource identifiers and external custom class names are not exported. Custom classes are represented by a digest; restricted resource suffixes are redacted. A missing resource ID is recorded as missing rather than invented. This can expose incompatibilities in the existing parser that need measured implementation changes.

On an emulator, `--enable-emulator-service` temporarily adds only the measurement service and restores the exact accessibility settings afterward. It does not suppress other services. Physical-device consent must be granted in Android settings. Physical capture has not been tested by this work.

## Test and promotion boundaries

`scripts/test.sh` runs the selected-path/lifetime suite and report/signature regressions. Run the device self-test only on an account-free emulator because it opens a synthetic owned screen:

```sh
python3 scripts/measure-installation.py self-test \
  --serial emulator-5576 --enable-emulator-service \
  --output output/measurements/probe-self-test-01.json
```

Current observed records are under `output/measurements/`: API 29 and API 36 root self-tests and the selected installation's API 36 version/certificate history. The latest API 36 self-test additionally verifies that text is not inspected. The selected installation's root check returned `SELECTED_APP_NOT_FOREGROUND`; no account field was captured. Failed captures remain failed records and the wrapper exits nonzero. The environment record does not prove login, a business classification, a receiving identity, a profile selector, a block result or sender delivery. Every report explicitly sets `physicalQualification=false` and `mutationPerformed=false`. No registry entry has been added.

Next: measure the open receiver-identity page, then a user-selected existing business profile, entry/confirmation controls and postconditions. Implement any required binding or transition changes using those facts, test the exact-number flow, and only then ask the owner to send the incoming test message. Promotion still requires the project’s physical and acceptance evidence; the measurement tool cannot create it by changing flags.
