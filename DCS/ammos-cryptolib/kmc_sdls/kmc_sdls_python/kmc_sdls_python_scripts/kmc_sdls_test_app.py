#!/usr/bin/env python3

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

import argparse
import os
import binascii
#Import the KMC SDLS Client
from gov.nasa.jpl.ammos.kmc.sdlsclient import KmcSdlsClient

# Default 5 byte frame header
frame_header_hex = "202c040800"
# Default frame body
frame_body_hex = "0001bd37"
default_frame_hex = "{}{}".format(frame_header_hex, frame_body_hex)

class ArgumentException(Exception):
    "Raise when there is a command line argument error"
    pass

def build_options_parser():
    arg_parser=argparse.ArgumentParser(description='Simple KMC SDLS Python Test Application that will Apply and Process Security on a given frame')
    arg_parser.add_argument("-f", "--frame",
                            dest="frame",
                            help="Hex frame string representation of telecommand transfer-frame to apply & process SDLS layering on.\n Defaults to: '{}'".format(default_frame_hex))
    arg_parser.add_argument("-p", "--properties", 
                            dest="properties", 
                            help="The properties file that contains all the configuration needed by this application (supported properties defined in KMC SIS)", 
                            default=(os.path.dirname(os.path.realpath(__file__))+"/../etc/kmc_sdls_test_app.properties"), 
                            type=argparse.FileType('r'))
    arg_parser.add_argument("-P", "--processOnly", 
                            dest="process_only", 
                            help="Flag to only process security on the frame (default is to apply & process)", 
                            action='store_true')
    arg_parser.add_argument("-A", "--applyOnly", 
                            dest="apply_only", 
                            help="Flag to only apply security on the frame (default is to apply & process)", 
                            action='store_true')
    arg_parser.add_argument("-s", "--scid", 
                            dest="scid", 
                            type=scid_type, 
                            help="Override the default frame SC ID field")
    arg_parser.add_argument("-V", "--vcid", 
                            dest="vcid", 
                            type=vcid_type, 
                            help="Override the default frame VC ID field")
    return arg_parser

def scid_type(scid):
    msg = "SC ID must be a number between 0 and 1023 inclusive"
    try:
        int(scid) >= 0 and int(scid) <= 1023
    except:
        raise argparse.ArgumentTypeError(msg)
    return scid

def vcid_type(vcid):
    msg = "VC ID must be a number between 0 and 63 inclusive"    
    try:
        int(vcid) >= 0 and int(vcid) <= 63
    except:
        raise argparse.ArgumentTypeError(msg)
    return vcid

def main():
    # Default frame header (202c040800) fields in binary
    version               = "00"         #  2 bit version number
    bypass_flag           = "1"          #  1 bit bypass flag
    ctrl_cmd_flag         = "0"          #  1 bit control command flag
    spare                 = "00"         #  2 bit spare
    sc_id                 = "0000101100" # 10 bit spacecraft id (44)
    vc_id                 = "000001"     #  6 bit virtual channel id
    frame_length          = "0000001000" # 10 bit frame length
    frame_sequence_number = "00000000"   #  8 bit frame sequence number

    parser=build_options_parser()
    cli_args=parser.parse_args()

    # Can't have both custom frame and (SC_ID or VC_ID) overrides specified at the same time
    if cli_args.frame and (cli_args.scid or cli_args.vcid):
        raise ArgumentException("Can't have both Custom Frame override and (SC_ID or VC_ID) overrides specified at the same time.")

    # Override the default frame SC ID if specified
    if cli_args.scid:
        sc_id = '{0:010b}'.format(int(cli_args.scid))

    # Override the default frame VC ID if specified
    if cli_args.vcid:
        vc_id = '{0:06b}'.format(int(cli_args.vcid))

    # Use the frame override if passed in
    if cli_args.frame:
        frame_hex = cli_args.frame
    else:
        # If we have a SC_ID or VC_ID override, rebuild frame header otherwise use the default frame
        if cli_args.scid or cli_args.vcid:
            frame_header_bin = "{}{}{}{}{}{}{}{}".format(version, bypass_flag, ctrl_cmd_flag, spare, sc_id, vc_id, frame_length, frame_sequence_number)
            frame_header_hex = format(int(frame_header_bin, 2), 'x')
            frame_hex = "{}{}".format(frame_header_hex, frame_body_hex)
        else:
            frame_hex = default_frame_hex

    kmc_sdls_props = list()
    for line in cli_args.properties:
        if(not line.startswith('#') and line.rstrip() != ''):
            kmc_sdls_props.append(line.rstrip())

    # Initialize the KmcSdlsClient object with configuration
    k = KmcSdlsClient.KmcSdlsClient(kmc_sdls_props)

    # Print hex frame to be used:
    print("Using telecommand transfer frame: \n%s\n" % frame_hex)

    # Convert a hex-string representation of a JPL frame into a python bytearray
    tc = bytearray(binascii.unhexlify(frame_hex))

    if(not cli_args.process_only or (cli_args.process_only and cli_args.apply_only)):
        # Apply security to the telecommand transfer frame, store the result
        result = k.apply_security_tc(tc)
        print("SDLS TC Apply Security Result:\n%s\n"%result.hex())
    else:
        result = tc

    if(not cli_args.apply_only or (cli_args.process_only and cli_args.apply_only)):
        # Process the security headers on the result of the apply operation (or raw frame if processing only)
        reversed_tc = k.process_security_tc(result)

        print("SDLS TC Process Security Result:")
        #print(reversed_tc)
        print("SPI: ", reversed_tc.tc_security_header.spi)
        if(len(reversed_tc.tc_security_header.iv) != 0):
            print("IV: ", reversed_tc.tc_security_header.iv.hex())
        if(len(reversed_tc.tc_security_header.sn) != 0):
            print("SN: ", reversed_tc.tc_security_header.sn.hex())
        print("PDU: ", reversed_tc.tc_pdu.hex())
        print("MAC: ", reversed_tc.tc_security_trailer.mac.hex())
        print("FECF: ", hex(reversed_tc.tc_security_trailer.fecf))


if __name__ == "__main__":
    try:
        main()
    except ArgumentException as ae:
        print("Command Line Argument Error: ", ae)            
    except Exception as e:
        print("Encountered an unexpected error: ", e)
    finally:
        os._exit(1)



