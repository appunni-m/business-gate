#!/bin/sh
# Every exit retains explicit failed/unrun cells. Only a complete matrix can pass.
set -eu
cd "$(dirname "$0")/.."
stage='reset evidence'
record_exit() {
    result=$?
    trap - EXIT
    if [ "$result" -ne 0 ] && [ "${GITHUB_ACTIONS:-}" = true ]; then
        echo "::error title=Owned device verification::Failed during $stage (exit $result); see the environment or instrumentation annotation for details."
    fi
    evidence=0
    python3 scripts/verification_evidence.py record || evidence=$?
    if [ "$result" -ne 0 ]; then exit "$result"; fi
    exit "$evidence"
}
trap record_exit EXIT
trap 'exit 1' HUP INT TERM
python3 scripts/verification_evidence.py reset
stage='collect environment'
python3 scripts/verification_evidence.py environment
stage='native state and lifecycle tests'
echo '::group::Native state, focus, dialogs and restart tests'
scripts/device-tests.sh
echo '::endgroup::'
echo '::group::Owned reminder permission, cap and channel tests'
stage='reminder tests'
python3 scripts/reminder-tests.py
echo '::endgroup::'
echo '::group::Saved-task process death'
stage='saved-task process death'
python3 scripts/saved-task-tests.py
echo '::endgroup::'
echo '::group::Owned responsive layout matrix'
stage='responsive layouts'
scripts/ui-layout-tests.sh
echo '::endgroup::'
echo '::group::Compact navigation and IME matrix'
stage='compact navigation'
python3 scripts/navigation-tests.py
echo '::endgroup::'
