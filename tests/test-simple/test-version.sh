#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

unset HBOX_HOME
# shellcheck disable=SC1091
. "$(dirname -- "$0")/../ver.sh" # set HBOX_VERSION
HBOX_HOME="$(dirname -- "$0")/../../hbox-$HBOX_VERSION"

ver=$( "$HBOX_HOME"/bin/hbox-submit --version 2>/dev/null | grep 'hbox version' | cut -d ' '  -f3 )

if [[ $ver != "$HBOX_VERSION" ]]; then
  echo "[ERROR] expect version $HBOX_VERSION, but get $ver" >&2
  exit 1
fi
