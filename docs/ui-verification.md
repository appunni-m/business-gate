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

The local API 36 arm64 run passed all 24 configurations (252 assertions) and produced 72 validated PNGs at 1080×2400/2400×1080 pixels and 420/525 dpi. The IME was closed. The 20 restoration checks, full 253-check core harness, 80 performance/capacity checks, restart stages and all build gates passed. CI repeats the matrix on its exact source commit before signed delivery. These results cover the declared owned views, not every dialog or the remaining accessibility/lifecycle matrix.

## CI search-anchor regression

[Run 17](https://github.com/appunni-m/business-gate/actions/runs/34449903447) failed after core assertion 174, before the layout matrix or signed delivery. A local 1080×1920 reproduction reached the same timeout. The legacy test expected the first business after clearing search, although the updated UI correctly restores the previously scrolled position. The core test now selects the pending account by its stable fixture ID and checks the saved row and pixel offset after clearing search. It passes 253 assertions at both 1080×2400 and 1080×1920. The complete 24-case layout matrix also passes at 1080×1920/1920×1080 with 252 assertions and 72 validated renderings; original size, density, font and night mode were restored. The runner retains eight recent check labels on failure; an optional `trace` argument exposes the full owned assertion sequence. No wait or performance threshold was increased.

[Run 18](https://github.com/appunni-m/business-gate/actions/runs/34451758306) passed the 253 core assertions, then failed the `ui` mode after assertion 18. Its reset check assumed the saved-list empty card was visible below setup. The same timeout was reproduced at 1080×1920 and 525 dpi. The test now waits for the owned empty row by stable ID and scrolls it into view before checking its text. The corrected 20-check mode passes that compact configuration. Delivery was skipped for both failed runs.

The [storage-boundary follow-up](storage-boundary-verification.md) also clears prior presentation while the current data cannot be established, and distinguishes unavailable data from an empty saved list.

## Short-landscape correction

[Run 19](https://github.com/appunni-m/business-gate/actions/runs/34453034270) passed all ten native harness modes, including the 32 storage-boundary assertions, before the layout matrix found less than a 48 dp list viewport in landscape at 150% text and larger display scale. CI query p95 was 91 ms and its 50,000-record snapshot loaded in 2,457 ms; both unchanged performance limits passed. Delivery was skipped.

The layout failure was reproduced locally at 720×1280 with 450 dpi in landscape. The fixed landscape header now contains search, Pause and the menu in one row. The title and status remain in the same scrollable list, which retains the 600 dp maximum column width. Portrait retains its existing arrangement. Search remains directly accessible without scrolling to a separate control row. Native layout tests scroll to the exact matching fixture row before expanding it; they still enforce the original minimum viewport and action-reachability requirements. Failure output now includes the owned window dimensions, list height and density.

The full 24-case matrix passes at 720×1280/1280×720 and 360/450 dpi. An additional check in all 12 landscape configurations verifies that the title and status return after filtering, scrolling and row recycling. The current case set totals 264 assertions with 72 validated owned renderings. Original emulator size, density, theme and font settings were restored. These results do not establish IME-visible or TalkBack behavior.

## Still required

- Real TalkBack traversal, stable keyboard/accessibility focus when rows move or recycle, and completion announcement behavior.
- IME-visible layouts, keyboard/Back behavior and both navigation modes on API 29 and newer versions.
- Every setup, settings, confirmation, recovery and long-content dialog; canceled or interrupted dialogs must not replay consent or a choice.
- Actual saved-task restoration after process death, receiving-account transitions while a dialog is open, and storage failure at each presentation boundary.
- The complete physical layout/performance and interruption matrix, including the visible Stop overlay during qualified connected-app actions.

The production compatibility registry remains empty. No image or synthetic UI result supplies missing receiver identity, selectors, sender delivery effects or physical qualification.
