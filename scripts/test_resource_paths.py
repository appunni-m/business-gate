#!/usr/bin/env python3
"""Contract validation for explicit framework and absent ancestor resources."""
import copy
import unittest
from compatibility import path


class ResourcePaths(unittest.TestCase):
    def sample(self):
        return {'children': [0, 0], 'resourceSuffix': 'button1', 'resourceNamespace': 'android',
                'className': 'android.widget.Button', 'expectedText': 'Block', 'ancestors': [
                    {'resourceSuffix': '', 'resourceNamespace': 'none', 'className': 'android.widget.FrameLayout', 'childCount': 1},
                    {'resourceSuffix': 'content', 'resourceNamespace': 'android', 'className': 'android.widget.FrameLayout', 'childCount': 1}]}

    def test_explicit_origins(self):
        path(self.sample())

    def test_legacy_origin_stays_selected(self):
        value = self.sample(); value['ancestors'] = []; value['children'] = []; del value['resourceNamespace']
        path(value)

    def test_absent_field_rejected(self):
        value = self.sample(); value['resourceNamespace'] = 'none'; value['resourceSuffix'] = ''
        with self.assertRaises(AssertionError): path(value)

    def test_unknown_and_ambiguous_origins(self):
        original = self.sample()
        for where in ('field', 'ancestor'):
            for namespace in ('other', '', None, True):
                value = copy.deepcopy(original)
                selected = value if where == 'field' else value['ancestors'][0]
                selected['resourceNamespace'] = namespace
                with self.assertRaises(AssertionError): path(value)

    def test_no_implicit_absence_or_wildcard(self):
        for namespace, suffix in (('none', 'named'), ('selected', ''), ('android', ''), ('android', '*')):
            value = self.sample(); value['ancestors'][0].update(resourceNamespace=namespace, resourceSuffix=suffix)
            with self.assertRaises(AssertionError): path(value)


if __name__ == '__main__':
    unittest.main()
