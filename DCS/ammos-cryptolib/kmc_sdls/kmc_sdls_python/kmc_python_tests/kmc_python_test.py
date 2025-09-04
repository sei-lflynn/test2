#Copyright 2021, by the California Institute of Technology.
#ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
#Any commercial use must be negotiated with the Office of Technology
#Transfer at the California Institute of Technology.
#
#This software may be subject to U.S. export control laws. By accepting
#this software, the user agrees to comply with all applicable U.S.
#export laws and regulations. User has the responsibility to obtain
#export licenses, or other export authority as may be required before
#exporting such information to foreign countries or providing access to
#foreign persons.

import unittest
import binascii
from gov.nasa.jpl.ammos.kmc.sdlsclient import KmcSdlsClient

cryptolib_inmemory_default_config = ['cryptolib.sadb.type=inmemory','cryptolib.crypto.type=libgcrypt','cryptolib.process_tc.ignore_antireplay=true',
                               'cryptolib.process_tc.ignore_sa_state=true','cryptolib.process_tc.process_pdus=false',
                               'cryptolib.tc.vcid_bitmask=0x3F','cryptolib.tc.3.0.0.has_segmentation_header=true',
                               'cryptolib.tc.3.0.0.has_pus_header=true','cryptolib.tc.3.0.0.has_ecf=true',
                               'cryptolib.tc.3.0.0.max_frame_length=1024', 'cryptolib.tc.3.1.0.has_segmentation_header=true',
                               'cryptolib.tc.3.1.0.has_pus_header=true','cryptolib.tc.3.1.0.has_ecf=true',
                               'cryptolib.tc.3.1.0.max_frame_length=1024']
kmc_mmt_inmemory_default_config =  ['cryptolib.sadb.type=inmemory','cryptolib.crypto.type=libgcrypt','cryptolib.process_tc.ignore_antireplay=true',
                               'cryptolib.process_tc.ignore_sa_state=true','cryptolib.process_tc.process_pdus=false',
                               'cryptolib.tc.vcid_bitmask=0x07','cryptolib.tc.44.1.0.has_segmentation_header=false',
                               'cryptolib.tc.44.1.0.has_pus_header=false','cryptolib.tc.44.1.0.has_ecf=true',
                               'cryptolib.tc.44.1.0.max_frame_length=1024','cryptolib.tc.44.0.0.has_segmentation_header=false',
                                    'cryptolib.tc.44.0.0.has_pus_header=false','cryptolib.tc.44.0.0.has_ecf=true',
                                    'cryptolib.tc.44.0.0.max_frame_length=1024']

kmc_mmt_inmemory_default_config_rev =  ['cryptolib.sadb.type=inmemory','cryptolib.crypto.type=libgcrypt','cryptolib.process_tc.ignore_antireplay=true',
                                    'cryptolib.process_tc.ignore_sa_state=true','cryptolib.process_tc.process_pdus=false',
                                    'cryptolib.tc.vcid_bitmask=0x07','cryptolib.tc.44.0.0.has_segmentation_header=false',
                                    'cryptolib.tc.44.0.0.has_pus_header=false','cryptolib.tc.44.0.0.has_ecf=true',
                                    'cryptolib.tc.44.0.0.max_frame_length=1024','cryptolib.tc.44.1.0.has_segmentation_header=false',
                                    'cryptolib.tc.44.1.0.has_pus_header=false','cryptolib.tc.44.1.0.has_ecf=true',
                                    'cryptolib.tc.44.1.0.max_frame_length=1024']


cryptolib_inmemory_invalid_config = ['cryptolib.sadb.type=inmemory','cryptolib.crypto.type=libgcrypt','cryptolib.process_tc.ignore_antireplay=true',
                                     'cryptolib.process_tc.ignore_sa_state=true','cryptolib.process_tc.process_pdus=false',
                                     'cryptolib.tc.vcid_bitmask=0x3F','cryptolib.tc.3.0.0.has_segmentation_header=true',
                                     'cryptolib.tc.3.0.0.has_pus_header=true',
                                     'cryptolib.tc.3.0.0.max_frame_length=1024']

class TestConfigMethods(unittest.TestCase):

    def test_config_prop_init_cryptolib_defaults(self):
        k = KmcSdlsClient.KmcSdlsClient(cryptolib_inmemory_default_config)
        #self.assertEqual('foo'.upper(),'FOO')
    def test_config_prop_init_bad_properties(self):
        with self.assertRaises(KmcSdlsClient.SdlsClientException):
            k = KmcSdlsClient.KmcSdlsClient(cryptolib_inmemory_invalid_config)
    '''
    def test_simple_apply_security(self):
        k = KmcSdlsClient.KmcSdlsClient(cryptolib_inmemory_default_config)
        #k = KmcSdlsClient.KmcSdlsClient(kmc_mmt_inmemory_default_config_rev)
        #tc = bytearray(binascii.unhexlify("2003001c00ff000100001880d03e000a197f0b000300020093d4ba21c4555555555555"))
        tc = bytearray(binascii.unhexlify("200304080000014D3C")) #With FECF
        apply_result = k.apply_security_tc(tc)
        process_result = k.process_security_tc(apply_result)
        print(process_result)
        #self.assertEqual("0001",process_result.tc_pdu.hex())
    '''
if __name__ == '__main__':
    unittest.main()