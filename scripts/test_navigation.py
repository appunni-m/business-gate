#!/usr/bin/env python3
"""Synthetic navigation states exercise restoration of Android's base resource mode."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('navigation_tests', Path(__file__).with_name('navigation-tests.py'))
navigation = importlib.util.module_from_spec(spec)
spec.loader.exec_module(navigation)

PREFIX = 'com.android.internal.systemui.navbar.'


class NavigationTests(unittest.TestCase):
    def state(self, active=(), available=('threebutton', 'twobutton', 'gestural'), mode='0'):
        def run(*args):
            if 'list' in args:
                return 'android\n' + '\n'.join(('[x] ' if name in active else '[ ] ') + PREFIX + name for name in available)
            self.assertEqual(args[-2:], ('android', 'android:integer/config_navBarInteractionMode'))
            return mode
        return navigation.navigation_snapshot(run)

    def test_base_resource_mode_needs_no_enabled_overlay(self):
        for mode in ('0', '2', '0x00000000'):
            state = self.state(mode=mode)
            self.assertEqual(state['active'], [])
            self.assertEqual(state['mode'], int(mode, 0))
            self.assertEqual(navigation.restore_navigation_commands(state), [
                ('shell', 'cmd', 'overlay', 'disable', '--user', '0', package) for package in state['available']])

    def test_enabled_overlay_restores_exact_original_selection(self):
        for name, mode in (('threebutton', '0'), ('twobutton', '1'), ('gestural', '2')):
            state = self.state(active=(name,), mode=mode)
            self.assertEqual(navigation.restore_navigation_commands(state), [
                ('shell', 'cmd', 'overlay', 'enable-exclusive', '--category', '--user', '0', PREFIX + name)])

    def test_missing_required_navigation_mode_fails(self):
        with self.assertRaisesRegex(RuntimeError, 'Navigation setup unavailable'):
            self.state(available=('threebutton',))

    def test_ambiguous_enabled_overlays_fail(self):
        with self.assertRaisesRegex(RuntimeError, 'Navigation setup unavailable'):
            self.state(active=('threebutton', 'gestural'))

    def test_unknown_resource_cannot_be_treated_as_a_supported_mode(self):
        for mode in ('3', '', 'missing resource'):
            with self.assertRaises(RuntimeError):
                self.state(mode=mode)

    def test_restoration_detects_same_visible_mode_with_different_overlay_selection(self):
        self.assertNotEqual(self.state(), self.state(active=('threebutton',)))

    def test_probe_modes_require_a_successful_raw_instrumentation_result(self):
        for mode in ('0', '1', '2'):
            self.assertEqual(navigation.parse_probe_navigation('INSTRUMENTATION_RESULT: stream=NAVIGATION_MODE ' + mode + '\n\nINSTRUMENTATION_CODE: -1\n'), mode)

    def test_probe_rejects_failure_missing_status_duplicate_and_unknown_modes(self):
        for report in ('NAVIGATION_MODE 0\n', 'INSTRUMENTATION_RESULT: stream=NAVIGATION_MODE 0\nINSTRUMENTATION_CODE: 0',
                       'INSTRUMENTATION_RESULT: stream=NAVIGATION_MODE 3\nINSTRUMENTATION_CODE: -1',
                       'INSTRUMENTATION_RESULT: stream=NAVIGATION_MODE 0\nINSTRUMENTATION_RESULT: stream=NAVIGATION_MODE 2\nINSTRUMENTATION_CODE: -1',
                       'INSTRUMENTATION_RESULT: stream=NAVIGATION_MODE 0\nFAIL synthetic error\nINSTRUMENTATION_CODE: -1'):
            with self.assertRaises(RuntimeError):
                navigation.parse_probe_navigation(report)

    def test_early_failure_is_retained_and_public_without_invoking_a_device(self):
        with tempfile.TemporaryDirectory(prefix='business-gate-navigation-') as directory:
            result = subprocess.run([sys.executable, str(Path(navigation.__file__).resolve())], cwd=directory,
                                    env=dict(os.environ, GATE_TEST_SERIAL='not-an-emulator', GITHUB_ACTIONS='true'),
                                    capture_output=True, text=True, timeout=10)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn('::error title=Owned navigation verification::', result.stdout)
            self.assertIn('Use an isolated emulator', result.stdout)
            evidence = json.loads((Path(directory) / 'output/verification/navigation-failure.json').read_text())
            self.assertIs(evidence['complete'], False)


if __name__ == '__main__':
    unittest.main()
