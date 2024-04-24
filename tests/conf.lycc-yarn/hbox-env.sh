# shellcheck shell=bash

# shellcheck disable=SC2034
JAVA_HOME=/usr/bin/hadoop/software/java8

# activate hadoop 3.2.1
PATH="/usr/bin/hadoop/software/yarn3/bin:$PATH"

if [[ ${1-} == run-history-server ]]; then
  # prefer non standard hdfs-client
  HBOX_PRE_CLASSPATH='/usr/bin/hadoop/software/yarn3/share/hadoop/before_classpath/*'
fi
