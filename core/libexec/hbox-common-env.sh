# shellcheck shell=bash

# Set Hbox-specific common environment variables here, and load hbox-env.sh for the target cluster
#   JAVACMD - required, path to java binary
#   HBOX_CLASSPATH - required, classpath to run hbox
#   HBOX_PRE_CLASSPATH - optional, classpath before main jar, e.g. special hdfs client
#   HBOX_JAR - required, result array for finding the hbox main jars, may find 0 or multiple ones
#   HBOX_CLIENT_OPTS - optional, java cli opts to pass to hbox client
#   HBOX_EXTRA_ARGS - optional, extra args for hbox client

unset CLASSPATH
unset HADOOP_CLASSPATH
unset HBOX_PRE_CLASSPATH

[[ ${HBOX_HOME-} ]] || HBOX_HOME="$(cd -- "$(dirname -- "$0")"/.. && pwd)"
: "${HBOX_CONF_DIR:="$HBOX_HOME/conf"}"

# export for generating the kill-job command
export HBOX_HOME HBOX_CONF_DIR

: "[DEBUG] hbox home at $HBOX_HOME"
: "[DEBUG] load hbox config at $HBOX_CONF_DIR"

# shellcheck source=/dev/null
[[ ! -f "$HBOX_CONF_DIR"/hbox-env.sh ]] || . "$HBOX_CONF_DIR"/hbox-env.sh "$@"
# hbox-env.sh setups:
#   JDK for the hbox client, via JAVA_HOME or java on $PATH
#   'yarn' command are invokable from $PATH

if ! hash yarn >/dev/null; then
  echo "[ERROR] cannot find the 'yarn' commmond" >&2
  return 64
fi

# Find the java binary
if [[ ${JAVA_HOME-} ]] && [[ -x "${JAVA_HOME}/bin/java" ]]; then
  JAVACMD="${JAVA_HOME}/bin/java"
elif hash java >/dev/null; then
  # shellcheck disable=SC2034
  JAVACMD=java
else
  echo "[ERROR] JAVA_HOME is not set" >&2
  return 65
fi

# classpath order:
#   target cluster conf
#   hbox jars
#   yarn system conf
#   yarn system jars
# shellcheck disable=SC2034
HBOX_CLASSPATH="$HBOX_CONF_DIR:$HBOX_HOME/lib/*:$(yarn classpath)"

# shellcheck disable=SC2034
HBOX_CLIENT_OPTS="-Xmx1024m"

case "${1-}" in
run-submit)
  readarray -t HBOX_JAR < <(find "$HBOX_HOME/" -maxdepth 1 -name "hbox-core-*.jar")
  HBOX_EXTRA_ARGS=()

  if [[ ${OTEL_JAVA_GLOBAL_AUTOCONFIGURE_ENABLED-} == true ]]; then
    # propagate opentelemetry environments to hbox client, AM and all yarn containers
    __propagate_otel_env() {
      local k="${1:?__propagate_otel_env <env_name> [<value>] [<default_value>]}"
      local v="${2:-${!k:-${3:-}}}"
      if [[ ${v-} ]]; then
        HBOX_EXTRA_ARGS+=(--conf "hbox.am.env.$k=$v" --conf "hbox.container.env.$k=$v")
        export "${k?}"
      else
        unset "$k"
      fi
    }

    __propagate_otel_env OTEL_JAVA_GLOBAL_AUTOCONFIGURE_ENABLED
    __propagate_otel_env OTEL_EXPORTER_OTLP_ENDPOINT
    __propagate_otel_env OTEL_EXPORTER_OTLP_TRACES_ENDPOINT
    __propagate_otel_env OTEL_EXPORTER_OTLP_PROTOCOL
    __propagate_otel_env OTEL_EXPORTER_OTLP_TRACES_PROTOCOL
    __propagate_otel_env OTEL_EXPORTER_OTLP_TIMEOUT
    __propagate_otel_env OTEL_EXPORTER_OTLP_TRACES_TIMEOUT
    __propagate_otel_env OTEL_METRICS_EXPORTER '' none

    export OTEL_SERVICE_NAME=hbox OTEL_RESOURCE_ATTRIBUTES

    OTEL_RESOURCE_ATTRIBUTES="service.namespace=net.qihoo${OTEL_RESOURCE_ATTRIBUTES:+,$OTEL_RESOURCE_ATTRIBUTES}"
    OTEL_RESOURCE_ATTRIBUTES="process.parent_pid=$PPID,$OTEL_RESOURCE_ATTRIBUTES"
    OTEL_RESOURCE_ATTRIBUTES="process.pid=$$,$OTEL_RESOURCE_ATTRIBUTES"
    OTEL_RESOURCE_ATTRIBUTES="process.executable.name=hbox-submit,$OTEL_RESOURCE_ATTRIBUTES"
    OTEL_RESOURCE_ATTRIBUTES="process.executable.path=$HBOX_HOME/bin/hbox-submit,$OTEL_RESOURCE_ATTRIBUTES"
    OTEL_RESOURCE_ATTRIBUTES="process.command=$2,$OTEL_RESOURCE_ATTRIBUTES"

    unset OTEL_TRACES_SAMPLER  # use default parentbased_always_on
    unset OTEL_TRACES_EXPORTER # use default oltp
    unset OTEL_PROPAGATORS     # use default tracecontext,baggage
    unset __propagate_otel_env
  else
    unset OTEL_JAVA_GLOBAL_AUTOCONFIGURE_ENABLED
  fi
  ;;

run-history-server) readarray -t HBOX_JAR < <(find "$HBOX_HOME/" -maxdepth 1 -name "hbox-history-server-*.jar") ;;
esac

if [[ ${1-} ]]; then
  if ((${#HBOX_JAR[@]} == 0)); then
    echo "[ERROR] Failed to find Hbox jar in $HBOX_HOME/lib." >&2
    return 66
  elif ((${#HBOX_JAR[@]} > 1)); then
    echo "[ERROR] Found multiple Hbox jars in $HBOX_HOME/lib:" >&2
    printf "  %s\n" "${HBOX_JAR[@]}"
    echo "Please remove all but one jar." >&2
    return 67
  fi
fi
