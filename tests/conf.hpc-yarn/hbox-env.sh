# shellcheck shell=bash

# find prefix
for _hbox_prefix in /usr/bin/hadoop/software /home/yarn/software; do
  [[ ! -e "$_hbox_prefix/java8" ]] || break
done

if  [[ -e "$_hbox_prefix/java8" ]]; then
  # shellcheck disable=SC2034
  JAVA_HOME="$_hbox_prefix/java8"

  if [[ ${1-} == run-history-server ]]; then
    # activate hadoop 3.2.1 for history server
    PATH="$_hbox_prefix/yarn3/bin:$PATH"

    # prefer non standard hdfs-client
    # shellcheck disable=SC2034
    HBOX_PRE_CLASSPATH="$_hbox_prefix"'/yarn3/share/hadoop/before_classpath/*'
  else
    # activate hadoop 2.7.2
    PATH="$_hbox_prefix/yarn2/bin:$PATH"
  fi
fi

unset _hbox_prefix
