#!/bin/bash
################################################################################
#
# finish_deploy.sh - creates local users if needed, sets ownership/group and 
#                    file permissions after an archive install
#
# Options:
#   -a | --all-services - deploy all KMC services
#          (implies --kmc-crypto-service --kmc-sdls-service
#           --kmc-sa-mgmt-service)
#   -c | --crypto-service  - Finish deployment of KMC Crypto Service
#   -h | --help - Display script usage
#   -s | --sdls-service - Finish deployment of KMC SDLS Service
#   -m | --sa-mgmt-service - Finish deployment of KMC SA Management Service
#
################################################################################
# Setup execution environment
source $(dirname "$0")/setenv.sh

usage() {
>&2 cat << EOF
Usage: $0
  [ -a | --all-services ] - deploy ALL KMC services
  [ -c | --crypto-service ] - deploy KMC Crypto Service
  [ -h | --help ] - display deploy.sh script usage
  [ -m | --sa-mgmt-service ] - deploy KMC SA Management Service
  [ -s | --sdls-service ] - deploy KMC SDLS REST API
EOF
exit 1
}

# Parse Command line arguments
args=$(getopt -a -o achims --long help,img,crypto-service,sdls-service,sa-mgmt-service,all-services -- "$@")
if [[ $? -ne 0 ]]; then
    usage
fi

eval set -- "$args"
while [ : ]; do
  case $1 in
    -h | --help)
      usage;;
    -c | --crypto-service)
      DEPLOY_KMC_CRYPTO_SERVICE=1
      shift
      ;;
    -i | --img)
      CRYPTOUSR_SHELL=/bin/bash
      shift
      ;;
    -s | --sdls-service)
      DEPLOY_KMC_SDLS_SERVICE=1
      shift
      ;;
    -m | --sa-mgmt-service)
      DEPLOY_KMC_SA_MGMT_SERVICE=1
      shift
      ;;
    -a | --all-services)
      DEPLOY_KMC_CRYPTO_SERVICE=1
      DEPLOY_KMC_SDLS_SERVICE=1
      DEPLOY_KMC_SA_MGMT_SERVICE=1
      shift
      ;;
    --) shift; break;;
    *)  >&2 echo Unsupported option: "$1"
        usage ;;
  esac
done

# Create local users/groups as needed
# Make sure effective UID is 0 (elevated privileges) -- deployment requires
# privileges to create users/groups and install files outside of 
# user-writeable space
if [ "${EUID}" != "0" ]; then
  echo "ERROR: KMC local deployment requires elevated (root) privileges to be able to write necessary files & directories.  Please execute ${0} with elevated privileges." >&2
  exit 1
fi

# Verify KMC is deployed before attempting to make any changes
if [ ! -d "${PREFIX}" ]; then
  echo "ERROR: KMC is not deployed in ${PREFIX}.  Please deploy KMC or rebuild with intended PREFIX setting." >&2
  exit 1
fi

# Define users/groups as needed
# Users Group
USERSGRP="$(/bin/getent group "${USERSGRP_NAME}")"
if [ $? -ne 0 ]; then
  # group name doesn't already exist, check gid
  USERSGRP="$(/bin/getent group "${USERSGRP_GID}")"
  if [ $? -ne 0 ]; then
    # group doesn't exist, create locally
    if [ -n "${USERSGRP_MEMBERS}" ]; then
      /sbin/groupadd -g "${USERSGRP_GID}" -U "$( echo "${USERSGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' )" "${USERSGRP_NAME}"
    else
      /sbin/groupadd -g "${USERSGRP_GID}" "${USERSGRP_NAME}"
    fi
  else
    # gid already exists, but with a different name
    NAME=$(echo "${USERSGRP}" | /bin/cut -f1 -d':')
    echo "WARNING: GID ${USERSGRP_GID} already exists, but is named ${NAME}.  Changing deployment config to use ${NAME} instead of ${USERSGRP_NAME} for the users group." 1>&2
    echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
    sleep 5
    USERSGRP_NAME=${NAME}
    # Make sure USERSGRP_MEMBERS are in the group membership list.  
    # Only add if group is local (in /etc/group), otherwise warn.
    if [ -n "${USERSGRP_MEMBERS}" ]; then
      # Iterate over defined members list, determine necessary additions
      CUR_MEMBERS=$(echo "${USERSGRP}" | /bin/cut -f3 -d':')
      GROUP_MEMBERS_ADD=""
      for USER in $( echo "${USERSGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' ); do
        echo "${CUR_MEMBERS}" | grep "${USER}" >/dev/null 2>&1
        if [ $? -ne 0 ]; then
          GROUP_MEMBERS_ADD+="${USER} "
        fi
      done
      /bin/grep -E "^${USERSGRP_NAME}:" /etc/group  >/dev/null 2>&1
      if [ $? -eq 0 ]; then
        # Group is locally defined.  
        /sbin/groupmod -a -U "${GROUP_MEMBERS_ADD}" "${USERSGRP_NAME}"
      else
        # Group is network defined
        echo "WARNING: Users Group (${USERSGRP_NAME}) is provided by a network-based directory (LDAP, Active Directory, etc.).  KMC Deployment cannot change group membership lists.  Users that need to be added: ${GROUP_MEMBERS_ADD}" 1>&2
      fi
    fi
  fi
else
  # named group already exists, check to make sure the GID matches
  GID=$(echo "${USERSGRP}" | /bin/cut -f3 -d':')
  if [ "${GID}" != "${USERSGRP_GID}" ]; then
    echo "WARNING: ${USERSGRP_NAME} already exists, but is has GID ${GID}.  Changing deployment config to use ${GID} instead of ${USERSGRP_GID} for the users group GID." 1>&2
    echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
    sleep 5
    USERSGRP_GID=${GID}
    # Make sure USERSGRP_MEMBERS are in the group membership list.  
    # Only add if group is local (in /etc/group), otherwise warn.
    if [ -n "${USERSGRP_MEMBERS}" ]; then
      # Iterate over defined members list, determine necessary additions
      CUR_MEMBERS=$(echo "${USERSGRP}" | /bin/cut -f3 -d':')
      GROUP_MEMBERS_ADD=""
      for USER in $( echo "${USERSGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' ); do
        echo "${CUR_MEMBERS}" | grep "${USER}" >/dev/null 2>&1
        if [ $? -ne 0 ]; then
          GROUP_MEMBERS_ADD+="${USER} "
        fi
      done
      /bin/grep -E "^${USERSGRP_NAME}:" /etc/group  >/dev/null 2>&1
      if [ $? -eq 0 ]; then
        # Group is locally defined.  
        /sbin/groupmod -a -U "${GROUP_MEMBERS_ADD}" "${USERSGRP_NAME}"
      else
        # Group is network defined
        echo "WARNING: Users Group (${USERSGRP_NAME}) is provided by a network-based directory (LDAP, Active Directory, etc.).  KMC Deployment cannot change group membership lists.  Users that need to be added: ${GROUP_MEMBERS_ADD}" 1>&2
      fi
    fi
  fi
fi

# Crypto Group
CRYPTOGRP=$(/bin/getent group "${CRYPTOGRP_NAME}")
if [ $? -ne 0 ]; then
  # group name doesn't already exist, check gid
  CRYPTOGRP=$(/bin/getent group "${CRYPTOGRP_GID}")
  if [ $? -ne 0 ]; then
    # group doesn't exist, create locally
    if [ -n "${CRYPTOGRP_MEMBERS}" ]; then
      /sbin/groupadd -g "${CRYPTOGRP_GID}" -U "$( echo "${CRYPTOGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' )" "${CRYPTOGRP_NAME}"
    else
      /sbin/groupadd -g "${CRYPTOGRP_GID}" "${CRYPTOGRP_NAME}"
    fi
  else
    # gid already exists, but with a different name
    NAME=$(echo "${CRYPTOGRP}" | /bin/cut -f1 -d':')
    echo "WARNING: GID ${CRYPTOGRP_GID} already exists, but is named ${NAME}.  Changing deployment config to use ${NAME} instead of ${CRYPTOGRP_NAME} for the crypto group." 1>&2
    echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
    sleep 5
    CRYPTOGRP_NAME=${NAME}
    # Make sure CRYPTOGRP_MEMBERS are in the group membership list.  
    # Only add if group is local (in /etc/group), otherwise warn.
    if [ -n "${CRYPTOGRP_MEMBERS}" ]; then
      # Iterate over defined members list, determine necessary additions
      CUR_MEMBERS=$(echo "${CRYPTOGRP}" | /bin/cut -f3 -d':')
      GROUP_MEMBERS_ADD=""
      for USER in $( echo "${CRYPTOGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' ); do
        echo "${CUR_MEMBERS}" | grep "${USER}"  >/dev/null 2>&1
        if [ $? -ne 0 ]; then
          GROUP_MEMBERS_ADD+="${USER} "
        fi
      done
      /bin/grep -E "^${CRYPTOGRP_NAME}:" /etc/group  >/dev/null 2>&1
      if [ $? -eq 0 ]; then
        # Group is locally defined.  
        /sbin/groupmod -a -U "${GROUP_MEMBERS_ADD}" "${CRYPTOGRP_NAME}"
      else
        # Group is network defined
        echo "WARNING: Crypto Group (${CRYPTOGRP_NAME}) is provided by a network-based directory (LDAP, Active Directory, etc.).  KMC Deployment cannot change group membership lists.  Users that need to be added: ${GROUP_MEMBERS_ADD}" 1>&2
      fi
    fi
  fi
else
  # named group already exists, check to make sure the GID matches
  GID=$(echo "${CRYPTOGRP}" | /bin/cut -f3 -d':')
  if [ "${GID}" != "${CRYPTOGRP_GID}" ]; then
    echo "WARNING: ${CRYPTOGRP_NAME} already exists, but is has GID ${GID}.  Changing deployment config to use ${GID} instead of ${CRYPTOGRP_GID} for the crypto group GID." 1>&2
    echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
    sleep 5
    CRYPTOGRP_GID=${GID}
    # Make sure CRYPTOGRP_MEMBERS are in the group membership list.  
    # Only add if group is local (in /etc/group), otherwise warn.
    if [ -n "${CRYPTOGRP_MEMBERS}" ]; then
      # Iterate over defined members list, determine necessary additions
      CUR_MEMBERS=$(echo "${CRYPTOGRP}" | /bin/cut -f3 -d':')
      GROUP_MEMBERS_ADD=""
      for USER in $( echo "${CRYPTOGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' ); do
        echo "${CUR_MEMBERS}" | grep "${USER}" >/dev/null 2>&1
        if [ $? -ne 0 ]; then
          GROUP_MEMBERS_ADD+="${USER} "
        fi
      done
      /bin/grep -E "^${CRYPTOGRP_NAME}:" /etc/group  >/dev/null 2>&1
      if [ $? -eq 0 ]; then
        # Group is locally defined.  
        /sbin/groupmod -a -U "${GROUP_MEMBERS_ADD}" "${CRYPTOGRP_NAME}"
      else
        # Group is network defined
        echo "WARNING: Crypto Group (${CRYPTOGRP_NAME}) is provided by a network-based directory (LDAP, Active Directory, etc.).  KMC Deployment cannot change group membership lists.  Users that need to be added: ${GROUP_MEMBERS_ADD}" 1>&2
      fi
    fi
  fi
fi

# Config Group
CFGGROUP=$(/bin/getent group "${CFGGRP_NAME}")
if [ $? -ne 0 ]; then
  # group name doesn't already exist, check gid
  CFGGROUP=$(/bin/getent group "${CFGGRP_GID}")
  if [ $? -ne 0 ]; then
    # group doesn't exist, create locally
    if [ -n "${CFGGRP_MEMBERS}" ]; then
      /sbin/groupadd -g "${CFGGRP_GID}" -U "$( echo "${CFGGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' )" "${CFGGRP_NAME}"
    else
      /sbin/groupadd -g "${CFGGRP_GID}" "${CFGGRP_NAME}"
    fi
  else
    # gid already exists, but with a different name
    NAME=$(echo "${CFGGROUP}" | /bin/cut -f1 -d':')
    echo "WARNING: GID ${CFGGRP_GID} already exists, but is named ${NAME}.  Changing deployment config to use ${NAME} instead of ${CFGGRP_NAME} for the config group." 1>&2
    echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
    sleep 5
    CFGGRP_NAME=${NAME}
    # Make sure CFGGRP_MEMBERS are in the group membership list.  
    # Only add if group is local (in /etc/group), otherwise warn.
    if [ -n "${CFGGRP_MEMBERS}" ]; then
      # Iterate over defined members list, determine necessary additions
      CUR_MEMBERS=$(echo "${CFGGROUP}" | /bin/cut -f3 -d':')
      GROUP_MEMBERS_ADD=""
      for USER in $( echo "${CFGGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' ); do
        echo "${CUR_MEMBERS}" | grep "${USER}" >/dev/null 2>&1
        if [ $? -ne 0 ]; then
          GROUP_MEMBERS_ADD+="${USER} "
        fi
      done
      /bin/grep -E "^${CFGGRP_NAME}:" /etc/group  >/dev/null 2>&1
      if [ $? -eq 0 ]; then
        # Group is locally defined.  
        /sbin/groupmod -a -U "${GROUP_MEMBERS_ADD}" "${CFGGRP_NAME}"
      else
        # Group is network defined
        echo "WARNING: Config Group (${CFGGRP_NAME}) is provided by a network-based directory (LDAP, Active Directory, etc.).  KMC Deployment cannot change group membership lists.  Users that need to be added: ${GROUP_MEMBERS_ADD}" 1>&2
      fi
    fi
  fi
else
  # named group already exists, check to make sure the GID matches
  GID=$(echo "${CFGGROUP}" | /bin/cut -f3 -d':')
  if [ "${GID}" != "${CFGGRP_GID}" ]; then
    echo "WARNING: ${CFGGRP_NAME} already exists, but is has GID ${GID}.  Changing deployment config to use ${GID} instead of ${CFGGRP_GID} for the config group GID." 1>&2
    echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
    sleep 5
    CFGGRP_GID=${GID}
    # Make sure CFGGRP_MEMBERS are in the group membership list.  
    # Only add if group is local (in /etc/group), otherwise warn.
    if [ -n "${CFGGRP_MEMBERS}" ]; then
      # Iterate over defined members list, determine necessary additions
      CUR_MEMBERS=$(echo "${CFGGRP}" | /bin/cut -f3 -d':')
      GROUP_MEMBERS_ADD=""
      for USER in $( echo "${CFGGRP_MEMBERS}" | /bin/tr -d '[:space:]' | /bin/sed 's/,/ /g' ); do
        echo "${CUR_MEMBERS}" | grep "${USER}" >/dev/null 2>&1
        if [ $? -ne 0 ]; then
          GROUP_MEMBERS_ADD+="${USER} "
        fi
      done
      /bin/grep -E "^${CFGGRP_NAME}:" /etc/group  >/dev/null 2>&1
      if [ $? -eq 0 ]; then
        # Group is locally defined.  
        /sbin/groupmod -a -U "${GROUP_MEMBERS_ADD}" "${CFGGRP_NAME}"
      else
        # Group is network defined
        echo "WARNING: Config Group (${CFGGRP_NAME}) is provided by a network-based directory (LDAP, Active Directory, etc.).  KMC Deployment cannot change group membership lists.  Users that need to be added: ${GROUP_MEMBERS_ADD}" 1>&2
      fi
    fi
  fi
fi

# Crypto User
CRYPTOUSER=$(/bin/getent passwd "${CRYPTOUSR_NAME}")
if [ $? -ne 0 ]; then
  # username does not exist, check UID
  CRYPTOUSER=$(/bin/getent passwd "${CRYPTOUSR_UID}")
  if [ $? -ne 0 ]; then
    # UID is not defined, create local user
    /sbin/useradd -u "${CRYPTOUSR_UID}" -g "${CRYPTOGRP_NAME}" -d "${CRYPTOUSR_HOME}" -M -c "${CRYPTOUSR_COMMENT}" -s "${CRYPTOUSR_SHELL}" "${CRYPTOUSR_NAME}"
  else
    # UID already exists, check to see if username matches
    NAME=$(echo "$CRYPTOUSER" | /bin/cut -f1 -d':')
    echo "WARNING: UID ${CRYPTOUSR_UID} already exists, but is assigned to user ${NAME}.  Changing deployment config to use ${NAME} instead of ${CRYPTOUSR_NAME} for the service (crypto) user." 1>&2
    echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
    sleep 5
    CRYPTOUSR_NAME=${NAME}
  fi
else
  # username already exists, check to see if UID matches
  C_UID=$(echo "${CRYPTOUSER}" | cut -f3 -d':')
  if [ "${C_UID}" != "${CRYPTOUSR_UID}" ]; then
    echo "WARNING: Username ${CRYPTOUSR_NAME} already exists, but is assigned to UID ${UID}.  Changing deployment config to use ${UID} instead of ${CRYPTOUSR_UID} for the service (crypto) user UID." 1>&2
    echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
    sleep 5
    CRYPTOUSR_UID=${C_UID}
 fi
fi

# Config User
CFGUSER=$(/bin/getent passwd "${CFGUSR_NAME}")
if [ $? -ne 0 ]; then
  # username does not exist, check UID
  CFGUSER=$(/bin/getent passwd "${CFGUSR_UID}")
  if [ $? -ne 0 ]; then
    # UID is not defined, create local user
    /sbin/useradd -u "${CFGUSR_UID}" -g "${CFGGRP_NAME}" -d "${CFGUSR_HOME}" -M -c "${CFGUSR_COMMENT}" -s "${CFGUSR_SHELL}" "${CFGUSR_NAME}"
  else
    # UID already exists, check to see if username matches
    NAME=$(echo "$CFGUSER" | /bin/cut -f1 -d':')
    echo "WARNING: UID ${CFGUSR_UID} already exists, but is assigned to user ${NAME}.  Changing deployment config to use ${NAME} instead of ${CFGUSR_NAME} for the config user." 1>&2
    echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
    sleep 5
    CFGUSR_NAME=${NAME}
  fi
else
  # username already exists, check to see if UID matches
  C_UID=$(echo "${CFGUSER}" | cut -f3 -d':')
  if [ "${C_UID}" != "${CFGUSR_UID}" ]; then
    echo "WARNING: Username ${CFGUSR_NAME} already exists, but is assigned to UID ${UID}.  Changing deployment config to use ${UID} instead of ${CFGUSR_UID} for the config user UID." 1>&2
    echo "  Cancel (Control-C) within 5 seconds to abort deployment..." 1>&2
    sleep 5
    CFGUSR_UID=${C_UID}
  fi
fi

# Set file owner, group, permissions as needed
/bin/chown -R root:root ${PREFIX}
/bin/find ${PREFIX} -type d -exec /bin/chmod 0755 {} \;
/bin/find ${PREFIX} -type f -exec /bin/chmod 0644 {} \;
/bin/find ${PREFIX} -name \*.so -exec /bin/chmod 0755 {} \;

/bin/chmod 0755 ${BINPATH}/*
/bin/chown ${CFGUSR_UID} ${CFGPATH}
/bin/chown ${CFGUSR_UID}:${USERSGRP_GID} ${CFGPATH}/kmc*
/bin/chgrp ${USERSGRP_GID} ${LOGPATH}
/bin/chmod 2770 ${LOGPATH}
/bin/chmod 0755 ${TESTPATH}/bin/*

# Crypto Service
if [ ${DEPLOY_KMC_CRYPTO_SERVICE} -eq 1 ]; then
  /bin/chown -R ${CFGUSR_UID}:${CRYPTOGRP_GID} ${CRYPTOSVC_CFGPATH}
  /bin/chown -R root:${CRYPTOGRP_GID} ${CRYTPOSVC_LIBPATH} ${CRYPTOSVC_LOGPATH} ${CRYPTOSVC_PREFIX}/work/Tomcat/localhost

  /bin/chmod 0750 ${CRYPTOSVC_PREFIX}/bin/*
  /bin/chmod 0750 ${CRYPTOSVC_CFGPATH} ${CRYTPOSVC_LIBPATH}
  /bin/chmod 0640 ${CRYPTOSVC_CFGPATH}/* ${CRYTPOSVC_LIBPATH}/*
  /bin/chmod 0640 ${CRYPTOSVC_CFGPATH}/* ${CRYTPOSVC_LIBPATH}/*
  /bin/chmod 2770 ${CRYPTOSVC_LOGPATH} ${CRYPTOSVC_PREFIX}/work/Tomcat/localhost
fi

# SDLS Service
if [ ${DEPLOY_KMC_SDLS_SERVICE} -eq 1 ]; then
  /bin/chown -R ${CFGUSR_UID}:${CRYPTOGRP_GID} ${SDLSSVC_CFGPATH}
  /bin/chown -R root:${CRYPTOGRP_GID} ${SDLSSVC_LIBPATH} ${SDLSSVC_LOGPATH} ${SDLSSVC_PREFIX}/work/Tomcat/localhost

  /bin/chmod 0750 ${SDLSSVC_PREFIX}/bin/*
  /bin/chmod 0750 ${SDLSSVC_CFGPATH} ${SDLSSVC_LIBPATH}
  /bin/chmod 0640 ${SDLSSVC_CFGPATH}/* ${SDLSSVC_LIBPATH}/*
  /bin/chmod 2770 ${SDLSSVC_LOGPATH} ${SDLSSVC_PREFIX}/work/Tomcat/localhost
fi

# SA-Mgmt Service
if [ ${DEPLOY_KMC_SA_MGMT_SERVICE} -eq 1 ]; then
  /bin/chown -R ${CFGUSR_UID}:${CRYPTOGRP_GID} ${SAMGMTSVC_CFGPATH}
  /bin/chown -R root:${CRYPTOGRP_GID} ${SAMGMTSVC_LIBPATH} ${SAMGMTSVC_LOGPATH} ${SAMGMTSVC_PREFIX}/work/Tomcat/localhost

  /bin/chmod 0750 ${SAMGMTSVC_PREFIX}/bin/*
  /bin/chmod 0750 ${SAMGMTSVC_CFGPATH} ${SAMGMTSVC_LIBPATH}
  /bin/chmod 0640 ${SAMGMTSVC_CFGPATH}/* ${SAMGMTSVC_LIBPATH}/*
  /bin/chmod 2770 ${SAMGMTSVC_LOGPATH} ${SAMGMTSVC_PREFIX}/work/Tomcat/localhost
fi



