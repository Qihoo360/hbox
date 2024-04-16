#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

exec "$(dirname -- "$0")"/hpc-yarn.sh ss-yarn
