#!/usr/bin/env python3
"""Experimental evidence must not imply physical qualification or broad device support."""
import copy
import hashlib
import json
import unittest
from pathlib import Path
from compatibility import canonical, validate

ROOT=Path(__file__).resolve().parents[1]/'app/src/main/assets'

class MeasuredRegistryTests(unittest.TestCase):
    def setUp(self):
        self.registry=json.loads((ROOT/'adapters/compatibility.json').read_text())
        self.evidence=json.loads((ROOT/'adapters/evidence/business-profile-v1.json').read_text())
    def run_registry(self,registry,evidence=None):
        evidence=copy.deepcopy(self.evidence if evidence is None else evidence)
        if registry.get('measuredRoutes'):
            row=registry['measuredRoutes'][0]
            evidence['contractSha256']=hashlib.sha256(canonical({k:v for k,v in row.items() if k!='evidenceSha256'})).hexdigest()
            row['evidenceSha256']=hashlib.sha256(canonical(evidence)).hexdigest()
        def read(path):
            if path=='adapters/compatibility.json':return canonical(registry)
            if path=='adapters/evidence/business-profile-v1.json':return canonical(evidence)
            return (ROOT/path).read_bytes()
        return validate(read)
    def test_empty_registry_disables_every_action(self):
        result=self.run_registry({'schemaVersion':1,'adapters':[],'measuredRoutes':[]})
        self.assertFalse(result['connectedAppActionsEnabled'])
    def test_measured_result_has_no_physical_qualification(self):
        result=self.run_registry(self.registry)
        self.assertTrue(result['connectedAppActionsEnabled'])
        self.assertEqual(result['qualifiedAdapters'],[])
        self.assertEqual(result['experimentalAdapters'],['business-profile-v1'])
        self.assertEqual(result['supportLevel'],'emulator-experimental')
    def test_expanded_authority_rejected_even_with_matching_hashes(self):
        for field,value in [('supportLevel','production'),('physicalQualification',True),('foregroundSessionRequired',False),('api',35),('api','36'),('versionCode',0),('id','unmeasured')]:
            with self.subTest(field=field):
                registry=copy.deepcopy(self.registry);registry['measuredRoutes'][0][field]=value
                with self.assertRaises((AssertionError,KeyError,ValueError)):self.run_registry(registry)
    def test_unmeasured_environment_rejected(self):
        for field,value in [('model','different'),('manufacturer','different'),('densityDpi',440),('orientation',2),('fontScalePercent',120),('deviceLocale','en-GB')]:
            with self.subTest(field=field):
                registry=copy.deepcopy(self.registry);registry['measuredRoutes'][0]['environment'][field]=value
                with self.assertRaises(AssertionError):self.run_registry(registry)
    def test_failed_or_fabricated_evidence_rejected(self):
        for field in ['physicalQualification','receiverIdentitySyntaxVerified','block','unblock']:
            evidence=copy.deepcopy(self.evidence)
            if field=='block':evidence[field]['blockedStateVerified']=False
            elif field=='unblock':evidence[field]['unblockedStateVerified']=False
            else:evidence[field]=field=='physicalQualification'
            with self.subTest(field=field),self.assertRaises(AssertionError):self.run_registry(copy.deepcopy(self.registry),evidence)
    def test_duplicate_route_rejected(self):
        self.registry['measuredRoutes']*=2
        with self.assertRaises(AssertionError):self.run_registry(self.registry)
    def test_raw_evidence_change_rejected(self):
        def read(path):
            content=(ROOT/path).read_bytes()
            return content+b' ' if path.endswith('business-profile-v1.json') else content
        with self.assertRaises(AssertionError):validate(read)

if __name__=='__main__':unittest.main()
