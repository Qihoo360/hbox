$HBOX_HOME/bin/hbox-submit \
  --app-type "distlightgbm" \
  --app-name "distLightGBM-demo" \
  --files train.conf,demo.sh \
  --worker-num 2 \
  --worker-memory 10G \
  --cacheArchive /tmp/data/distLightGBM/LightGBM.tgz#LightGBM \
  --cacheFile /tmp/data/distLightGBM/data#data \
  --output /tmp/lightGBM_output#output \
  --queue default \
  sh demo.sh

