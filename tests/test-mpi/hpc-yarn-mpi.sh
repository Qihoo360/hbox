#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

: "${HBOX_HOME:="$(dirname -- "$0")"/../../hbox-1.7-SNAPSHOT-hadoop3.2.1}"
: "${HBOX_CONF_DIR:="$(dirname -- "$0")"/../conf.hpc-yarn}"
export HBOX_CONF_DIR

: "${MPI_TAR_NAME:=am_mpi_env}"
: "${MPI_HDFS_PATH:=/home/hdp-ads-algo/jiangxinglei/am_mpi_env.tar.gz}"

submit_opts=(--app-name "[HBOX][test] yarn submit")
submit_opts+=(--app-type  "TENSORNET")
submit_opts+=(--worker-num 2)
submit_opts+=(--worker-cores 2)
submit_opts+=(--worker-memory 8G)
submit_opts+=(--jars "$HBOX_HOME/lib/hbox-web-1.7-SNAPSHOT.jar,$HBOX_HOME/lib/hbox-common-1.7-SNAPSHOT.jar")

if [[ ${USE_HDFS_MPI-} == true ]]; then
  submit_opts+=(--cacheArchive "${MPI_HDFS_PATH}#${MPI_TAR_NAME}")
  submit_opts+=(--conf hbox.mpi.install.dir=./${MPI_TAR_NAME})
fi

# command run on containers
submit_opts+=(--hbox-cmd)
submit_opts+=("/bin/sh -xc hostname;pwd;whoami;{ls,-l,.,tmp};{ps,f};cat<launch_container.sh;{which,hadoop};env")

exec "$HBOX_HOME"/bin/hbox-submit "${submit_opts[@]}"
