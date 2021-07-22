#!/bin/sh


python_path="./anaconda3/bin/python"
#python_path="./py2.7/bin/python"
#python_path="python"
$XLEARNING_HOME/bin/xl-submit \
    --app-type "tensorflow" \
    --app-name "tf-demo" \
    --input /data/tmp#data \
    --output ./tensorflow_model#model \
    --files demo.py,dataDeal.py \
    --launch-cmd "${python_path} demo.py --data_path=./data --save_path=./model --log_dir=./eventLog --training_epochs=10" \
    --worker-memory 25G \
    --worker-num 2 \
    --worker-cores 3 \
    --ps-memory 25G \
    --ps-num 1 \
    --ps-cores 2 \
	--queue hs2 \
    --cacheArchive "./anaconda3.tar.gz#anaconda3,./tensorflow2.2.0.tar.gz#tensorflow" 
