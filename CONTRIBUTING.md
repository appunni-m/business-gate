# Contributing

Read [AGENTS.md](AGENTS.md) first. Keep the original blue identity and the neutral naming rule across code, assets, documentation, fixtures and commit messages. Run `scripts/test.sh` before every push. Do not introduce a runtime dependency or broaden permission scope without a documented reason and review.

Use JDK 17 and the pinned Gradle wrapper. Source is Java with Android framework widgets and SQLite; tests use plain Java and a test-only platform Instrumentation runner. Format new code readably, preserve explicit invariants, and keep production services separate from UI and persistence. All disk work belongs on the repository's serial executor.

For a change to action policy, add a failing pure safety test. For persistence, exercise the actual schema and Android repository. For UI, build and inspect the changed state in both themes and at large text sizes. Run the native test harness on an isolated emulator; it replaces local development-app data with fictional records.

Do not log real names, numbers, message text, raw notification objects or UI trees. A fixture is synthetic until its physical provenance is documented. Adding fake evidence to the compatibility registry is a release-blocking error.

No public API is promised at version 0.1.0. New schemas require explicit, non-destructive upgrade tests from every shipped version. Version 1 is a new-install schema; there is no invented predecessor migration.

Contribution licensing terms remain a publisher decision. Do not assume a license from repository visibility. Discuss external contributions with the owner before submitting code.
