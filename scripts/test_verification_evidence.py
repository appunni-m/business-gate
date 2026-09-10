#!/usr/bin/env python3
"""Evidence completeness regressions using explicitly synthetic reports and image headers."""
import json
import copy
import os
from pathlib import Path
import struct
import sys
import tempfile
import unittest
from unittest.mock import patch

import verification_evidence as evidence


def synthetic_navigation(api='36'):
    modes = ('interaction', 'layout-light-largest-portrait-large', 'layout-light-largest-landscape-large')
    return {'api': api, 'complete': True, 'settingsRestored': True, 'scope': 'Synthetic offline fixture', 'cases': [
        {'navigation': name, 'status': 'passed', 'environment': {'sourceCommit': 'a' * 40,
         'device': {'api': api}, 'runId': '1', 'workingTreeModified': False},
         'reports': [{'mode': mode, 'assertions': 1, 'sha256': 'b' * 64} for mode in modes],
         'images': [{'file': f'{mode}-{view}.png', 'sha256': 'c' * 64} for mode in modes if mode.startswith('layout-') for view in evidence.views(mode)]}
        for name in ('threebutton', 'gestural')]}


class EvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='business-gate-evidence-')
        self.root = Path(self.temporary.name)
        (self.root / 'device-tests').mkdir()
        (self.root / 'ui-layout').mkdir()
        (self.root / 'verification').mkdir()
        (self.root / 'verification/navigation-api36.json').write_text(json.dumps(synthetic_navigation()))
        for mode in evidence.ALL_MODES:
            (self.root / 'device-tests' / f'{mode}.txt').write_text(f'PASS 1 synthetic assertion; mode={mode}\n')
            if mode.startswith('layout-'):
                for view in evidence.views(mode):
                    (self.root / 'ui-layout' / f'{mode}-{view}.png').write_bytes(b'\x89PNG\r\n\x1a\n' + struct.pack('>I', 13) + b'IHDR' + struct.pack('>II', 320, 480))

    def tearDown(self):
        self.temporary.cleanup()

    def result(self):
        return evidence.matrix(self.root, {'sourceCommit': 'a' * 40, 'runId': '1', 'scope': 'synthetic offline fixture'})

    def test_navigation_must_cover_both_modes_and_restore_settings(self):
        path = self.root / 'verification/navigation-api36.json'
        path.unlink()
        self.assertFalse(self.result()['complete'])
        for key, value in (('complete', 'true'), ('settingsRestored', False), ('api', '29'), ('cases', [])):
            fixture = synthetic_navigation()
            fixture[key] = value
            path.write_text(json.dumps(fixture))
            self.assertFalse(self.result()['complete'])

    def test_navigation_rejects_stale_source_run_dirty_environment_and_partial_images(self):
        fixture = synthetic_navigation()
        for key, value in (('sourceCommit', 'e' * 40), ('runId', '2'), ('workingTreeModified', True)):
            changed = copy.deepcopy(fixture)
            changed['cases'][0]['environment'][key] = value
            self.assertFalse(evidence.valid_navigation(changed, '36', 'a' * 40, '1'))
        fixture['cases'][0]['images'][0]['file'] = 'unrelated.png'
        self.assertFalse(evidence.valid_navigation(fixture, '36', 'a' * 40, '1'))

    def test_navigation_rejects_zero_assertions_duplicates_and_bad_hashes(self):
        for key, value in (('assertions', 0), ('assertions', True), ('mode', 'wrong'), ('sha256', 'invalid')):
            fixture = synthetic_navigation()
            fixture['cases'][0]['reports'][0][key] = value
            self.assertFalse(evidence.valid_navigation(fixture, '36', 'a' * 40, '1'))
        fixture = synthetic_navigation()
        fixture['cases'][1] = fixture['cases'][0]
        self.assertFalse(evidence.valid_navigation(fixture, '36', 'a' * 40, '1'))

    def test_complete_inventory_hashes_every_report_and_owned_image(self):
        result = self.result()
        self.assertTrue(result['complete'])
        self.assertEqual(result['imageCount'], 152)
        self.assertEqual(result['assertions'], len(evidence.ALL_MODES))
        self.assertTrue(all(len(case['reportSha256']) == 64 for case in result['cases']))
        self.assertEqual(result['physicalQualification'], 'unrun')

    def test_missing_mode_is_unrun_and_cannot_pass(self):
        (self.root / 'device-tests/focus.txt').unlink()
        result = self.result()
        self.assertFalse(result['complete'])
        self.assertEqual(next(c for c in result['cases'] if c['mode'] == 'focus')['status'], 'unrun')

    def test_late_failure_and_wrong_mode_cannot_pass(self):
        for text in ('PASS 1 synthetic assertion; mode=all\nFAIL late crash\n', 'PASS 1 synthetic assertion; mode=other\n', 'PASS 0 synthetic assertions; mode=all\n'):
            (self.root / 'device-tests/all.txt').write_text(text)
            self.assertFalse(self.result()['complete'])

    def test_required_image_missing_or_invalid_fails_its_case(self):
        image = self.root / 'ui-layout' / f'{evidence.LAYOUT[0]}-layout.png'
        image.unlink()
        self.assertFalse(self.result()['complete'])
        image.write_bytes(b'not an image')
        result = self.result()
        self.assertFalse(result['complete'])
        self.assertEqual(next(c for c in result['cases'] if c['mode'] == evidence.LAYOUT[0])['status'], 'failed')

    def test_missing_environment_retains_failed_evidence(self):
        previous = Path.cwd()
        with tempfile.TemporaryDirectory(prefix='business-gate-empty-evidence-') as directory:
            try:
                os.chdir(directory)
                with patch.object(sys, 'argv', ['verification_evidence.py', 'record']):
                    with self.assertRaises(SystemExit):
                        evidence.main()
                result = json.loads(Path('output/verification/verification.json').read_text())
                self.assertFalse(result['complete'])
                self.assertTrue(result['environment']['missing'])
            finally:
                os.chdir(previous)

    def test_reset_removes_previous_case_files_but_preserves_other_outputs(self):
        previous = Path.cwd()
        with tempfile.TemporaryDirectory(prefix='business-gate-reset-evidence-') as directory:
            try:
                os.chdir(directory)
                Path('output/device-tests').mkdir(parents=True)
                Path('output/device-tests/all.txt').write_text('old pass')
                Path('output/device-tests/other.txt').write_text('unrelated result')
                with patch.object(sys, 'argv', ['verification_evidence.py', 'reset']):
                    evidence.main()
                self.assertFalse(Path('output/device-tests/all.txt').exists())
                self.assertEqual(Path('output/device-tests/other.txt').read_text(), 'unrelated result')
            finally:
                os.chdir(previous)


if __name__ == '__main__':
    unittest.main()
