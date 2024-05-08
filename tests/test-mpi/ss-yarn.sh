#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

: "${HBOX_CONF_DIR:="$(dirname -- "$0")"/../conf.ss-yarn}"
export HBOX_CONF_DIR

exec "$(dirname -- "$0")"/hpc-yarn.sh
