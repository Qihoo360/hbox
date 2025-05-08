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

# shellcheck disable=SC2016
exec "$HBOX_HOME"/bin/hbox-submit "${submit_opts[@]}" \
/bin/sh -xc 'hostname; pwd; whoami; ls -alh; cat launch_container.sh; env
cid=( $(env | grep -o '"'container_[a-f0-9]\+[_0-9]\+'"' | sort -u | grep -v "${CONTAINER_ID:-$(basename "$PWD")}") );
if (( ${#cid[@]} > 0 )); then
  echo "[ERROR] found non local container id:" >&2
  printf "%s\n" "${cid[@]}"
  exit 1
fi
env | grep "PMIX_INSTALL_PREFIX[=]"
if (( $? != 1 )); then
  echo "[ERROR] PMIX_INSTALL_PREFIX should be unset, but set to [$PMIX_INSTALL_PREFIX]" >&2
  exit 1
fi
'
