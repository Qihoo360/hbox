#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

: "${HBOX_CONF_DIR:="$(dirname -- "$0")"/../conf.ss-yarn}"
export HBOX_CONF_DIR

export MPI_TAR_NAME=am_mpi_env
export MPI_HDFS_PATH=/user/jiangxinglei/cache/am_mpi_env.tar.gz
export USE_HDFS_MPI=true

exec "$(dirname -- "$0")"/hpc-yarn-mpi.sh
