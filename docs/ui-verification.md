# Native UI verification

This work implements F23/F24/F26 engineering subsets of the completion plan. It does not qualify a connected-app route or establish TalkBack, keyboard, physical-device or complete design parity.

## Reproduced defects and changes

- Activity recreation retained the search but discarded an expanded exact-number row. Restoration now waits for the committed results and checks the persistent local-data identity, receiving namespace and exact phone.
- Scroll restoration ran before the asynchronous result list arrived. Saved and pre-search anchors now include stable row identity, phone, position and offset; clearing search after recreation restores that anchor.
- Reset can reuse database IDs. It now replaces a persistent presentation identity, and old queries/details cannot restore into the replacement data. This identity grants no action authority.
- At 200% text in landscape, the fixed controls left less than a 48 dp list viewport. Landscape uses a single-row toolbar, the column respects its width after insets, and the bottom Apply action is shown only for eligible pending work with the required prerequisites.

The native `ui` mode checks actual Activity replacement, expanded details, filtered exact-number toggles, clearing search, scroll offsets and reset isolation. The test manifest targets only the `.debug` app; the runner also rejects any other target before a synthetic reset. Its data directory is separate from the signed app and receiver application.

## Layout and contrast checks

`scripts/ui-layout-tests.sh` runs 24 configurations: two themes, three font scales (100/150/200%), two orientations and two display densities (current baseline and 125%). Each case verifies the applied configuration, a list viewport of at least one 48 dp target, text layout, maximum-length number/long-label expansion, and reaching an expanded action through bounded native list scrolling. The script restores font scale, night mode and the original density override on exit.

Text overflow uses `Layout.getLineMax()` to exclude trailing whitespace; the initial width probe incorrectly counted trailing spaces as clipping. This follows the [Android Layout reference](https://developer.android.com/reference/android/text/Layout#getLineMax(int)). The numeric failure evidence distinguished a 1,212 px whitespace-inclusive extent from a 1,194 px visible extent in a 1,195 px layout.

The test draws only Business Gate's owned view hierarchy into PNG files with fictional data. It does not capture the device framebuffer or inspect receiver content. Local renderings are under `output/ui-layout/`; CI retains them with verification reports. A failed case is not a passing qualification record. `scripts/check_contrast.py` checks 28 declared text/surface pairs across both palettes, with a 4.5:1 minimum. The current minimum is 5.44:1; native controls and focus indicators still need their full rendered audit.

Run after building debug and instrumentation APKs:

```sh
ANDROID_HOME=/path/to/android-sdk GATE_TEST_SERIAL=emulator-5554 scripts/device-tests.sh
ANDROID_HOME=/path/to/android-sdk GATE_TEST_SERIAL=emulator-5554 scripts/ui-layout-tests.sh
```

The local API 36 arm64 run passed all 24 configurations (252 assertions) and produced 72 validated PNGs at 1080×2400/2400×1080 pixels and 420/525 dpi. The IME was closed. The 20 restoration checks, full 252-check core harness, 80 performance/capacity checks, restart stages and all build gates passed. CI repeats the matrix on its exact source commit before signed delivery. These results cover the declared owned views, not every dialog or the remaining accessibility/lifecycle matrix.

## Still required

- Real TalkBack traversal, stable keyboard/accessibility focus when rows move or recycle, and completion announcement behavior.
- IME-visible layouts, keyboard/Back behavior and both navigation modes on API 29 and newer versions.
- Every setup, settings, confirmation, recovery and long-content dialog; canceled or interrupted dialogs must not replay consent or a choice.
- Actual saved-task restoration after process death, receiving-account transitions while a dialog is open, and storage failure at each presentation boundary.
- The complete physical layout/performance and interruption matrix, including the visible Stop overlay during qualified connected-app actions.

The production compatibility registry remains empty. No image or synthetic UI result supplies missing receiver identity, selectors, sender delivery effects or physical qualification.
