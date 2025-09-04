# kmc-cli-common.sh defines common constants and functions used by
# the KMC Crypto CLI scripts.

KMC_CFG=__CFGPATH__/kmc-crypto.cfg

if [ ! -r $KMC_CFG ]; then
  KMC_CFG=/opt/ammos/kmc/etc/kmc-crypto.cfg
fi

# verify_readable_file $name $value, returns $value or error message
function verify_readable_file
{
  name="$1"
  value="$2"
  if [ -z "$value" ]; then
    echo "ERROR: missing $name value"
    return 1
  elif [[ "$value" == "~/"* ]]; then
    value="${value/#~/$HOME}"
  elif [[ "$value" == "~"* ]]; then
    echo "ERROR: unacceptable $name file $value"
    return 1
  fi

  if [ ! -f $value ]; then
    echo "ERROR: The $name file is not found - $value"
    return 1
  elif [ ! -r $value ]; then
    echo "ERROR: The $name file is not readable - $value"
    return 1
  else
    echo "$value"
    return 0
  fi
}

# verify_writable_file $name $value, returns $value or error message
function verify_writable_file
{
  name="$1"
  value="$2"
  if [ -z "$value" ]; then
    echo "ERROR: missing $name value"
    return 1
  elif [[ "$value" == "~/"* ]]; then
    value="${value/#~/$HOME}"
  elif [[ "$value" == "~"* ]]; then
    echo "ERROR: unacceptable $name file $value"
    return 1
  fi
  if [ -f "$value" ] && [ ! -w "$value" ]; then
    echo "ERROR: The $name file not writable - $value"
    return 1
  elif [ ! -f "$value" ] && [ ! -w `dirname $value` ]; then
    echo "ERROR: The $name directory not writable - $value"
    return 1
  else
    echo $value
    return 0
  fi
}

# Extract the crypto_service_uri to $CRYPTO_SERVICE_URI from $KMC_CFG (kmc-crypto.cfg)
function get_crypto_service_uri
{
  uri=`grep crypto_service_uri $KMC_CFG | grep -v "#"`
  if [ -z "$uri" ]; then
    echo "ERROR: crypto_service_uri not found in $KMC_CFG"
    return 1
  fi
  CRYPTO_SERVICE_URI=`echo $uri | cut -d "=" -f 2`
  # check if it's a good URI
  CRYPTO_SERVICE_URI=`echo $CRYPTO_SERVICE_URI` # trim the space
  space=`echo $CRYPTO_SERVICE_URI | cut -d " " -f 1`
  if [ "$space" != "$CRYPTO_SERVICE_URI" ]; then
    echo "ERROR: crypto_service_uri in $KMC_CFG contains space in the URI"
    return 1
  fi
  if [[ "$CRYPTO_SERVICE_URI" != https://* ]]; then
    echo "ERROR: crypto_service_uri in $KMC_CFG is not a HTTPS URI"
    return 1
  fi
  echo $CRYPTO_SERVICE_URI
}

function get_access_control
{
  cookie_property=$(grep "^crypto_service_sso_cookie_file" $KMC_CFG)
  if [ "$cookie_property" ]; then
    COOKIE_FILE=$(echo $cookie_property | cut -d'=' -f2)
    COOKIE_FILE=$(eval echo $COOKIE_FILE)	# evaluate bash variable such as $HOME
    if [ "$COOKIE_FILE" ]; then
      error=$(verify_readable_file crypto_service_sso_cookie_file $COOKIE_FILE)
      if [ $? == 0 ]; then
        ACCESS_CONTROL="-b $COOKIE_FILE"
        return 0
      else
        echo $error
      fi
    fi
  fi
  mtls_property=$(grep "^crypto_service_mtls_client_type" $KMC_CFG)
  if [ "$mtls_property" ]; then
    MTLS_VALUE=$(echo $mtls_property | cut -d'=' -f2)
    if [ "$MTLS_VALUE" == "PEM" ]; then
      key_property=$(grep "^crypto_service_mtls_client_key" $KMC_CFG)
      KEY_VALUE=$(echo $key_property | cut -d'=' -f2)
      error=$(verify_readable_file crypto_service_mtls_client_key $KEY_VALUE)
      if [ $? != 0 ]; then
        echo $error
        return 1
      fi
      cert_property=$(grep "^crypto_service_mtls_client_cert" $KMC_CFG)
      CERT_VALUE=$(echo $cert_property | cut -d'=' -f2)
      error=$(verify_readable_file crypto_service_mtls_client_cert $CERT_VALUE)
      if [ $? != 0 ]; then
        echo $error
        return 1
      fi
      ACCESS_CONTROL="--key $KEY_VALUE --cert $CERT_VALUE"
    fi
  fi
}
