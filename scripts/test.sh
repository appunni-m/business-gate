#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
java_bin=${JAVA_HOME:+$JAVA_HOME/bin/}
test_dir=$(mktemp -d "${TMPDIR:-/tmp}/business-gate-tests.XXXXXX")
trap 'rm -rf "$test_dir"' EXIT HUP INT TERM
"${java_bin}javac" -encoding UTF-8 -d "$test_dir" \
 app/src/main/java/io/github/appunnim/businessgate/policy/*.java \
 app/src/main/java/io/github/appunnim/businessgate/automation/AutomationController.java \
 app/src/test/java/io/github/appunnim/businessgate/policy/CoreSuite.java
"${java_bin}java" -cp "$test_dir" io.github.appunnim.businessgate.policy.CoreSuite
python3 scripts/check_schema.py
python3 scripts/check_brand.py
