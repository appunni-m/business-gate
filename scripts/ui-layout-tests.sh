#!/bin/sh
# Exercises only the separately installed debug app. Restore emulator display preferences on every exit.
set -eu
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Set ANDROID_HOME}"
: "${GATE_TEST_SERIAL:?Set GATE_TEST_SERIAL}"
case "$GATE_TEST_SERIAL" in emulator-*) ;; *) echo 'Use a dedicated emulator for owned layout tests.' >&2; exit 1;; esac
adb_bin="$ANDROID_HOME/platform-tools/adb"
layout_folder=${GATE_LAYOUT_REPORTS:-ui-layout}
case "$layout_folder" in ''|*[!a-z0-9-]*) echo 'Invalid owned rendering folder.' >&2; exit 1;; esac
layout_dir="output/$layout_folder"
original_font=$("$adb_bin" -s "$GATE_TEST_SERIAL" shell settings get system font_scale | tr -d '\r')
original_night=$("$adb_bin" -s "$GATE_TEST_SERIAL" shell cmd uimode night | awk '{print $NF}' | tr -d '\r')
density_state=$("$adb_bin" -s "$GATE_TEST_SERIAL" shell wm density | tr -d '\r')
original_override=$(printf '%s\n' "$density_state" | sed -n 's/^Override density: //p')
base_density=$(printf '%s\n' "$density_state" | tail -n 1 | awk '{print $NF}')
restore_display() {
    "$adb_bin" -s "$GATE_TEST_SERIAL" shell am force-stop io.github.appunnim.businessgate.debug
    if [ "$original_font" = null ]; then "$adb_bin" -s "$GATE_TEST_SERIAL" shell settings delete system font_scale >/dev/null; else "$adb_bin" -s "$GATE_TEST_SERIAL" shell settings put system font_scale "$original_font"; fi
    "$adb_bin" -s "$GATE_TEST_SERIAL" shell cmd uimode night "$original_night" >/dev/null
    if [ -n "$original_override" ]; then "$adb_bin" -s "$GATE_TEST_SERIAL" shell wm density "$original_override"; else "$adb_bin" -s "$GATE_TEST_SERIAL" shell wm density reset; fi
}
trap restore_display EXIT
trap 'exit 1' HUP INT TERM
"$adb_bin" -s "$GATE_TEST_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb_bin" -s "$GATE_TEST_SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
for display in ${GATE_LAYOUT_DISPLAYS:-standard large}; do
    if [ "$display" = large ]; then density=$((base_density * 5 / 4)); else density=$base_density; fi
    "$adb_bin" -s "$GATE_TEST_SERIAL" shell wm density "$density"
    for theme in ${GATE_LAYOUT_THEMES:-light dark}; do
        if [ "$theme" = dark ]; then night=yes; else night=no; fi
        "$adb_bin" -s "$GATE_TEST_SERIAL" shell cmd uimode night "$night" >/dev/null
        for scale in ${GATE_LAYOUT_SCALES:-normal large largest}; do
            case "$scale" in normal) font=1.0;; large) font=1.5;; largest) font=2.0;; *) exit 1;; esac
            "$adb_bin" -s "$GATE_TEST_SERIAL" shell settings put system font_scale "$font"
            for orientation in ${GATE_LAYOUT_ORIENTATIONS:-portrait landscape}; do
                mode="layout-$theme-$scale-$orientation-$display"
                "$adb_bin" -s "$GATE_TEST_SERIAL" shell am force-stop io.github.appunnim.businessgate.debug
                "$adb_bin" -s "$GATE_TEST_SERIAL" shell run-as io.github.appunnim.businessgate.debug rm -f cache/layout.png cache/layout-expanded.png cache/layout-action.png
                result=$("$adb_bin" -s "$GATE_TEST_SERIAL" shell am instrument -w -e mode "$mode" -e font "$font" -e night "$theme" -e orientation "$orientation" io.github.appunnim.businessgate.debug.test/io.github.appunnim.businessgate.GateInstrumentation)
                mkdir -p "$layout_dir"
                for view in layout layout-expanded layout-action; do
                    "$adb_bin" -s "$GATE_TEST_SERIAL" exec-out run-as io.github.appunnim.businessgate.debug cat "cache/$view.png" > "$layout_dir/$mode-$view.png" 2>/dev/null || true
                done
                if [ "$scale" = largest ]; then
                    for dialog in addNumber settings compatibility diagnostics accessDisclosure notificationDisclosure salesDisclosure clearLocal manualBlock unblockNow; do
                        "$adb_bin" -s "$GATE_TEST_SERIAL" exec-out run-as io.github.appunnim.businessgate.debug cat "cache/dialog-$dialog.png" > "$layout_dir/$mode-dialog-$dialog.png" 2>/dev/null || true
                    done
                fi
                printf '%s\n' "$result" | python3 scripts/assert_instrumentation.py "$mode"
                python3 -c 'from pathlib import Path; import sys; files=[Path(sys.argv[2]) / (sys.argv[1]+"-"+view+".png") for view in ("layout","layout-expanded","layout-action")]; assert all(path.read_bytes()[:8] == bytes([137,80,78,71,13,10,26,10]) for path in files), "Owned rendering missing or invalid"' "$mode" "$layout_dir"
            done
        done
    done
done
