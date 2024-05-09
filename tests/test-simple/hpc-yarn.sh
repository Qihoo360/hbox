#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

if [[ ! ${HBOX_HOME-} ]]; then
  . "$(dirname -- "$0")/../ver.sh" # set HBOX_VERSION
  HBOX_HOME="$(dirname -- "$0")/../../hbox-$HBOX_VERSION"
fi

: "${HBOX_CONF_DIR:="$(dirname -- "$0")"/../conf.hpc-yarn}"
export HBOX_CONF_DIR

submit_opts=(--app-name "[HBOX][test] yarn submit")
submit_opts+=(--app-type  fake-app-type)
submit_opts+=(--worker-num 2)
submit_opts+=(--worker-cores 2)
submit_opts+=(--worker-memory 8G)

# command run on containers
submit_opts+=(--hbox-cmd)
submit_opts+=("/bin/sh -xc hostname;pwd;whoami;cat<launch_container.sh;env")

exec "$HBOX_HOME"/bin/hbox-submit "${submit_opts[@]}"
