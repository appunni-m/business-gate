#!/usr/bin/env python3
"""Use a fictional policy token to verify encoded source and artifact scanning."""
import base64
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile
import check_brand


class EncodedBrandTests(unittest.TestCase):
    def setUp(self):
        check_brand.failures.clear()
        policy=patch.object(check_brand,'RESTRICTED',{hashlib.sha256(b'blockedx').hexdigest()})
        policy.start();self.addCleanup(policy.stop);self.addCleanup(check_brand.failures.clear)

    def payload(self,raw):
        return json.dumps({'encoding':'base64-utf8-json','payload':base64.b64encode(raw).decode('ascii')}).encode()

    def test_valid_rules(self):
        check_brand.check_payload('supplied-sender-rules.json',self.payload(b'["Harbor Clinic"]'))
        self.assertFalse(check_brand.failures)

    def test_decoded_and_unicode_escaped_names(self):
        for raw in (b'["blockedx sender"]',b'["\\u0062lockedx sender"]'):
            with self.subTest(raw=raw):
                check_brand.failures.clear()
                check_brand.check_payload('supplied-sender-rules.json',self.payload(raw))
                self.assertEqual(check_brand.failures,['supplied-sender-rules.json:decoded'])

    def test_malformed_payloads_fail_closed(self):
        for data in (b'{}',b'not JSON',b'x'*128_001,self.payload(b'{}'),self.payload(b'[1]'),b'{"encoding":"base64-utf8-json","payload":"!"}'):
            with self.subTest(size=len(data)):
                check_brand.failures.clear()
                check_brand.check_payload('supplied-sender-rules.json',data)
                self.assertTrue(check_brand.failures)

    def test_apk_entries_are_decoded(self):
        with tempfile.TemporaryDirectory() as folder:
            apk=Path(folder)/'fixture.apk'
            with zipfile.ZipFile(apk,'w') as archive:
                archive.writestr('assets/supplied-sender-rules.json',self.payload(b'["blockedx sender"]'))
            check_brand.scan(apk)
            self.assertTrue(any(label.endswith(':decoded') for label in check_brand.failures))


if __name__=='__main__':
    unittest.main()
