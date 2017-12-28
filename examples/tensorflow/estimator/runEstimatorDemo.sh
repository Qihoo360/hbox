#!/bin/sh
$XLEARNING_HOME/bin/xl-submit \
   --app-type "tensorflow" \
   --app-name "tf-estimator-demo" \
   --files estimatorDemo.py \
   --launch-cmd "python estimatorDemo.py --data_path=hdfs://localhost:9000/tmp/data/iris --model_path=hdfs://localhost:9000/tmp/model/estimatorDemo" \
   --worker-memory 2G \
   --worker-num 1 \
   --worker-cores 2 \
   --ps-memory 2G \
   --ps-num 1 \
   --ps-cores 2 \
   --queue default \
