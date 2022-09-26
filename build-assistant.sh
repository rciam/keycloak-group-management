#!/bin/bash

if [[ $# -eq 0 ]]
then
  echo $'Usage: build-assistant.sh [OPTION]... \n OPTION can be: \n --compile-only    compile only the typescript \n --build    to perform a full build of the plugin \n --build-server    to build only the server-side, but pack everything (including the last build of the UI) into the jar';
  exit 1;
fi

if [[ $1 == "--compile-only" ]]
then
  tsc --strictNullChecks --jsx react -p ./src/main/resources/theme/groups/account/src
  exit 0;
fi

if [[ $1 == "--build" ]]
then
  mvn clean package -DskipTests -Dmaven.source.skip
  exit 0;
fi

if [[ $1 == "--build-server" ]]
then
  mvn clean package -DskipTests -Dmaven.source.skip -DskipAccount2
  exit 0;
fi

