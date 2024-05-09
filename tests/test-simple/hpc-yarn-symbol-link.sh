#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

unset HBOX_HOME
# shellcheck disable=SC1091
. "$(dirname -- "$0")/../ver.sh" # set HBOX_VERSION
HBOX_HOME="$(dirname -- "$0")/../../hbox-$HBOX_VERSION"
ln -s "$HBOX_HOME" "$(dirname -- "$0")/hbox"
HBOX_HOME="$(dirname -- "$0")/hbox"

# shellcheck disable=SC2064
trap "rm \"$(dirname -- "$0")/hbox\"" EXIT

"$(dirname -- "$0")"/hpc-yarn.sh
