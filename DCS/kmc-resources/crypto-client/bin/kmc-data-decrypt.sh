#!/bin/bash

# kmc-data-decrypt.sh, based on the metadata, decrypts a ciphertext file to create a cleartext file.
# Exit status: 
#   0: decryption is successful, result is in the cleartext file.
#   1: error in user input or from KMC Crypto Service
#
# The script uses the access control properties in kmc-crypto.cfg
# to access the protected KMC Crypto Service.

source `dirname $0`/kmc-cli-common.sh

function usage {
  echo "Usage: kmc-data-decrypt.sh [-v]"
  echo "       -input=CIPHERTEXT_FILE -output=CLEARTEXT_FILE -metadata=METADATA_FILE"
  echo "       [-crypto_service_uri=URI]"
}

if [ "$1" == "-v" ]; then
  VERBOSE="-v"
  shift
fi
if [ -z "$1" ] || [ "$1" == "help" ] || [ "$1" == "?" ]; then
  usage
  exit 0
fi

function process_command_line_arguments {
  for i in "$@"
  do
  case $i in
    -input=*)
      INPUT="${i#*=}"
      ;;
    -output=*)
      OUTPUT="${i#*=}"
      ;;
    -metadata=*)
      METADATA="${i#*=}"
      ;;
    -crypto_service_uri=*)
      CRYPTO_SERVICE_URI="${i#*=}"
      ;;
    *)
      echo "ERROR: Invalid argument ${i}"
      usage
      exit 1
      ;;
   esac
   done
}

process_command_line_arguments "$@"
get_access_control
if [ $? != 0 ]; then
  echo "Error in getting access control value.  Please check kmc-crypto for error."
  exit 1
fi

INPUT=$(verify_readable_file "input" "$INPUT")
if [ $? != 0 ]; then
  echo $INPUT
  exit 1
fi

OUTPUT=$(verify_writable_file "output" "$OUTPUT")
if [ $? != 0 ]; then
  echo $OUTPUT
  exit 1
fi

METADATA=$(verify_readable_file "metadata" "$METADATA")
if [ $? != 0 ]; then
  echo $METADATA
  exit 1
fi

if [ "$INPUT" == "$OUTPUT" ]; then
  echo "ERROR: input and output cannot be the same $INPUT"
  exit 1
elif [ "$INPUT" == "$METADATA" ]; then
  echo "ERROR: input and metadata cannot be the same $INPUT"
  exit 1
elif [ "$OUTPUT" == "$METADATA" ]; then
  echo "ERROR: output and metadata cannot be the same $OUTPUT"
  exit 1
fi

AMMOS_CA_BUNDLE=${AMMOS_CA_BUNDLE:-/etc/pki/tls/certs/ammos-ca-bundle.crt}
if [ -r $AMMOS_CA_BUNDLE ]; then
  CA="--cacert $AMMOS_CA_BUNDLE"
else
  echo "INFO: $AMMOS_CA_BUNDLE is not found, use -k without server validation."
  CA="-k"
fi

if [ -z "$CRYPTO_SERVICE_URI" ]; then
  CRYPTO_SERVICE_URI=$(get_crypto_service_uri)
fi
if [ $? != 0 ]; then
  echo $CRYPTO_SERVICE_URI
  exit 1
fi

DECRYPT=$CRYPTO_SERVICE_URI/decrypt
PARAMS="metadata=$(cat $METADATA)"
POST="curl --tlsv1.2 $CA $ACCESS_CONTROL -w |%{http_code} -sS -X POST"

if [ $VERBOSE ]; then
  echo
  echo "curl --tlsv1.2 $CA \\"
  echo -e "   $ACCESS_CONTROL -w '|%{http_code}' -sS -X POST \\"
  echo -e "   -H 'Content-Type: application/octet-stream' \\"
  echo -e "   --data-binary @$INPUT \\"
  echo -e "   $DECRYPT?$PARAMS"
  echo
fi

response=$($POST -H "Content-Type: application/octet-stream" --data-binary "@$INPUT" "$DECRYPT?$PARAMS")
if [ $? != 0 ]; then
  echo "ERROR: execution of the curl command failed.  Run with -v to check the curl command."  
  exit 1
fi

if [ "$VERBOSE" ]; then
  echo response = $response
  echo
fi

http_code=`echo $response | tail -c 4`
if [ "$http_code" == "302" ]; then
  echo "ERROR: The request requires a valid SSO Token."
  exit 1
elif [ "$http_code" == "403" ]; then
  echo "ERROR: The operation is not authorized."
  exit 1
elif [ "$http_code" == "404" ]; then
  echo -e "ERROR: requested KMC Crypto Service operation invalid. Run with -v to see the details."
  exit 1
fi

response=$(echo $response | cut -f1 -d"|")
if [ -z "$response" ]; then
  echo "ERROR: KMC Crypto Service returned an empty response. Run with -v to see the details"
  exit 1
elif [[ "$response" == *Error* ]]; then
  echo "ERROR: data decrypt response: $response"
  exit 1
else
  httpCode=$(echo $response | tr '{' '\n' | tr ',' '\n' | grep httpCode | cut -d: -f 2 | tr -d '"')
  if [ "$httpCode" == "200" ]; then
    rm -f $OUTPUT
    base64cleartext=$(echo $response | tr ',' '\n' | grep base64cleartext | cut -d: -f 2 | tr -d '"' | tr -d '}')
    echo $base64cleartext | base64 --decode > $OUTPUT
    if [ "$VERBOSE" ]; then
      echo "Data decryption completed successfully. Cleartext is written to file $OUTPUT."
    fi
  else
    echo "ERROR: data decrypt response: $response"
    exit 1
  fi
fi
