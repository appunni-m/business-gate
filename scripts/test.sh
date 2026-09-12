#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
java_bin=${JAVA_HOME:+$JAVA_HOME/bin/}
test_dir=$(mktemp -d "${TMPDIR:-/tmp}/business-gate-tests.XXXXXX")
trap 'rm -rf "$test_dir"' EXIT HUP INT TERM
"${java_bin}javac" -encoding UTF-8 -d "$test_dir" \
 app/src/main/java/io/github/appunnim/businessgate/policy/*.java \
 app/src/main/java/io/github/appunnim/businessgate/automation/AutomationController.java \
 app/src/main/java/io/github/appunnim/businessgate/automation/BoundedNodes.java \
 app/src/main/java/io/github/appunnim/businessgate/automation/FinalDispatch.java \
 app/src/test/java/io/github/appunnim/businessgate/policy/*.java
"${java_bin}java" -cp "$test_dir" io.github.appunnim.businessgate.policy.CoreSuite
"${java_bin}java" -cp "$test_dir" io.github.appunnim.businessgate.policy.StructuralSuite
python3 scripts/check_schema.py
python3 scripts/check_contrast.py
python3 scripts/check_registry.py
python3 scripts/test_resource_paths.py
python3 scripts/check_traceability.py
python3 scripts/test_delivery.py
python3 scripts/test_public_release.py
python3 scripts/test_verification_evidence.py
python3 scripts/test_navigation.py
python3 scripts/test_release_metadata.py
"${java_bin}javac" -encoding UTF-8 -d "$test_dir" \
 qualification-probe/src/main/java/io/github/appunnim/businessgate/measure/ExactPath.java \
 qualification-probe/src/main/java/io/github/appunnim/businessgate/measure/Neutral.java \
 qualification-probe/src/test/java/io/github/appunnim/businessgate/measure/MeasurementSuite.java
"${java_bin}java" -cp "$test_dir" io.github.appunnim.businessgate.measure.MeasurementSuite
python3 scripts/test_measurement.py
python3 scripts/check_brand.py
