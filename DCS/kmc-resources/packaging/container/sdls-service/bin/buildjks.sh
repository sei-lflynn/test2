#!/bin/bash

usage() {
>&2 cat << EOF
Usage: $0
  -f - JKS truststore file to create
  -b - PEM formatted CA Bundle to import
  -p - Truststore Password (defaults to changeit if not provided)
EOF
exit 1
}

# Option defaults
JKS_PASS='changeit'

# Parse command line args
args=$(getopt -o f:b:p:h -- "$@")
[[ $? -ne 0 ]] && usage
eval set -- "$args"
while [ : ]; do
  case $1 in
    -h)
      usage 
      ;;
    -b)
      BUNDLE="${2}"
      shift; shift
      ;;
    -f)
      JKS="${2}"
      shift; shift
      ;;
    -p)
      JKS_PASS="${2}"
      shift; shift
      ;;
    --) shift; break;;
    *) >&2 echo Unsupported option: "$1"
      usage 
      ;;
  esac
done

if [ -z "${BUNDLE}" ]; then 
  >&2 echo "ERROR: no CA Bundle specified"
  usage
fi

if [ ! -r "${BUNDLE}" ]; then 
  >&2 echo "ERROR: CA Bundle unreadable: ${BUNDLE}"
  usage
fi

if [ -z "${JKS}" ]; then
  >&2 echo "ERROR: no JKS truststore file specified: ${JKS}"
  usage 
fi

flag=0
TMP=`mktemp`
while IFS= read -r -u3 line; do
	if [[ "$line" =~ ^[-]*BEGIN ]]
  then
    flag=1
		echo $line >> ${TMP}
    continue
  fi

  if [[ "$line" =~ ^[-]*END ]]; then
		flag=2
		echo $line >> ${TMP}
	fi	

	[ $flag -eq 1 ] && echo $line >> ${TMP}

  if [ $flag -eq 2 ]; then
		CERT_CN=`/bin/openssl x509 -in ${TMP} -noout -text | grep -i 'subject:' | awk -F',' '{print $NF}' | cut -f2 -d'=' | cut -c1- | tr ' ' '_'`
    CERT_EXP=`/bin/openssl x509 -in ${TMP} -noout -text | grep -i 'not after :' | awk '{print $(NF-1)}'`

    /bin/keytool -import -alias "${CERT_CN}_${CERT_EXP}" -file ${TMP} -keystore ${JKS} -storepass "${JKS_PASS}" -storetype JKS -trustcacerts -noprompt 2>&1 >/dev/null

    if [ $? -eq 0 ]; then
		  echo "imported $CERT_CN"
    else
      echo "Keytool errors -- fix issues and try again!"
      exit 1
    fi

		flag=0
		rm ${TMP}
		TMP=`mktemp`
	fi
done 3< "${BUNDLE}"
