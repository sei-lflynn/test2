#!/bin/bash

# kmc-data-encrypt.sh encrypts a plaintext file to create a ciphertext file.
# The metadata file is used for decrypting the ciphertext file.
# Exit status:
#   0: encription is successful, result in the metadata and ciphertext files.
#   1: error in user input or from KMC Crypto Service
#
# The script uses the access control properties in kmc-crypto.cfg
# to access the protected KMC Crypto Service.

source `dirname $0`/kmc-cli-common.sh

function usage {
  echo "Usage: kmc-data-encrypt.sh [-v] -keyRef=KEYREF"
  echo "       -input=PLAINTEXT_FILE -output=CIPHERTEXT_FILE -metadata=METADATA_FILE"
  echo "       [-transform=CIPHER_TRANSFORMATION] [-encryptOffset=INTEGER] [-iv=INITIAL_VECTOR]"
  echo "       [-macLength=INTEGER]"
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
    -keyRef=*)
      KEYREF="${i#*=}"
      ;;
    -input=*)
      INPUT="${i#*=}"
      ;;
    -output=*)
      OUTPUT="${i#*=}"
      ;;
    -metadata=*)
      METADATA="${i#*=}"
      ;;
    -transform=*)
      TRANSFORM="${i#*=}"
      ;;
    -iv=*)
      IV="${i#*=}"
      ;;
    -encryptOffset=*)
      ENCRYPT_OFFSET="${i#*=}"
      ;;
    -macLength=*)
      MAC_LENGTH="${i#*=}"
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
if [ -z "$KEYREF" ]; then
  echo "ERROR: missing keyRef argument"
  echo
  usage
  exit 1
fi

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

METADATA=$(verify_writable_file "metadata" "$METADATA")
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

ENCRYPT=$CRYPTO_SERVICE_URI/encrypt
PARAMS="keyRef=$KEYREF"
if [ "$TRANSFORM" ]; then
  PARAMS="$PARAMS&transformation=$TRANSFORM"
fi
if [ "$IV" ]; then
  PARAMS="$PARAMS&iv=$IV"
fi
if [ "$ENCRYPT_OFFSET" ]; then
  PARAMS="$PARAMS&encryptOffset=$ENCRYPT_OFFSET"
fi
if [ "$MAC_LENGTH" ]; then
  PARAMS="$PARAMS&macLength=$MAC_LENGTH"
fi
POST="curl --tlsv1.2 $CA $ACCESS_CONTROL -w |%{http_code} -sS -X POST"

if [ $VERBOSE ]; then
  echo
  echo "curl --tlsv1.2 $CA \\"
  echo -e "   $ACCESS_CONTROL -w '|%{http_code}' -sS -X POST \\"
  echo -e "   -H 'Content-Type: application/octet-stream' \\"
  echo -e "   --data-binary @$INPUT \\"
  echo -e "   $ENCRYPT?$PARAMS"
  echo
fi

response=$($POST -H "Content-Type: application/octet-stream" --data-binary "@$INPUT" "$ENCRYPT?$PARAMS")
if [ $? != 0 ]; then
  echo "ERROR: execution of the curl command failed. Run with -v to check the curl command."  
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
  echo -e "ERROR: requested KMC Crypto Service operation invalid. Run with -v to see the details"
  exit 1
fi

response=$(echo $response | cut -f1 -d"|")
if [ -z "$response" ]; then
  echo "ERROR: KMC Crypto Service returned empty response. Run with -v to see the details."
  exit 1
elif [[ "$response" == *Error* ]]; then
  echo "ERROR: data encrypt response: $response"
  exit 1
else
  httpCode=$(echo $response | tr '{' '\n' | tr ',' '\n' | grep httpCode | cut -d: -f 2 | tr -d '"')
  if [ "$httpCode" == "200" ]; then
    metadata=$(echo $response | sed 's/,"/\n/g' | grep metadata | sed 's/metadata":"//' | tr -d '"')
    ciphertext=$(echo $response | tr ',' '\n' | grep base64ciphertext | cut -d: -f 2 | tr -d '"' | tr -d '}')
    echo -n $metadata > $METADATA
    echo -n $ciphertext > $OUTPUT
    if [ "$VERBOSE" ]; then
      echo "Data encryption completed successfully."
      echo "Ciphertext written to file $OUTPUT, metadata written to file $METADATA."
    fi
  else
    echo "ERROR: data encrypt response: $response"
    exit 1
  fi
fi
