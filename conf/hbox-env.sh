# Set Hadoop-specific environment variables here.

# The only required environment variable is JAVA_HOME.  All others are
# optional.  When running a distributed configuration it is best to
# set JAVA_HOME in this file, so that it is correctly defined on
# remote nodes.

# The java implementation to use.

unset CLASSPATH
unset HADOOP_CLASSPATH
unset HBASE_CLASSPATH
unset HIVE_CLASSPATH

export HBOX_HOME="$(cd "`dirname "$0"`"/..; pwd)"
export JAVA_HOME=$HBOX_HOME/../java/
if [ -z $HADOOP_CONF_DIR ];then
    export HADOOP_CONF_DIR=$HBOX_HOME/../hadoop/etc/hadoop/:$HBOX_HOME/../yarn/etc/hadoop/
fi
export HBOX_CONF_DIR=$HBOX_HOME/conf/
export HBOX_CLASSPATH="$HBOX_HOME/../lib4yarn/hadoop/:$HBOX_CONF_DIR:$HADOOP_CONF_DIR"

for f in $HBOX_HOME/lib/*.jar; do
    export HBOX_CLASSPATH=$HBOX_CLASSPATH:$f
done

export HBOX_CLIENT_OPTS="-Xmx1024m"
