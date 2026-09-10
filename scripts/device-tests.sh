#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
: "${GATE_TEST_SERIAL:?Set GATE_TEST_SERIAL to an isolated emulator serial}"
case "$GATE_TEST_SERIAL" in emulator-*) ;; *) echo 'Use a dedicated emulator for the synthetic fixture harness.' >&2; exit 1;; esac
adb_bin="$ANDROID_HOME/platform-tools/adb"
"$adb_bin" -s "$GATE_TEST_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb_bin" -s "$GATE_TEST_SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
for test_mode in all performance prepare-attention verify-attention prepare-recovery-entry verify-recovery-entry prepare-recovery-confirm verify-recovery-confirm; do
    "$adb_bin" -s "$GATE_TEST_SERIAL" shell am force-stop io.github.appunnim.businessgate.debug
    test_result=$("$adb_bin" -s "$GATE_TEST_SERIAL" shell am instrument -w -e mode "$test_mode" io.github.appunnim.businessgate.debug.test/io.github.appunnim.businessgate.GateInstrumentation)
    printf '%s\n' "$test_result" | python3 scripts/assert_instrumentation.py "$test_mode"
done
