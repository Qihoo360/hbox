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

submit_opts=(--app-name "[HBOX][test] submit a mpi base application")
submit_opts+=(--app-type  "TENSORNET")
submit_opts+=(--worker-num 2)
submit_opts+=(--worker-cores 2)
submit_opts+=(--worker-memory 8G)

exec "$HBOX_HOME"/bin/hbox-submit "${submit_opts[@]}" \
 /bin/sh -xc 'hostname; pwd; whoami; ls -alh; cat launch_container.sh; env'
