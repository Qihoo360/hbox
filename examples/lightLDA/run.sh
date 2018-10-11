#!/bin/sh
$XLEARNING_HOME/bin/xl-submit \
   --app-type "lightlda" \
   --files demo.sh \
   --worker-memory 15G \
   --worker-num 2 \
   --ps-memory 3G \
   --ps-num 1 \
   --launch-cmd "sh demo.sh" \
   --cacheArchive /tmp/data/lightLDA/lightLDA.tgz#lightLDA \
   --cacheFile /tmp/data/lightLDA/dict#dict \
   --input /tmp/data/lightLDA/data#data \
   --output /tmp/lightLDA_output#output \
   --app-name "lightLDA_demo" \
   --queue default \
   --conf xlearning.container.env.LD_LIBRARY_PATH=lightLDA/multiverso/third_party/lib \