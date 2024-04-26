#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

: "${HBOX_CONF_DIR:="$(dirname -- "$0")"/../conf.lycc-yarn}"
export HBOX_CONF_DIR

exec "$(dirname -- "$0")"/hpc-yarn-mpi.sh
