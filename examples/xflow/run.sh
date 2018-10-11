#!/bin/sh
$XLEARNING_HOME/bin/xl-submit \
   --app-type "xflow" \
   --worker-memory 5g \
   --worker-num 2 \
   --ps-memory 2g \
   --ps-num 2 \
   --files xflow_lr,demo.sh \
   --cacheFile hdfs://xlearning.test.host1:9000/tmp/data/xflow#data \
   --launch-cmd "sh demo.sh" \
   --app-name "xflow" \
   --queue default \
