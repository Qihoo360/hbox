# shellcheck shell=bash

# Set Hbox-specific common environment variables here, and load hbox-env.sh for the target cluster
#   JAVACMD - required, path to java binary
#   HBOX_CLASSPATH - required, classpath to run hbox
#   HBOX_JARS - required, result array for finding the hbox main jars, may find 0 or multiple ones
#   HBOX_CLIENT_OPTS - optional, java cli opts to pass to hbox client

unset CLASSPATH
unset HADOOP_CLASSPATH

[[ -d ${HBOX_HOME-} ]] || HBOX_HOME="$(cd -- "$(dirname -- "$0")"/.. && pwd)"

if [[ ${HBOX_CONF_DIR-} ]]; then
  : "[DEBUG] load hbox config at $HBOX_CONF_DIR"
else
  # detect target yarn cluster
  HBOX_TARGET_CLUSTER=hpc-yarn
  while (( $# > 0 )); do
    case "${1-}" in
    (-help|--help) shift ;;
    (-cluster|--cluster)
      shift
      HBOX_TARGET_CLUSTER="${1-"$HBOX_TARGET_CLUSTER"}"
      shift || :
      ;;
    (--*|-*) shift 2 || : ;;
    (*) break ;;
    esac
  done
  HBOX_CONF_DIR="$HBOX_HOME/conf.$HBOX_TARGET_CLUSTER"
  : "[DEBUG] load hbox config at $HBOX_CONF_DIR for cluster $HBOX_TARGET_CLUSTER"
  unset HBOX_TARGET_CLUSTER
fi

# export for generating the kill-job command
export HBOX_HOME HBOX_CONF_DIR

# shellcheck source=/dev/null
[[ ! -f "$HBOX_CONF_DIR"/hbox-env.sh ]] || . "$HBOX_CONF_DIR"/hbox-env.sh
# hbox-env.sh setups:
#   JDK for the hbox client, via JAVA_HOME or java on $PATH
#   'yarn' command are invokable from $PATH

if ! hash yarn >/dev/null; then
  echo "[ERROR] cannot find the 'yarn' commmond" >&2
  return 65
fi

# Find the java binary
if [[ ${JAVA_HOME-} ]] && [[ -x "${JAVA_HOME}/bin/java" ]]; then
  JAVACMD="${JAVA_HOME}/bin/java"
elif hash java >/dev/null; then
  # shellcheck disable=SC2034
  JAVACMD=java
else
  echo "[ERROR] JAVA_HOME is not set" >&2
  return 64
fi

# classpath order:
#   target cluster conf
#   hbox common conf
#   hbox jars
#   yarn system conf
#   yarn system jars
# shellcheck disable=SC2034
HBOX_CLASSPATH="$HBOX_CONF_DIR:$HBOX_HOME/conf:$HBOX_HOME/lib/*:$(yarn classpath)"

# shellcheck disable=SC2034
HBOX_CLIENT_OPTS="-Xmx1024m"

# shellcheck disable=SC2034
readarray -t HBOX_JAR < <(find "$HBOX_HOME/lib" -maxdepth 1 -name "hbox*hadoop*.jar")

#if [ -z $HADOOP_CONF_DIR ];then
    #export HADOOP_CONF_DIR=$HBOX_HOME/../yarn/etc/hadoop/:$HBOX_HOME/../hadoop/etc/hadoop/
#fi
#export HBOX_CONF_DIR=$HBOX_HOME/conf/
#export HBOX_CLASSPATH="$HBOX_HOME/lib/*.jar:$HBOX_CONF_DIR:$HADOOP_CONF_DIR"
