#!/bin/sh
$HBOX_HOME/bin/hbox-submit \
   --app-type "lightlda" \
   --files demo.sh \
   --worker-memory 15G \
   --worker-num 2 \
   --ps-memory 3G \
   --ps-num 1 \
   --cacheArchive /tmp/data/lightLDA/lightLDA.tgz#lightLDA \
   --cacheFile /tmp/data/lightLDA/dict#dict \
   --input /tmp/data/lightLDA/data#data \
   --output /tmp/lightLDA_output#output \
   --app-name "lightLDA_demo" \
   --queue default \
   --conf hbox.container.env.LD_LIBRARY_PATH=lightLDA/multiverso/third_party/lib \
   sh demo.sh
