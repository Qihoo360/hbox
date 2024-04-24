# shellcheck shell=bash

# shellcheck disable=SC2034
JAVA_HOME=/usr/bin/hadoop/software/java8

if [[ ${1-} == run-history-server ]]; then
  # activate hadoop 3.2.1 for history server
  PATH="/usr/bin/hadoop/software/yarn3/bin:$PATH"

  # prefer non standard hdfs-client
  HBOX_PRE_CLASSPATH='/usr/bin/hadoop/software/yarn3/share/hadoop/before_classpath/*'
else
  # activate hadoop 2.7.2
  PATH="/usr/bin/hadoop/software/yarn2/bin:$PATH"
fi
