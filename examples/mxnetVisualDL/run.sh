$HBOX_HOME/bin/hbox-submit \
   --app-type "mxnet" \
   --worker-memory 10G \
   --worker-num 1 \
   --files demo.py \
   --cacheFile /tmp/data/mxnet#data \
   --output /tmp/mxnet_single_output#output \
   --app-name "mxnet_demo" \
   --board-logdir log \
   --board-historydir /tmp/mxnet_visualDL_log \
   --queue default \
   python demo.py
