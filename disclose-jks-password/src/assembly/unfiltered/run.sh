#!/bin/sh

#Example: ./run.sh -d examples/password-dict.txt -k examples/keystore-rsa.jks

DIR=`dirname $0`

MAIN_CLASS=org.xipki.jksfail.Main

if [ "x${JAVA_HOME}" = "x" ]; then
  JAVA_EXEC=java
else
  JAVA_EXEC="${JAVA_HOME}/bin/java"
fi

${JAVA_EXEC} -cp "${DIR}/lib:${DIR}/lib/*" ${MAIN_CLASS} "$@"

