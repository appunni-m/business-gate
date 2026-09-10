#!/usr/bin/env python3
"""Public transport and integrity failure tests; no credentials or network."""
import copy
import hashlib
import json
import unittest
from unittest.mock import Mock

import verify_public_release as public


class PublicReleaseTests(unittest.TestCase):
    def setUp(self):
        self.repository = 'appunni-m/business-gate'
        self.pin = 'b' * 64
        self.files = {}
        self.info, self.apk = self.version(201, 'a' * 40)
        self.rolling(self.info, self.apk)
        self.inspect = Mock()

    def version(self, code, commit):
        apk = ('synthetic owned APK ' + str(code)).encode()
        info = {'applicationId': public.PACKAGE, 'releaseTag': f'build-{code}', 'versionCode': code,
                'versionName': f'0.1.0-dev.{code}.1', 'minimumAndroidApi': 29, 'targetAndroidApi': 36,
                'sha256': hashlib.sha256(apk).hexdigest(), 'signingCertificateSha256': self.pin,
                'commit': commit, 'workflowRun': f'https://github.com/{self.repository}/actions/runs/1',
                'connectedAppActionsEnabled': False, 'qualifiedAdapters': []}
        base = f'https://github.com/{self.repository}/releases/download/build-{code}/'
        values = {'business-gate.apk': apk, 'build-info.json': json.dumps(info).encode()}
        for name, value in values.items():
            self.files[base + name] = value
        self.files[base + 'SHA256SUMS'] = ''.join(f'{hashlib.sha256(value).hexdigest()}  {name}\n' for name, value in values.items()).encode()
        self.files[f'https://api.github.com/repos/{self.repository}/git/ref/tags/build-{code}'] = json.dumps({'object': {'type': 'commit', 'sha': commit}}).encode()
        return info, apk

    def rolling(self, info, apk):
        self.files[f'https://api.github.com/repos/{self.repository}/releases/tags/development'] = json.dumps({'body': f'<!-- version-code: {info["versionCode"]} -->\n{info["sha256"]}'}).encode()
        self.files[f'https://api.github.com/repos/{self.repository}/git/ref/tags/development'] = json.dumps({'object': {'type': 'commit', 'sha': info['commit']}}).encode()
        self.files[f'https://github.com/{self.repository}/releases/download/development/business-gate.apk'] = apk

    def run_verification(self):
        return public.verify(self.repository, self.info, self.pin, self.files.__getitem__, self.inspect)

    def bind_manifest(self, evidence):
        base = f'https://github.com/{self.repository}/releases/download/build-201/'
        raw = json.dumps(evidence).encode()
        self.info['verificationSha256'] = hashlib.sha256(raw).hexdigest()
        self.files[base + 'verification.json'] = raw
        self.files[base + 'build-info.json'] = json.dumps(self.info).encode()
        self.files[base + 'SHA256SUMS'] = ''.join(
            f'{hashlib.sha256(self.files[base + name]).hexdigest()}  {name}\n'
            for name in ('business-gate.apk', 'build-info.json', 'verification.json')).encode()

    def test_manifest_is_hash_bound_to_exact_source_and_apk(self):
        evidence = {'complete': True, 'environment': {'sourceCommit': self.info['commit']},
                    'signedUpgrade': {'currentApkSha256': self.info['sha256']}}
        self.bind_manifest(evidence)
        self.assertTrue(self.run_verification()['anonymousDownloadVerified'])
        self.files[f'https://github.com/{self.repository}/releases/download/build-201/verification.json'] += b' '
        with self.assertRaises(ValueError):
            self.run_verification()

    def test_rehashed_wrong_or_incomplete_manifest_fails(self):
        base = {'complete': True, 'environment': {'sourceCommit': self.info['commit']},
                'signedUpgrade': {'currentApkSha256': self.info['sha256']}}
        for value in (False, 'true', 1, None):
            self.bind_manifest(dict(base, complete=value))
            with self.assertRaises(ValueError):
                self.run_verification()
        for bad in (dict(base, environment={'sourceCommit': 'e' * 40}),
                    dict(base, signedUpgrade={'currentApkSha256': 'e' * 64})):
            self.bind_manifest(bad)
            with self.assertRaises(ValueError):
                self.run_verification()

    def test_actual_certificate_gate_rejects_before_badging(self):
        from unittest.mock import patch
        with patch.dict('os.environ', {'ANDROID_HOME': '/synthetic-sdk'}), \
                patch.object(public, 'signing_fingerprint', return_value='c' * 64), \
                patch.object(public.subprocess, 'check_output') as inspect:
            with self.assertRaisesRegex(ValueError, 'certificate'):
                public.inspect_apk(b'synthetic bytes', self.info, self.pin)
            inspect.assert_not_called()

    def test_exact_public_bytes_and_source_pass(self):
        result = self.run_verification()
        self.assertEqual(result['versionCode'], 201)
        self.assertTrue(result['anonymousDownloadVerified'])
        self.inspect.assert_called_once_with(self.apk, self.info, self.pin)

    def test_each_missing_asset_fails(self):
        for url in list(self.files):
            with self.subTest(url=url):
                value = self.files.pop(url)
                with self.assertRaises(KeyError):
                    self.run_verification()
                self.files[url] = value

    def test_changed_apk_or_metadata_bytes_fail_before_signature(self):
        for name in ('business-gate.apk', 'build-info.json'):
            url = f'https://github.com/{self.repository}/releases/download/build-201/{name}'
            value = self.files[url]
            self.files[url] = value + b' changed'
            with self.assertRaises(ValueError):
                self.run_verification()
            self.inspect.assert_not_called()
            self.files[url] = value

    def test_checksum_inventory_is_exact(self):
        url = f'https://github.com/{self.repository}/releases/download/build-201/SHA256SUMS'
        value = self.files[url]
        for bad in (b'', value + value.splitlines()[0], value.replace(b'build-info.json', b'../other'), value.replace(b'  ', b' ')):
            self.files[url] = bad
            with self.assertRaises(ValueError):
                self.run_verification()

    def test_rehashed_metadata_cannot_replace_verified_build(self):
        base = f'https://github.com/{self.repository}/releases/download/build-201/'
        changed = dict(self.info, commit='c' * 40)
        raw = json.dumps(changed).encode()
        self.files[base + 'build-info.json'] = raw
        self.files[base + 'SHA256SUMS'] = f'{self.info["sha256"]}  business-gate.apk\n{hashlib.sha256(raw).hexdigest()}  build-info.json\n'.encode()
        with self.assertRaises(ValueError):
            self.run_verification()

    def test_wrong_signature_and_actual_apk_inspection_failure_stop_verification(self):
        self.inspect.side_effect = ValueError('Wrong publisher')
        with self.assertRaises(ValueError):
            self.run_verification()

    def test_stale_or_ambiguous_notes_fail(self):
        url = f'https://api.github.com/repos/{self.repository}/releases/tags/development'
        for body in ('', '<!-- version-code: 200 -->', '<!-- version-code: 201 -->', '<!-- version-code: 201 --><!-- version-code: 201 -->'):
            self.files[url] = json.dumps({'body': body}).encode()
            with self.assertRaises(ValueError):
                self.run_verification()

    def test_both_tag_refs_must_match(self):
        for tag in ('build-201', 'development'):
            url = f'https://api.github.com/repos/{self.repository}/git/ref/tags/{tag}'
            value = self.files[url]
            self.files[url] = json.dumps({'object': {'type': 'commit', 'sha': 'd' * 40}}).encode()
            with self.assertRaises(ValueError):
                self.run_verification()
            self.files[url] = value

    def test_rolling_bytes_must_equal_versioned_bytes(self):
        url = f'https://github.com/{self.repository}/releases/download/development/business-gate.apk'
        self.files[url] = b'changed'
        with self.assertRaises(ValueError):
            self.run_verification()

    def test_newer_channel_is_independently_verified_and_never_downgraded(self):
        newer, apk = self.version(301, 'e' * 40)
        self.rolling(newer, apk)
        before = copy.deepcopy(self.files)
        result = self.run_verification()
        self.assertEqual(result['versionCode'], 201)
        self.assertEqual(result['rollingVersionCode'], 301)
        self.assertEqual(self.files, before)
        self.assertEqual(self.inspect.call_count, 2)

    def test_visibility_delay_is_bounded_and_recovers(self):
        operation = Mock(side_effect=[ValueError('not propagated'), KeyError('missing'), {'verified': True}])
        sleep = Mock()
        self.assertEqual(public.verify_with_retries(operation, sleep=sleep), {'verified': True})
        self.assertEqual(operation.call_count, 3)
        self.assertEqual([call.args[0] for call in sleep.call_args_list], [3, 6])

    def test_persistent_failure_never_becomes_a_pass(self):
        operation = Mock(side_effect=ValueError('bad bytes'))
        sleep = Mock()
        with self.assertRaises(ValueError):
            public.verify_with_retries(operation, sleep=sleep)
        self.assertEqual(operation.call_count, 4)
        self.assertEqual(sleep.call_count, 3)


if __name__ == '__main__':
    unittest.main()
