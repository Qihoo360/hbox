#!/bin/bash
export LD_LIBRARY_PATH=$HADOOP_HOME/lib/native:$LD_LIBRARY_PATH
export PYTHONPATH=xgboost/python-package:$PYTHONPATH
xgboost/xgboost mushroom.yarn.conf nthread=2 model_dir=model

