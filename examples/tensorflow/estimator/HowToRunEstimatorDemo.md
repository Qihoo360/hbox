# How to Run Estimator Demo 
## 第一步： 上传数据到HDFS
```bash
# hdfs dfs -put $XLEARNING_HOME/data/tensorflow/iris /tmp/data
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

> 因使用 Estimator API 进行 TensorFlow 分布式训练，单机和分布式的代码是一样的，
> 即单机的代码不需要修改即可在分布式集群上进行训练，所以 PS Server 同样需要加载
> 数据，故需要采用 HDFS 等共享文件作为输入文件和输出文件。 

> 另，本人刚参加工作还不到半年，可能代码写的比较 Low, 提交 pull request 只希望抛砖引玉，希望经过你们的修改后加入 TF_CONFIG 环境变量，以便兼容 Estimator API 的 TensorFlow 分布式训练。

