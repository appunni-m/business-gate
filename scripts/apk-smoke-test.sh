#!/bin/sh
# Run only on a disposable emulator; release data is left in place to test upgrades.
set -eu
: "${ANDROID_HOME:?Set ANDROID_HOME}"
: "${GATE_TEST_SERIAL:?Set GATE_TEST_SERIAL}"
case "$GATE_TEST_SERIAL" in emulator-*) ;; *) echo 'Use an isolated emulator.' >&2; exit 1;; esac
adb_bin="$ANDROID_HOME/platform-tools/adb"
apk=${1:?Provide the signed APK}
package=io.github.appunnim.businessgate
"$adb_bin" -s "$GATE_TEST_SERIAL" install -r "$apk"
"$adb_bin" -s "$GATE_TEST_SERIAL" shell am force-stop "$package"
launch=$("$adb_bin" -s "$GATE_TEST_SERIAL" shell am start -W -n "$package/.ui.MainActivity")
echo "$launch"
case "$launch" in *'Status: ok'*) ;; *) exit 1;; esac
"$adb_bin" -s "$GATE_TEST_SERIAL" shell input keyevent KEYCODE_WAKEUP
"$adb_bin" -s "$GATE_TEST_SERIAL" shell wm dismiss-keyguard
"$adb_bin" -s "$GATE_TEST_SERIAL" shell pidof "$package"
# The separate probe verifies rendering through package-checked Business Gate nodes.
echo 'PASS signed release installation and activity start'
