# Storage publication boundaries

This is owned engineering evidence for F05/F06/F24/F26. It does not establish connected-app or physical qualification.

## Reproduction

An isolated debug fixture installed a temporary SQLite trigger that removed the replacement presentation identity during reset. The deletion transaction committed, then the subsequent read failed. Before the fix, the database held zero account rows while the cached snapshot and search each returned six deleted fictional records. The failure was reproduced by the native `privacy-boundary` instrumentation mode. Its manifest and runtime package guard restrict it to the debug data directory; every temporary trigger is removed in a `finally` block.

## Resulting behavior

The serial writer clears the previous published projection when it starts a reset, receiving-namespace switch, or installation-identity replacement. Until storage establishes the current data, the snapshot is unloaded, its binding and search results are empty, and choices cannot be submitted from that state. Snapshot, search projection and presentation identity are published together after the required reads succeed.

The UI clears the previous query, expanded number and anchors when presentation identity is unavailable. It asks the user to check storage. It does not display a saved-list empty state or claim that previous choices remain after a reset whose result could not be fully read.

This temporary loss of presentation does not erase durable choices. If reset rolls back, an explicit storage check loads the unchanged choices and keeps actions paused. If a receiving-namespace change commits, recovery loads that namespace alone; seeing the original namespace's choices requires an explicit return. Ordinary read failures within the same established namespace retain the last committed search projection, as covered by the existing storage-recovery regressions.

## Native checks

The `privacy-boundary` mode passes 32 assertions covering:

- A committed reset followed by failed publication: zero durable account rows, zero cached/search rows, cleared exact-number presentation, no false acknowledgement or resumed authority.
- A reset aborted before commit: all six original records and two ALLOW choices remain durable; explicit storage recovery reveals them without repeating the reset.
- A committed receiver switch followed by failed publication: no old binding or searchable records; explicit recovery loads only the committed receiver; an explicit return restores the original namespace's saved choices.

The synthetic receiver identifiers, digest and fault triggers are test-only fixtures. They are not measured integration data. Local reproduction and fixed-run output are retained under `/tmp/business-gate/` during execution. The regular device script and CI include the new mode.

The complete local run also passed 253 core checks, 20 Activity/restoration checks, 80 performance/capacity checks, six restart stages and all 24 compact layout configurations (252 assertions, 72 validated owned renderings). Pure/schema/brand/wrapper gates, debug/release lint, APK/bundle/probe builds, actionlint and the release audit passed. The release audit found 132 mapped app/compiler classes and no runtime SDK, native library or test harness.

Remaining qualification includes real storage faults, installation/restore transitions, process death at every boundary and the complete physical-device matrix. These owned checks do not complete those scenarios.
