#!/bin/sh
# The probe is self-instrumenting and uses its own test key; the publisher secret is never exposed.
set -eu
: "${ANDROID_HOME:?Set ANDROID_HOME}"
: "${GATE_TEST_SERIAL:?Set GATE_TEST_SERIAL}"
case "$GATE_TEST_SERIAL" in emulator-*) ;; *) echo 'Use an isolated emulator.' >&2; exit 1;; esac
adb_bin="$ANDROID_HOME/platform-tools/adb"
probe=${1:?Provide the separate test APK}
current=${2:?Provide the signed release APK}
previous=${3:-}
"$adb_bin" -s "$GATE_TEST_SERIAL" install -r "$probe"
trap '"$adb_bin" -s "$GATE_TEST_SERIAL" uninstall io.github.appunnim.businessgate.probe >/dev/null 2>&1 || true' EXIT HUP INT TERM
run_probe() {
    result=$("$adb_bin" -s "$GATE_TEST_SERIAL" shell am instrument -w -e mode "$1" io.github.appunnim.businessgate.probe/.ReleaseProbe)
    echo "$result"
    case "$result" in *FAIL*|*INSTRUMENTATION_FAILED*) exit 1;; esac
    case "$result" in *PASS*) ;; *) exit 1;; esac
}
if [ -n "$previous" ] && [ -f "$previous" ]; then
    scripts/apk-smoke-test.sh "$previous"
    run_probe prepare-upgrade
    scripts/apk-smoke-test.sh "$current"
    run_probe verify-upgrade
else
    scripts/apk-smoke-test.sh "$current"
    run_probe prepare-upgrade
    scripts/apk-smoke-test.sh "$current"
    run_probe verify-upgrade
fi
