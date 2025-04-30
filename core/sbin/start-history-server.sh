#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

[[ ${HBOX_HOME-} ]] || HBOX_HOME="$(cd -- "$(dirname -- "$0")"/.. && pwd)"

# shellcheck source-path=SCRIPTDIR/..
. "$HBOX_HOME/libexec/hbox-common-env.sh" run-history-server
# hbox-common-env.sh setups required and optional environments:
#   JAVACMD - required, path to java binary
#   HBOX_CLASSPATH - required, classpath to run hbox
#   HBOX_PRE_CLASSPATH - optional, classpath before main jar, e.g. special hdfs client
#   HBOX_JAR - required, the only hbox main jar for the current command
#   HBOX_CLIENT_OPTS - optional, java cli opts to pass to hbox client
#   HBOX_EXTRA_ARGS - optional, extra args for hbox client

# classpath order:
#  - prepend classpath
#  - hbox history server jar
#  - HBOX_CLASSPATH
HBOX_CLASSPATH="$HBOX_JAR:$HBOX_CLASSPATH"
[[ ! ${HBOX_PRE_CLASSPATH-} ]] || HBOX_CLASSPATH="$HBOX_PRE_CLASSPATH:$HBOX_CLASSPATH"

if [[ ${__HBOX_TEST_HISTORY_SERVER-} != true ]]; then
  nohup "$JAVACMD" -cp "$HBOX_CLASSPATH" net.qihoo.hbox.jobhistory.JobHistoryServer "$@" 2>&1 &
else
  exec "$JAVACMD" -cp "$HBOX_CLASSPATH" net.qihoo.hbox.jobhistory.JobHistoryServer "$@"
fi
