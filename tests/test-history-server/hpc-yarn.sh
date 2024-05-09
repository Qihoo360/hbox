#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

if [[ ! ${HBOX_HOME-} ]]; then
  # shellcheck disable=SC1091
  . "$(dirname -- "$0")/../ver.sh" # set HBOX_VERSION
  HBOX_HOME="$(dirname -- "$0")/../../hbox-$HBOX_VERSION"
fi

: "${HBOX_CONF_DIR:="$(dirname -- "$0")"/../conf.hpc-yarn}"
export HBOX_CONF_DIR

host=$(hostname -f)

submit_opts=( -D hbox.history.address="$host:10121" )
submit_opts+=( -D hbox.history.webapp.address="$host:19966" )
submit_opts+=( -D hbox.history.webapp.https.address="$host:19967" )
submit_opts+=( -D hbox.history.log.max-age-ms="2147483647" )

# do not nohup
export __HBOX_TEST_HISTORY_SERVER=true

exec "$HBOX_HOME"/sbin/start-history-server.sh "${submit_opts[@]}"
