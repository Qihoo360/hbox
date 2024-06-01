#!/usr/bin/env bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
set -e

export HBOX_HOME="$(cd "`dirname "$0"`"/..; pwd)"

. "$HBOX_HOME"/conf/hbox-env.sh

# Find the java binary
if [ -n "${JAVA_HOME}" ]; then
  RUNNER="${JAVA_HOME}/bin/java"
else
  if [ `command -v java` ]; then
    RUNNER="java"
  else
    echo "JAVA_HOME is not set" >&2
    exit 1
  fi
fi

HBOX_LIB_DIR=$HBOX_HOME/lib

num_jars="$(ls -1 "$HBOX_LIB_DIR" | grep "^hbox.*hadoop.*\.jar$" | wc -l)"
if [ "$num_jars" -eq "0" ]; then
  echo "Failed to find Hbox jar in $HBOX_LIB_DIR." 1>&2
  exit 1
fi
HBOX_JARS="$(ls -1 "$HBOX_LIB_DIR" | grep "^hbox.*hadoop.*\.jar$" || true)"
if [ "$num_jars" -gt "1" ]; then
  echo "Found multiple Hbox jars in $HBOX_LIB_DIR:" 1>&2
  echo "$HBOX_LIB_DIR" 1>&2
  echo "Please remove all but one jar." 1>&2
  exit 1
fi

HBOX_JAR="${HBOX_LIB_DIR}/${HBOX_JARS}"
LAUNCH_CLASSPATH=$HBOX_CLASSPATH
#LAUNCH_CLASSPATH=$HBOX_JARS:$HBOX_CLASSPATH

# an array that will be used to exec the final command.
CMD=()

while IFS= read -d '' -r ARG; do
  CMD+=("$ARG")


done < <(nohup "$RUNNER" -cp "$LAUNCH_CLASSPATH" "net.qihoo.hbox.jobhistory.JobHistoryServer" "$@"  2>&1 &)
