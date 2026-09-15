# Business Gate

Choose the businesses. Keep the people.

A native Android app that applies business blocking rules using explicitly enabled business names. It uses an original blue gate identity, Java, and Android framework components. No third-party runtime libraries, login, analytics, network permission, ads, or subscription.

**Experimental: supported on one measured Android 36 emulator installation.** The optional **Sender name filter** dismisses direct notifications matching ten public profile names, 55 supplied sender-name rules, or names learned from verified profiles, unless whitelisted. Opening Business Gate starts a finite visible cleanup with Stop and fresh native identity checks. A shared summary can retain a business preview because cancelling it can also remove personal notifications. Physical devices remain unqualified. The first message may appear; existing chats remain. See the [displayed sender design and evidence](docs/display-sender-filter-design.md).

## Try the native interface

[**Download the latest installable APK**](https://github.com/appunni-m/business-gate/releases/download/development/business-gate.apk) · [All builds and checksums](https://github.com/appunni-m/business-gate/releases) · [Build status](https://github.com/appunni-m/business-gate/actions/workflows/android.yml)

Requires **Android 10 or newer**. Open the APK and allow installation from your browser or file manager when Android asks. No GitHub login is needed to download. Each successful `main` build publishes a signed development APK; later downloads install as updates and preserve local choices. Connected actions require the exact experimental environment described in [the design](docs/name-visibility-design.md); other installations keep local choices without running actions.

To build locally:

Use JDK 17, Android SDK Platform 36 and Build Tools 35.0.0. The verified wrapper pins Gradle 8.13; the build pins Android Gradle Plugin 8.13.2. Set `JAVA_HOME` and `ANDROID_HOME` to your installations.

```sh
./gradlew :app:assembleDebug
"$ANDROID_HOME/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

Open **Business Gate**. Choose **Enable a number** from the overflow menu, enter the full international number and an optional local label, and save. Your preference persists without screen access. Search matches local names and number digits. The app explains that actions are paused; saving a choice never pretends to have changed another app.

On the supported emulator, use **Compatibility & help** to select and verify the receiving account. Then choose **Inspect business number**. Confirmed businesses follow the business-name switch; exact-number exceptions remain in the details. Each operation is a visible session with Stop. Opening a chat may mark messages as read.

There are no sample accounts on a normal installation. Screenshots and the test APK use fictional accounts inserted only by instrumentation.

## Verify a change

```sh
scripts/test.sh
./gradlew :app:lintDebug :app:lintRelease :app:assembleDebugAndroidTest \
  :app:assembleDebug :app:assembleRelease :app:bundleRelease \
  :release-probe:lintDebug :release-probe:assembleDebug
python3 scripts/check_brand.py app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/bundle/release/app-release.aab
```

For the native test harness, use an isolated emulator:

```sh
export GATE_TEST_SERIAL=emulator-5554
scripts/device-tests.sh
```

The device script deliberately rejects physical device serials because it resets local app data and inserts fictional test records. It checks actual process-restart recovery, the shipped-schema migration, receiver isolation, cancellation races, capacity, search, and rendered UI. The pure suite does not require Android or a network connection.

The delivery workflow signs the verified release APK with a private publisher key stored in an encrypted repository secret. It verifies the signature, installation, and a saved exact-number choice through an update before publishing a versioned GitHub prerelease and updating the continuous download link. The release includes checksums and source/build metadata. Pull requests only verify code. Signing keys are never committed.

Local Gradle release APK/AAB files remain unsigned; debug artifacts use the local Android debug key and a separate application ID. These are development distributions, not store releases. See [delivery and signing](docs/delivery.md) for the one-time setup, automatic checks, updates, and failure recovery.

## What the screen does

- One native page with pinned search, enabled businesses first, expandable account details, and truthful pending/observed subtitles.
- Separate exact-number exceptions and explicit business-name permissions. Optional local contact labels never create a name permission. Number-specific ALLOW or manual DENY takes precedence over the name rule.
- Setup and access disclosures, pause, compatibility explanation, offline privacy information, local diagnostics and effort totals, and confirmed local reset.
- System light/dark appearance, growing rows, native switches, 48 dp controls, and a centered single column on wide screens.

Connect a supported receiving account and allow notification access, then open **More → Sender name filter → Enable name filtering**. Review the exact-name disclosure once. **Review names and whitelist** lists public profile names, supplied sender rules and locally learned names, with their provenance. The supplied list is Base64-encoded in the repository and decoded locally; it is not encrypted and does not establish business identity. Whitelisting keeps matching notifications immediately; use **Apply pending** to finish native Unblock for previously verified businesses.

Unknown senders stay visible: use **Review unknown incoming senders** to inspect a supported business profile and learn its name. Name rules can also match a personal sender with the same name; they never prove identity. Exact-number permissions are preserved, and unresolved exceptions keep notifications visible. Hidden routes expire after 15 minutes or process/listener loss; history remains, and **Inspect business number** lets you finish a fresh check without guessing a number from a name. Cleanup handles up to five available conversations in three minutes. Stop disables automatic cleanup until **Clear pending businesses now** explicitly resumes it.

The switch represents your choice. The subtitle represents a pending operation or a separately observed outcome. Pausing or deleting local data does not undo blocks in another app. The first message can arrive before discovery.

## Continue implementation and qualification

The [displayed sender design](docs/display-sender-filter-design.md) extends the [business-name policy](docs/name-visibility-design.md) and [incoming session](docs/incoming-notification-design.md). A first preview may appear and existing chats remain. The earlier designs excluded name-based permission and notification suppression; those exclusions no longer describe the requested product.

[Execution status](docs/execution-status.md) gives every planned fix a disposition. [Implementation and evidence](docs/implementation.md), the [completion plan](docs/implementation-plan.md) and [acceptance ledger](docs/acceptance-ledger.json) distinguish engineering tests from physical qualification. [Adapter qualification](docs/qualification.md) defines the physical inputs and stop conditions. [Privacy](docs/privacy.md) describes the binary's data boundary. [Contributing](CONTRIBUTING.md) describes the development workflow.

The neutralized [full specification](docs/design/Business_Gate_Full_PRD_Design.md), [companion specification](docs/design/Business_Gate_Complete_PRD_Design.md), and [HTML design reference](docs/design/prototype.html) preserve the supplied requirements. They are design inputs, not evidence of completed features. The full specification's added review/manual-choice model is used; independent blue branding overrides its accent choice.

No open-source license has been selected by the owner. Repository visibility alone does not grant an open-source license. The Gradle wrapper is distributed under its upstream license; see [third-party notices](THIRD_PARTY_NOTICES.md).

## Native screenshots

API 36 emulator, fictional test records, connected-app actions inactive. [Large text](docs/screenshots/large-text.png) uses a 320 dp viewport and 200% font scale.

<img src="docs/screenshots/light.png" alt="Light theme showing saved exact-number choices and a compatibility explanation" width="280"> <img src="docs/screenshots/dark.png" alt="The same native management page in system dark mode" width="280">
