#!/usr/bin/env python3
"""Release evidence rejection tests; all reports, APK bytes and provenance are synthetic."""
import copy
import json
import os
from pathlib import Path
import runpy
import tempfile
import unittest
from unittest.mock import patch
import zipfile

from verification_evidence import ALL_MODES
from test_verification_evidence import synthetic_navigation

ROOT = Path(__file__).resolve().parents[1]


class MetadataTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='business-gate-metadata-test-')
        self.previous = Path.cwd()
        os.chdir(self.temp.name)
        for folder in ('output/delivery', 'output/verification', 'output/device-tests', 'config'):
            Path(folder).mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile('output/delivery/business-gate.apk', 'w') as archive:
            assets = ROOT / 'app/src/main/assets'
            for path in assets.rglob('*'):
                if path.is_file():
                    archive.writestr('assets/' + str(path.relative_to(assets)), path.read_bytes())
        Path('config/signing-certificate.sha256').write_text('b' * 64)
        env = {'sourceCommit': 'a' * 40, 'workingTreeModified': False, 'runId': '1', 'device': {'api': '36'}}
        minimum = dict(copy.deepcopy(env), device={'api': '29'})
        self.evidence = {'complete': True, 'environment': env, 'imageCount': 152,
                         'navigation': {api: synthetic_navigation(api) for api in ('29', '36')},
                         'cases': [{'mode': mode, 'status': 'passed', 'assertions': 1} for mode in ALL_MODES],
                         'minimumUi': {'environment': minimum, 'cases': [
                             {'mode': mode, 'status': 'passed', 'assertions': 1} for mode in
                             ('ui', 'focus', 'dialogs', 'interaction', 'service-lifecycle', 'prepare-task', 'verify-task', 'reminder', 'reminder-channel')]}}
        Path('output/verification/environment.json').write_text(json.dumps(minimum))
        for mode in ('prepare-upgrade', 'verify-upgrade'):
            Path('output/device-tests', mode + '.txt').write_text(f'PASS 1 installed-release assertions; mode={mode}\n')

    def tearDown(self):
        os.chdir(self.previous)
        self.temp.cleanup()

    def generate(self):
        Path('output/delivery/verification.json').write_text(json.dumps(self.evidence))
        env = {'ANDROID_HOME': '/synthetic-sdk', 'GITHUB_REPOSITORY': 'appunni-m/business-gate',
               'GITHUB_SHA': 'a' * 40, 'GITHUB_RUN_ID': '1'}
        badging = "package: name='io.github.appunnim.businessgate' versionCode='201' versionName='synthetic'\nsdkVersion:'29'\ntargetSdkVersion:'36'\n"
        with patch.dict(os.environ, env), patch('subprocess.check_output', return_value=badging):
            runpy.run_path(str(ROOT / 'scripts/release_metadata.py'), run_name='__main__')

    def test_complete_evidence_binds_all_three_asset_hashes(self):
        self.generate()
        import hashlib
        info = json.loads(Path('output/delivery/build-info.json').read_text())
        manifest = Path('output/delivery/verification.json')
        self.assertEqual(info['verificationSha256'], hashlib.sha256(manifest.read_bytes()).hexdigest())
        self.assertEqual(len(Path('output/delivery/SHA256SUMS').read_text().splitlines()), 3)
        self.assertTrue(info['connectedAppActionsEnabled'])
        self.assertEqual(info['supportLevel'], 'emulator-experimental')
        self.assertEqual(info['qualifiedAdapters'], [])
        self.assertIsNone(json.loads(manifest.read_text())['signedUpgrade']['previousApkSha256'])

    def test_incomplete_or_nonboolean_completion_is_rejected(self):
        for value in (False, 'true', 1):
            self.evidence['complete'] = value
            with self.assertRaises(SystemExit):
                self.generate()

    def test_wrong_source_or_dirty_build_is_rejected(self):
        for key, value in (('sourceCommit', 'e' * 40), ('workingTreeModified', True), ('runId', '2')):
            old = self.evidence['environment'][key]
            self.evidence['environment'][key] = value
            with self.assertRaises(SystemExit):
                self.generate()
            self.evidence['environment'][key] = old

    def test_missing_case_or_image_is_rejected(self):
        self.evidence['cases'].pop()
        with self.assertRaises(SystemExit):
            self.generate()

    def test_missing_minimum_ui_is_rejected(self):
        self.evidence.pop('minimumUi')
        with self.assertRaises(SystemExit):
            self.generate()

    def test_missing_or_stale_navigation_is_rejected(self):
        original = copy.deepcopy(self.evidence['navigation'])
        for api in ('29', '36'):
            self.evidence['navigation'] = copy.deepcopy(original)
            self.evidence['navigation'].pop(api)
            with self.assertRaises(SystemExit):
                self.generate()
            self.evidence['navigation'] = copy.deepcopy(original)
            self.evidence['navigation'][api]['cases'][0]['environment']['runId'] = '2'
            with self.assertRaises(SystemExit):
                self.generate()

    def test_wrong_minimum_upgrade_environment_is_rejected(self):
        Path('output/verification/environment.json').write_text(json.dumps(self.evidence['environment']))
        with self.assertRaises(SystemExit):
            self.generate()

    def test_zero_or_late_failed_upgrade_cannot_be_released(self):
        for report in ('PASS 0 installed-release assertions; mode=verify-upgrade\n',
                       'PASS 1 installed-release assertions; mode=verify-upgrade\nFAIL after reporting\n'):
            Path('output/device-tests/verify-upgrade.txt').write_text(report)
            with self.assertRaises(SystemExit):
                self.generate()


if __name__ == '__main__':
    unittest.main()
