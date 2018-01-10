#!/bin/sh
$XLEARNING_HOME/bin/xl-submit \
   --app-type "tensorflow" \
   --app-name "tf-estimator-demo" \
   --files demo.py \
   --launch-cmd "python demo.py --data_path=hdfs://xlearning.test.host1:9000/tmp/data/tfEstimator --model_path=hdfs://xlearning.test.host1:9000/tmp/estimatorDemoModel" \
   --worker-memory 2G \
   --worker-num 3 \
   --worker-cores 2 \
   --ps-memory 2G \
   --ps-num 1 \
   --ps-cores 2 \
   --queue default \
