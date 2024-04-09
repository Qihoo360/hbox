# shellcheck shell=bash

# Set Hbox-specific environment variables here.

unset CLASSPATH
unset HADOOP_CLASSPATH

if [[ ! ${HBOX_HOME-} ]] || [[ ! -d "$HBOX_HOME" ]]; then
  HBOX_HOME="$(cd -- "$(dirname -- "$0")"/.. && pwd)"
fi
export HBOX_HOME

# The java implementation to use.
# TODO special dir??
export JAVA_HOME=$HBOX_HOME/../java/

if [[ ! ${HADOOP_CONF_DIR-} ]]; then
  # TODO set for CDH
  export HADOOP_CONF_DIR="$HBOX_HOME/../hadoop/etc/hadoop:$HBOX_HOME/../yarn/etc/hadoop"
fi

export HBOX_CONF_DIR="$HBOX_HOME/conf"
export HBOX_CLASSPATH="$HBOX_HOME/../lib4yarn/hadoop:$HBOX_CONF_DIR:$HADOOP_CONF_DIR"

for f in "$HBOX_HOME"/lib/*.jar; do
  export HBOX_CLASSPATH=$HBOX_CLASSPATH:$f
done

export HBOX_CLIENT_OPTS="-Xmx1024m"
