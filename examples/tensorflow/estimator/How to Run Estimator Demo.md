# How to Run Estimator Demo 
## 第一步： 上传数据到HDFS
```bash
# hdfs dfs -put $XLEARNING_HOME/data/tensorflow/tensorflow/iris /tmp/data
``` 
## 第二步： 修改 `runEstimatorDemo.sh` 中的 IP
```bash
# cd $XLEARNING_HOME/examples/tensorflow/estimator
# vi runEstimatorDemo.sh
```
```bash
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
   --queue default
```
将localhost修改为自己的 Hadoop IP

## 第三步： 执行

```bash
# sh runEstimatorDemo.sh
```

## 第四步： tensorboard

[How to run TensorFlow on Hadoop](https://tensorflow.google.cn/deploy/hadoop)

```bash
# tensorboard --logdir=hdfs://localhost:9000/tmp/model/estimatorDemo
```

