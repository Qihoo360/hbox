#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

if [[ ! ${HBOX_HOME-} ]]; then
  . "$(dirname -- "$0")/../ver.sh" # set HBOX_VERSION
  HBOX_HOME="$(dirname -- "$0")/../../hbox-$HBOX_VERSION"
fi

: "${HBOX_CONF_DIR:="$(dirname -- "$0")"/../conf.hpc-yarn}"
export HBOX_CONF_DIR

submit_opts=(--app-name "[HBOX][test] submit a complex command")
submit_opts+=(--app-type  fake-app-type)
submit_opts+=(--worker-num 1)
submit_opts+=(--worker-cores 1)
submit_opts+=(--worker-memory 2G)

exec "$HBOX_HOME"/bin/hbox-submit "${submit_opts[@]}" \
  /bin/sh -xc 'hostname; pwd; whoami; ls -l; ps f; cat launch_container.sh; printf "[%s]\n" "$@"' \
  '' single "one space" "two  spaces" "one	tab" "new
line" '"double quote"' "'single quote'" 'special chars: {[( < #$*%^&-=/`\?!~ > )]}'
