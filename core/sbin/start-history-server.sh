#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

[[ -d ${HBOX_HOME-} ]] || HBOX_HOME="$(cd -- "$(dirname -- "$0")"/.. && pwd)"

# shellcheck source-path=SCRIPTDIR/..
. "$HBOX_HOME/libexec/hbox-common-env.sh" run-history-server
# hbox-common-env.sh setups required and optional environments:
#   JAVACMD - required, path to java binary
#   HBOX_CLASSPATH - required, classpath to run hbox
#   HBOX_PRE_CLASSPATH - optional, classpath before main jar, e.g. special hdfs client
#   HBOX_JAR - required, result array for finding the hbox main jars, may find 0 or multiple ones
#   HBOX_CLIENT_OPTS - optional, java cli opts to pass to hbox client

if (( ${#HBOX_JAR[@]} == 0 )); then
  echo "[ERROR] Failed to find Hbox jar in $HBOX_HOME/lib." >&2
  exit 1
elif (( ${#HBOX_JAR[@]} > 1 )); then
  echo "[ERROR] Found multiple Hbox jars in $HBOX_HOME/lib:" >&2
  printf "  %s\n" "${HBOX_JAR[@]}"
  echo "Please remove all but one jar." >&2
  exit 1
fi

# classpath order:
#  - prepend classpath
#  - hbox history server jar
#  - HBOX_CLASSPATH
HBOX_CLASSPATH="${HBOX_JAR[0]}:$HBOX_CLASSPATH"
[[ ! ${HBOX_PRE_CLASSPATH-} ]] || HBOX_CLASSPATH="$HBOX_PRE_CLASSPATH:$HBOX_CLASSPATH"

if [[ "${__HBOX_TEST_HISTORY_SERVER-}" != true ]]; then
  nohup "$JAVACMD" -cp "$HBOX_CLASSPATH" net.qihoo.hbox.jobhistory.JobHistoryServer "$@" 2>&1 &
else
  exec "$JAVACMD" -cp "$HBOX_CLASSPATH" net.qihoo.hbox.jobhistory.JobHistoryServer "$@"
fi
