$HBOX_HOME/bin/hbox-submit \
   --app-type "distxgboost" \
   --worker-memory 20G \
   --worker-num 2 \
   --files  mushroom.yarn.conf,demo.sh\
   --cacheArchive /tmp/data/distxgboost/xgboost.tgz#xgboost \
   --input /tmp/data/distxgboost/train#train \
   --input /tmp/data/distxgboost/test#test \
   --output /tmp/xgboost_dist_output#model \
   --app-name "distxgboost_demo" \
   --queue default \
   --conf hbox.container.env.PYTHONPATH=xgboost/python-package \
   sh demo.sh
