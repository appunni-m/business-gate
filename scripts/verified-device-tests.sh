#!/bin/sh
# Every exit retains explicit failed/unrun cells. Only a complete matrix can pass.
set -eu
cd "$(dirname "$0")/.."
record_exit() {
    result=$?
    trap - EXIT
    evidence=0
    python3 scripts/verification_evidence.py record || evidence=$?
    if [ "$result" -ne 0 ]; then exit "$result"; fi
    exit "$evidence"
}
trap record_exit EXIT
trap 'exit 1' HUP INT TERM
python3 scripts/verification_evidence.py reset
python3 scripts/verification_evidence.py environment
echo '::group::Native state, focus, dialogs and restart tests'
scripts/device-tests.sh
echo '::endgroup::'
echo '::group::Owned reminder permission, cap and channel tests'
python3 scripts/reminder-tests.py
echo '::endgroup::'
echo '::group::Saved-task process death'
python3 scripts/saved-task-tests.py
echo '::endgroup::'
echo '::group::Owned responsive layout matrix'
scripts/ui-layout-tests.sh
echo '::endgroup::'
echo '::group::Compact navigation and IME matrix'
python3 scripts/navigation-tests.py
echo '::endgroup::'
