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

    def root_report(self):
        return {**self.valid(), 'mode': 'root', 'screenClassification': 'unclassified',
                'measurement': {'path': [], 'ancestors': [], 'acquiredNodes': 1,
                                'activeFocusedWindow': True, 'selectedText': {'inspected': False}}}

    def test_root_has_no_text_or_children(self):
        value = self.root_report()
        self.assertEqual(module.parse_report(self.wire(value), 'root'), value)
        for key, invalid in (('path', [0]), ('ancestors', [{}]), ('acquiredNodes', 2),
                             ('acquiredNodes', True), ('activeFocusedWindow', False),
                             ('selectedText', {'inspected': True}),
                             ('selectedText', {'inspected': False, 'present': True})):
            changed = self.root_report(); changed['measurement'][key] = invalid
            with self.subTest(key=key, invalid=invalid), self.assertRaises(ValueError):
                module.parse_report(self.wire(changed), 'root')

    def test_root_cannot_classify_profile_or_substitute_mode(self):
        for field, value in (('mode', 'node'), ('screenClassification', 'receiver')):
            changed = self.root_report(); changed[field] = value
            with self.assertRaises(ValueError): module.parse_report(self.wire(changed), 'root')
        with self.assertRaises(ValueError): module.parse_report(self.wire(self.valid()), 'root')

    def test_structure_can_select_a_path_but_cannot_export_text(self):
        value = {**self.root_report(), 'mode': 'structure'}
        value['measurement'].update(path=[0], ancestors=[{}], acquiredNodes=2)
        self.assertEqual(module.parse_report(self.wire(value), 'structure'), value)
        value['measurement']['selectedText'] = {'inspected': True, 'present': True}
        with self.assertRaises(ValueError): module.parse_report(self.wire(value), 'structure')

    def test_read_only_modes_reject_focus_or_description_access(self):
        for key, invalid in (('inputFocusRequested', True), ('selectedDescription', {'inspected': True, 'present': True})):
            value = self.root_report(); value['measurement'][key] = invalid
            with self.assertRaises(ValueError): module.parse_report(self.wire(value), 'root')

    def test_focus_requires_observed_focus_and_active_window(self):
        value = {**self.root_report(), 'mode': 'focus-tab'}
        value['measurement'].update(path=[7, 3], inputFocusRequested=True, node={'focused': True})
        self.assertEqual(module.parse_report(self.wire(value), 'focus-tab'), value)
        for key, invalid in (('inputFocusRequested', False), ('node', {'focused': False}), ('activeFocusedWindow', False)):
            changed = {**value, 'measurement': {**value['measurement'], key: invalid}}
            with self.assertRaises(ValueError): module.parse_report(self.wire(changed), 'focus-tab')

    def test_navigation_titles_require_mode_surface_and_safe_text(self):
        value = {**self.root_report(), 'mode': 'navigation-label', 'surfaceAttestation': 'navigation'}
        value['measurement']['selectedText'] = {'inspected': True, 'navigationLabel': 'Settings X'}
        self.assertEqual(module.parse_report(self.wire(value), 'navigation-label'), value)
        for label in ('', '+12025550100', 'a.b', 'a\n', 'x' * 41, None):
            changed = {**value, 'measurement': {**value['measurement'], 'selectedText': {'inspected': True, 'navigationLabel': label}}}
            with self.assertRaises(ValueError): module.parse_report(self.wire(changed), 'navigation-label')
        value['surfaceAttestation'] = 'receiver'
        with self.assertRaises(ValueError): module.parse_report(self.wire(value), 'navigation-label')
        value['mode'] = 'node'
        with self.assertRaises(ValueError): module.parse_report(self.wire(value), 'node')

    def test_profile_navigation_is_explicit_and_not_a_focus_result(self):
        value = {**self.root_report(), 'mode': 'open-profile'}
        value['measurement'].update(path=[0, 4], ancestors=[{}, {}], acquiredNodes=3, navigationAction='open-profile')
        self.assertEqual(module.parse_report(self.wire(value), 'open-profile'), value)
        for key, invalid in (('navigationAction', 'save-profile'), ('activeFocusedWindow', False), ('inputFocusRequested', True)):
            changed = {**value, 'measurement': {**value['measurement'], key: invalid}}
            with self.assertRaises(ValueError): module.parse_report(self.wire(changed), 'open-profile')
        value['mode'] = 'structure'
        with self.assertRaises(ValueError): module.parse_report(self.wire(value), 'structure')

    def test_contact_navigation_cannot_substitute_own_profile(self):
        value = {**self.root_report(), 'mode': 'open-contact'}
        value['measurement'].update(path=[2], ancestors=[{}], acquiredNodes=2, navigationAction='open-contact')
        self.assertEqual(module.parse_report(self.wire(value), 'open-contact'), value)
        value['measurement']['navigationAction'] = 'open-profile'
        with self.assertRaises(ValueError): module.parse_report(self.wire(value), 'open-contact')

    def test_send_requires_explicit_mutation_scope_and_all_acknowledgments(self):
        value = {**self.root_report(), 'mode': 'send-draft', 'mutationPerformed': True}
        value['measurement'].update(path=[4, 5], ancestors=[{}, {}], acquiredNodes=6,
            messageDispatchAcknowledged=True, recipientProfileChecked=True, draftMatched=True)
        self.assertEqual(module.parse_report(self.wire(value), 'send-draft'), value)
        for key in ('messageDispatchAcknowledged', 'recipientProfileChecked', 'draftMatched', 'activeFocusedWindow'):
            changed = {**value, 'measurement': {**value['measurement'], key: False}}
            with self.assertRaises(ValueError): module.parse_report(self.wire(changed), 'send-draft')
        with self.assertRaises(ValueError): module.parse_report(self.wire({**value, 'mutationPerformed': False}), 'send-draft')
        value.update(mode='structure', mutationPerformed=False)
        with self.assertRaises(ValueError): module.parse_report(self.wire(value), 'structure')

    def test_chat_read_requires_exact_recipient_scope_and_active_window(self):
        value = {**self.root_report(), 'mode': 'chat-node', 'surfaceAttestation': 'authorized-chat'}
        value['measurement'].update(path=[4, 1, 2], recipientProfileCheckedForRead=True,
            selectedText={'inspected': True, 'observedText': 'Test received'})
        self.assertEqual(module.parse_report(self.wire(value), 'chat-node'), value)
        for key, invalid in (('recipientProfileCheckedForRead', False), ('activeFocusedWindow', False), ('path', [4, 2])):
            changed = {**value, 'measurement': {**value['measurement'], key: invalid}}
            with self.assertRaises(ValueError): module.parse_report(self.wire(changed), 'chat-node')
        with self.assertRaises(ValueError): module.parse_report(self.wire({**value, 'surfaceAttestation': 'profile'}), 'chat-node')
        with self.assertRaises(ValueError): module.parse_report(self.wire({**value, 'mode': 'node'}), 'node')

    def test_ordinary_node_cannot_export_chat_text_or_description(self):
        for section, key in (('selectedText', 'observedText'), ('selectedDescription', 'observedDescription')):
            value = {**self.root_report(), 'mode': 'node'}
            value['measurement'][section] = {'inspected': True, key: 'Read'}
            with self.assertRaises(ValueError): module.parse_report(self.wire(value), 'node')

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
