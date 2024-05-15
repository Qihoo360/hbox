# shellcheck shell=bash

# find prefix
for _hbox_prefix in /usr/bin/hadoop/software /home/yarn/software; do
  [[ ! -e "$_hbox_prefix/java8" ]] || break
done

if  [[ -e "$_hbox_prefix/java8" ]]; then
  # shellcheck disable=SC2034
  JAVA_HOME="$_hbox_prefix/java8"

  # activate hadoop 3.2.1
  PATH="$_hbox_prefix/yarn3/bin:$PATH"

  if [[ ${1-} == run-history-server ]]; then
    # prefer non standard hdfs-client
    # shellcheck disable=SC2034
    HBOX_PRE_CLASSPATH="$_hbox_prefix"'/yarn3/share/hadoop/before_classpath/*'
  fi
fi

unset _hbox_prefix
