## XLearning常见问题
<br>
[**English Document**](./faq.md)
<br>
### 1. 如何使用自定义版本或与集群安装版本不一致的框架执行作业？  
在XLearning客户端提交作业时，可通过`--file`、`--cacheFile`或`--cacheArchive`指定框架对应版本、依赖库等文件，并根据需求在运行脚本中指定PYTHONPATH环境变量，如`export PYTHONPATH=./:$PYTHONPATH`。用户可以此来使用自己所需的框架版本或依赖库，而不受限于计算机器所提供的依赖环境。  
### 2. 如何查看作业执行进度？  
若用户需要查看作业执行进度，需要在执行程序中按照`"report:progress:<float type>"`格式向标准错误打印进度信息，XLearning客户端及Web界面可根据所接收信息进行展示。  
### 3. TensorFlow分布式作业如何设置ClusterSpec？  
XLearning通过环境变量 TF\_CLUSTER\_DEF 、 TF\_ROLE 、 TF\_INDEX 对应的将clusterSpec、job\_name、task\_index等信息传送给各container（PS或Worker），用户只需在TensorFlow分布式模式程序中，从环境变量中获取对应变量，从而完成ClusterSpec及role、index分配。例如：  

    import os
    import json
    cluster_def = json.loads(os.environ["TF_CLUSTER_DEF"])
    cluster = tf.train.ClusterSpec(cluster_def)
    job_name = os.environ["TF_ROLE"]
    task_index = int(os.environ["TF_INDEX"])

