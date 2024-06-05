#!/bin/sh
$HBOX_HOME/bin/hbox-submit \
   --app-type "xflow" \
   --worker-memory 5g \
   --worker-num 2 \
   --ps-memory 2g \
   --ps-num 2 \
   --files xflow_lr,demo.sh \
   --cacheFile hdfs://hbox.test.host1:9000/tmp/data/xflow#data \
   --app-name "xflow" \
   --queue default \
   sh demo.sh
