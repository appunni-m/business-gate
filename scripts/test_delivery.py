#!/usr/bin/env python3
"""Offline publication failure/retry tests. No GitHub or signing credentials are used."""
import contextlib
import base64
import io
import json
import os
from pathlib import Path
import runpy
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
from compatibility import validate
import sign_apk

ROOT = Path(__file__).resolve().parent.parent


class Publication:
    def __init__(self, releases=(), changed_download=False, fail_upload=False, fail_at=(), paginated=False):
        self.releases = [dict(value) for value in releases]
        self.calls = []
        self.changed_download = changed_download
        self.fail_upload = fail_upload
        self.fail_at = fail_at
        self.paginated = paginated
        self.refs = {}
        self.assets = {release['tag_name']: {path.name: path.read_bytes() for path in Path('output/delivery').iterdir() if path.name in ('business-gate.apk', 'SHA256SUMS', 'build-info.json', 'verification.json')} for release in releases}

    def run(self, args, **kwargs):
        assert args[0] == 'gh', 'Unexpected subprocess: offline test must not run a command'
        command = args[1:]
        self.calls.append(command)
        if self.fail_at and tuple(command[:len(self.fail_at)]) == self.fail_at:
            self.fail_at = ()
            raise subprocess.CalledProcessError(1, ['gh', 'synthetic-boundary-failure'])
        output = ''
        if command[:2] == ['api', '--paginate']:
            output = json.dumps([self.releases[:1], self.releases[1:]] if self.paginated else [self.releases])
        elif command[:2] == ['release', 'download']:
            destination = Path(command[command.index('--dir') + 1])
            for name in self.assets[command[2]]:
                (destination / name).write_bytes(b'changed' if self.changed_download else self.assets[command[2]][name])
        elif command[:2] == ['release', 'create']:
            body = Path(command[command.index('--notes-file') + 1]).read_text() if '--notes-file' in command else ''
            self.releases.append({'tag_name': command[2], 'draft': True, 'body': body})
            self.assets[command[2]] = {Path(arg).name: Path(arg).read_bytes() for arg in command[3:] if Path(arg).is_file() and Path(arg).suffix != '.md'}
        elif command[:2] == ['release', 'edit']:
            release = next(value for value in self.releases if value['tag_name'] == command[2])
            release['draft'] = False
            if '--notes-file' in command:
                release['body'] = Path(command[command.index('--notes-file') + 1]).read_text()
        elif command[:2] == ['release', 'upload']:
            if command[2] == 'development' and self.fail_upload:
                self.fail_upload = False
                raise subprocess.CalledProcessError(1, ['gh', 'release', 'upload'])
            self.assets.setdefault(command[2], {}).update({Path(arg).name: Path(arg).read_bytes() for arg in command[3:] if Path(arg).is_file()})
        elif command[:3] == ['api', '--method', 'PATCH']:
            self.refs[command[3].split('/')[-1]] = next(arg[4:] for arg in command if arg.startswith('sha='))
        return subprocess.CompletedProcess(args, 0, stdout=output)

    def execute(self):
        with patch('subprocess.run', self.run), contextlib.redirect_stdout(io.StringIO()):
            try:
                runpy.run_path(str(ROOT / 'scripts/publish_release.py'), run_name='__main__')
            except SystemExit as error:
                if error.code not in (0, None):
                    raise


class DeliveryTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='business-gate-delivery-tests-')
        self.cwd = Path.cwd()
        os.chdir(self.temporary.name)
        self.environment = patch.dict(os.environ, {'GITHUB_REPOSITORY': 'appunni-m/business-gate'}, clear=True)
        self.environment.start()
        directory = Path('output/delivery')
        directory.mkdir(parents=True)
        info = {'releaseTag': 'build-201', 'versionCode': 201, 'versionName': 'synthetic-201',
                'commit': 'a' * 40, 'connectedAppActionsEnabled': False}
        (directory / 'build-info.json').write_text(json.dumps(info))
        (directory / 'business-gate.apk').write_bytes(b'opaque test artifact, not an APK')
        (directory / 'SHA256SUMS').write_text('opaque test checksum')
        (directory / 'RELEASE_NOTES.md').write_text('Business Gate test\n<!-- version-code: 201 -->\n')

    def tearDown(self):
        self.environment.stop()
        os.chdir(self.cwd)
        self.temporary.cleanup()

    def test_first_publication_uses_drafts(self):
        publisher = Publication()
        publisher.execute()
        creates = [call for call in publisher.calls if call[:2] == ['release', 'create']]
        self.assertEqual([call[2] for call in creates], ['build-201', 'development'])
        self.assertTrue(all('--draft' in call for call in creates))
        self.assertTrue(all(not release['draft'] for release in publisher.releases))

    def test_evidence_asset_is_published_immutably_and_identical_retry_passes(self):
        directory = Path('output/delivery')
        info = json.loads((directory / 'build-info.json').read_text())
        info['verificationSha256'] = 'c' * 64
        (directory / 'build-info.json').write_text(json.dumps(info))
        (directory / 'verification.json').write_text('synthetic retained evidence')
        publisher = Publication()
        publisher.execute()
        self.assertEqual(publisher.assets['build-201']['verification.json'], b'synthetic retained evidence')
        publisher.execute()
        (directory / 'verification.json').write_text('new evidence for old version')
        with self.assertRaises(SystemExit):
            publisher.execute()
        self.assertEqual(publisher.assets['build-201']['verification.json'], b'synthetic retained evidence')

    def test_published_version_is_not_replaced(self):
        publisher = Publication([{'tag_name': 'build-201', 'draft': False, 'body': ''}])
        publisher.execute()
        self.assertFalse(any(call[:3] == ['release', 'upload', 'build-201'] for call in publisher.calls))

    def test_changed_published_bytes_abort(self):
        publisher = Publication([{'tag_name': 'build-201', 'draft': False, 'body': ''}], changed_download=True)
        with self.assertRaises(SystemExit):
            publisher.execute()
        self.assertFalse(any(call[:2] == ['release', 'upload'] for call in publisher.calls))

    def test_newer_immutable_version_protects_stale_rolling_notes(self):
        publisher = Publication([{'tag_name': 'build-300', 'draft': False, 'body': ''},
                                 {'tag_name': 'development', 'draft': False, 'body': '<!-- version-code: 100 -->'}])
        publisher.execute()
        self.assertFalse(any(call[:3] == ['release', 'upload', 'development'] for call in publisher.calls))

    def test_interrupted_rolling_upload_is_resumable(self):
        publisher = Publication([{'tag_name': 'development', 'draft': False, 'body': '<!-- version-code: 100 -->'}], fail_upload=True)
        with self.assertRaises(subprocess.CalledProcessError):
            publisher.execute()
        self.assertTrue(any(value['tag_name'] == 'build-201' and not value['draft'] for value in publisher.releases))
        publisher.execute()
        self.assertEqual(sum(call[:3] == ['release', 'create', 'build-201'] for call in publisher.calls), 1)
        self.assertIn('201', next(value for value in publisher.releases if value['tag_name'] == 'development')['body'])

    def test_preexisting_draft_is_completed_with_all_assets(self):
        publisher = Publication([{'tag_name': 'build-201', 'draft': True, 'body': ''}])
        publisher.assets['build-201'] = {}
        publisher.execute()
        self.assertFalse(publisher.releases[0]['draft'])
        self.assertEqual(set(publisher.assets['build-201']), {'business-gate.apk', 'SHA256SUMS', 'build-info.json'})
        self.assertFalse(any(call[:3] == ['release', 'create', 'build-201'] for call in publisher.calls))

    def test_versioned_publish_failure_leaves_draft_and_no_rolling_release(self):
        publisher = Publication(fail_at=('release', 'edit', 'build-201'))
        with self.assertRaises(subprocess.CalledProcessError):
            publisher.execute()
        self.assertEqual([r['tag_name'] for r in publisher.releases], ['build-201'])
        self.assertTrue(publisher.releases[0]['draft'])
        self.assertEqual(len(publisher.assets['build-201']), 3)
        publisher.execute()
        self.assertTrue(all(not r['draft'] for r in publisher.releases))

    def test_tag_and_notes_failures_preserve_versioned_bytes_and_resume(self):
        for boundary in (('api', '--method', 'PATCH'), ('release', 'edit', 'development')):
            with self.subTest(boundary=boundary):
                publisher = Publication([{'tag_name': 'development', 'draft': False, 'body': '<!-- version-code: 100 -->'}], fail_at=boundary)
                with self.assertRaises(subprocess.CalledProcessError):
                    publisher.execute()
                versioned = dict(publisher.assets['build-201'])
                self.assertFalse(next(r for r in publisher.releases if r['tag_name'] == 'build-201')['draft'])
                self.assertIn('100', publisher.releases[0]['body'])
                publisher.execute()
                self.assertEqual(publisher.assets['build-201'], versioned)
                self.assertEqual(publisher.refs['development'], 'a' * 40)
                self.assertIn('201', publisher.releases[0]['body'])
                self.assertEqual(publisher.assets['development']['business-gate.apk'], versioned['business-gate.apk'])

    def test_missing_rolling_marker_cannot_replace_rolling_asset(self):
        publisher = Publication([{'tag_name': 'development', 'draft': False, 'body': 'no version marker'}])
        publisher.assets['development']['business-gate.apk'] = b'older public artifact'
        with self.assertRaises(SystemExit):
            publisher.execute()
        self.assertEqual(publisher.assets['development']['business-gate.apk'], b'older public artifact')
        self.assertIn('build-201', publisher.assets)

    def test_newer_version_on_later_page_prevents_older_channel_update(self):
        publisher = Publication([{'tag_name': 'development', 'draft': False, 'body': '<!-- version-code: 100 -->'},
                                 {'tag_name': 'build-301', 'draft': False, 'body': ''}], paginated=True)
        publisher.assets['development']['business-gate.apk'] = b'newer public artifact'
        publisher.execute()
        self.assertEqual(publisher.assets['development']['business-gate.apk'], b'newer public artifact')
        self.assertFalse(publisher.refs)

    def test_identical_rerun_does_not_recreate_published_version(self):
        publisher = Publication()
        publisher.execute()
        versioned = dict(publisher.assets['build-201'])
        publisher.execute()
        self.assertEqual(publisher.assets['build-201'], versioned)
        self.assertEqual(sum(call[:3] == ['release', 'create', 'build-201'] for call in publisher.calls), 1)

    def test_empty_registry_is_inactive(self):
        result = validate(lambda name: b'{"schemaVersion":1,"adapters":[]}')
        self.assertFalse(result['connectedAppActionsEnabled'])

    def test_malformed_or_unevidenced_registry_fails(self):
        for registry in ({'schemaVersion': 2, 'adapters': []}, {'schemaVersion': 1, 'adapters': [{'id': 'synthetic-unqualified'}]}):
            with self.assertRaises((AssertionError, KeyError, ValueError)):
                validate(lambda name: json.dumps(registry).encode())

    def test_missing_signing_input_never_invokes_tools(self):
        with patch.object(sys, 'argv', ['sign_apk.py', 'input.apk', 'output.apk']), patch('subprocess.run') as run:
            with self.assertRaises(SystemExit):
                sign_apk.main()
            run.assert_not_called()
        self.assertFalse(Path('output.apk').exists())

    def signing_failure(self, mismatch):
        # Deliberately invalid, synthetic key bytes. No real credential is read or generated.
        secret = {'keystore': base64.b64encode(b'invalid synthetic test key').decode(),
                  'keyAlias': 'test-only', 'storePassword': 'test-only', 'keyPassword': 'test-only'}
        Path('config').mkdir()
        Path('config/signing-certificate.sha256').write_text('a' * 64)
        paths = []

        def run(args, **kwargs):
            if args[1] == 'sign':
                key = Path(args[args.index('--ks') + 1])
                paths.append(key)
                self.assertEqual(key.stat().st_mode & 0o777, 0o600)
                self.assertNotIn('ANDROID_SIGNING', kwargs['env'])
                Path('output.apk').write_bytes(b'partial test artifact')
                if not mismatch:
                    raise subprocess.CalledProcessError(1, ['test-only-signing-failure'])
            return subprocess.CompletedProcess(args, 0)

        with patch.dict(os.environ, {'ANDROID_SIGNING': json.dumps(secret), 'ANDROID_HOME': '/synthetic-sdk'}), \
                patch.object(sys, 'argv', ['sign_apk.py', 'input.apk', 'output.apk']), \
                patch('subprocess.run', run), patch.object(sign_apk, 'signing_fingerprint', return_value='b' * 64):
            with self.assertRaises(SystemExit if mismatch else subprocess.CalledProcessError):
                sign_apk.main()
        self.assertTrue(paths)
        self.assertTrue(all(not key.exists() for key in paths))
        self.assertFalse(Path('output.apk').exists())

    def test_mismatched_certificate_removes_artifact_and_temporary_key(self):
        self.signing_failure(mismatch=True)

    def test_signing_failure_removes_temporary_key(self):
        self.signing_failure(mismatch=False)

    def test_instrumentation_requires_matching_completed_mode(self):
        for report in ('', 'PASS 1 test; mode=wrong\n', 'PASS 1 test; mode=all\nFAIL crash\n', 'x'*64_001):
            result = subprocess.run([sys.executable,str(ROOT/'scripts/assert_instrumentation.py'),'all'],input=report,text=True,capture_output=True)
            self.assertNotEqual(result.returncode,0)
        result = subprocess.run([sys.executable,str(ROOT/'scripts/assert_instrumentation.py'),'all'],input='PASS 1 owned assertion; mode=all\n',text=True,capture_output=True)
        self.assertEqual(result.returncode,0)

    def test_instrumentation_failure_annotation_is_escaped(self):
        environment = dict(os.environ,GITHUB_ACTIONS='true')
        result = subprocess.run([sys.executable,str(ROOT/'scripts/assert_instrumentation.py'),'all'],input='FAIL synthetic 10%\n',env=environment,text=True,capture_output=True)
        self.assertEqual(result.returncode,1)
        self.assertIn('::error title=Android all::FAIL synthetic 10%25',result.stdout)

    def test_unterminated_report_keeps_annotations_on_their_own_line(self):
        for report,kind,code in (('PASS 1 owned assertion; mode=all','notice',0),('FAIL synthetic assertion','error',1)):
            with self.subTest(kind=kind):
                result = subprocess.run([sys.executable,str(ROOT/'scripts/assert_instrumentation.py'),'all'],
                                        input=report,env=dict(os.environ,GITHUB_ACTIONS='true'),text=True,capture_output=True)
                self.assertEqual(result.returncode,code)
                self.assertIn('\n::'+kind+' title=Android all::',result.stdout)

    def test_failed_performance_exposes_bounded_numeric_metrics(self):
        environment = dict(os.environ,GITHUB_ACTIONS='true')
        report = 'METRIC records=10000 query_p95_ms=101\nMETRIC private=not-numeric\nFAIL query target\n'
        result = subprocess.run([sys.executable,str(ROOT/'scripts/assert_instrumentation.py'),'performance'],input=report,env=environment,text=True,capture_output=True)
        self.assertEqual(result.returncode,1)
        self.assertIn('::notice title=Android performance metrics::METRIC records=10000 query_p95_ms=101',result.stdout)
        self.assertNotIn('::notice title=Android performance metrics::METRIC private=',result.stdout)


if __name__ == '__main__':
    unittest.main()
