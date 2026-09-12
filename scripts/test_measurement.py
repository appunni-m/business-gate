#!/usr/bin/env python3
"""Report transport regressions for the isolated developer probe."""
import importlib.util
import json
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('measure_installation', Path(__file__).with_name('measure-installation.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class MeasurementReports(unittest.TestCase):
    def valid(self):
        return {'schemaVersion': 1, 'result': 'observed', 'physicalQualification': False, 'mutationPerformed': False}

    def wire(self, value):
        return json.dumps(value)

    def test_success(self):
        self.assertEqual(module.parse_report(self.wire(self.valid())), self.valid())

    def test_scope_cannot_be_promoted(self):
        for key in ('physicalQualification', 'mutationPerformed'):
            value = self.valid(); value[key] = True
            with self.assertRaises(ValueError): module.parse_report(self.wire(value))

    def test_missing_and_duplicate(self):
        raw = self.wire(self.valid())
        for value in ('', raw + raw):
            with self.assertRaises(ValueError): module.parse_report(value)

    def test_false_success(self):
        for value in (self.wire({**self.valid(), 'result': 'failed', 'reason': 'FOREGROUND_CHANGED'}), self.wire(self.valid()) + 'FAIL FOREGROUND_CHANGED\n'):
            with self.assertRaises(ValueError): module.parse_report(value)

    def test_unknown_schema_and_missing_scope(self):
        for value in ({}, {**self.valid(), 'schemaVersion': 2}):
            with self.assertRaises(ValueError): module.parse_report(self.wire(value))

    def test_bounded_output(self):
        with self.assertRaises(ValueError): module.parse_report(self.wire(self.valid()) + 'x' * 32_000)

    def test_rotated_signer_uses_measured_android_version(self):
        earlier, later = 'a' * 64, 'b' * 64
        raw = f'Signer (minSdkVersion=24, maxSdkVersion=32) certificate SHA-256 digest: {earlier}\nSigner (minSdkVersion=33, maxSdkVersion=2147483647) certificate SHA-256 digest: {later}'
        self.assertEqual(module.selected_signer(raw, 29), [earlier])
        self.assertEqual(module.selected_signer(raw, 36), [later])
        with self.assertRaises(ValueError): module.selected_signer(raw, 23)
        with self.assertRaises(ValueError): module.selected_signer(raw + '\n' + raw, 36)

    def test_single_signer_and_ambiguous_or_unknown_format(self):
        raw = 'Signer #1 certificate SHA-256 digest: ' + 'a' * 64
        self.assertEqual(module.selected_signer(raw, 36), ['a' * 64])
        for bad in ('', raw + '\n' + raw, raw.replace('Signer #1', 'Unknown signer')):
            with self.assertRaises(ValueError): module.selected_signer(bad, 36)


if __name__ == '__main__':
    unittest.main()
