#!/bin/sh
$XLEARNING_HOME/bin/xl-submit \
   --app-type "tensorflow" \
   --app-name "tf-demo" \
   --input /tmp/data/tensorflow#data \
   --output /tmp/tensorflow_model#model \
   --files demo.py,dataDeal.py \
   --launch-cmd "python demo.py --data_path=./data --save_path=./model --log_dir=./eventLog --training_epochs=10" \
   --worker-memory 10G \
   --worker-num 2 \
   --worker-cores 3 \
   --ps-memory 2G \
   --ps-num 1 \
   --ps-cores 2 \
