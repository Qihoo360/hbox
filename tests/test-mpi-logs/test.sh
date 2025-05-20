#!/usr/bin/env bash

set -euo pipefail
[[ ${DEBUG-} != true ]] || set -x

if [[ ! ${HBOX_HOME-} ]]; then
  # shellcheck disable=SC1091
  . "$(dirname -- "$0")/../ver.sh" # set HBOX_VERSION
  HBOX_HOME="$(dirname -- "$0")/../../hbox-$HBOX_VERSION"
fi

submit_opts=(--app-name "[HBOX][test] test mpi logs")
submit_opts+=(--conf hbox.agg.all.mpi.stderr) # now --conf cannot put at last just before submitting commands
submit_opts+=(--app-type "MPI")
submit_opts+=(--worker-num 3)
submit_opts+=(--worker-cores 1)
submit_opts+=(--worker-memory 1G)

output="$(dirname -- "$0")/client.log"

echo "[INFO] Submit job, write log to $output"

(
  set -x
  time "$HBOX_HOME"/bin/hbox-submit "${submit_opts[@]}" \
    /bin/sh -c 'seq 110000 -1 100000 & seq 205000 -1 200000 >&2 & wait' >"$output" 2>&1
)

missing=0

expect=10001
case "$(grep -c '^1' -- "$output")" in
"$expect") ;;
"$((expect - 1))")
  missing=$((missing + 1))
  echo "[WARN] rank 0 stdout misses one line"
  ;;
*)
  echo "[ERROR] rank 0 stdout is incorrect" >&2
  exit 1
  ;;
esac

expect=5001
case "$(grep -c 'rank 0 stderr' -- "$output")" in
"$expect") ;;
"$((expect - 1))")
  missing=$((missing + 1))
  echo "[WARN] rank 0 stderr misses one line"
  ;;
*)
  echo "[ERROR] rank 0 stderr is incorrect" >&2
  exit 1
  ;;
esac

expect=5001
for r in 1 2; do
  case "$(grep -c "rank $r stderr" -- "$output")" in
  "$expect") ;;
  "$((expect - 1))")
    missing=$((missing + 1))
    echo "[WARN] rank $r stderr misses one line"
    ;;
  *)
    echo "[ERROR] rank $r stderr is incorrect" >&2
    exit 1
    ;;
  esac
done

if ((missing == 0)); then
  echo "[INFO] SUCCESS! All logs are written into $output"
else
  echo "[INFO] success with $missing missing logs. All logs are written into $output"
fi
