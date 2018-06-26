## XLearning常见问题

[**English Document**](./faq.md)

### 1. 如何使用自定义版本或与集群安装版本不一致的框架执行作业？  
在XLearning客户端提交作业时，可通过`--file`、`--cacheFile`或`--cacheArchive`指定框架对应版本、依赖库等文件，并根据需求在运行脚本中指定PYTHONPATH环境变量，如`export PYTHONPATH=./:$PYTHONPATH`。用户可以此来使用自己所需的框架版本或依赖库，而不受限于计算机器所提供的依赖环境。  
例如，若集群未事先装有tensorflow模块，可利用cacheArchive参数特性进行配置，方法如下：  
- 进入本地tensorflow模块安装所在的目录，如：`/usr/lib/python2.7/site-packages/tensorflow/`  
- 将路径内的所有文件记性打包，如：` tar -zcvf  tensorflow.tgz ./* ` 
- 上传该压缩包至hdfs，如放置在hdfs的`/tmp/tensorflow.tgz`  
- xlearning提交脚本中，添加cacheArchive参数，如：  `--cacheArchive /tmp/tensorflow.tgz#tensorflow`  
- 在launch-cmd中所执行的脚本中，添加环境变量设置：`export PYTHONPATH=./:$PYTHONPATH`  

### 2. 如何查看作业执行进度？  
若用户需要查看作业执行进度，需要在执行程序中按照`"report:progress:<float type>"`格式向标准错误打印进度信息，XLearning客户端及Web界面可根据所接收信息进行展示。  

### 3. XLearning目前支持哪些分布式深度学习框架作业的提交，如何与单机模式区分？  
XLearning目前支持 TensorFlow、MXNet、XGBoost、LightGBM 学习框架的分布式模式作业提交，其中：  
- TensorFlow：作业类型需设置为 `TensorFlow` ，以申请的ps数目来区分单机与分布式模式；  
- MXNet：作业类型需设置为 `MXNet`，以是否申请ps数目来区分单机与分布式模式；  
- XGBoost： 分布式作业需设置作业类型为 `distxgboost` ；  
- LightGBM： 分布式作业需设置作业类型为 `distlightgbm` ；  


### 4. TensorFlow分布式作业如何设置ClusterSpec？  
XLearning通过环境变量 TF\_CLUSTER\_DEF 、 TF\_ROLE 、 TF\_INDEX 对应的将clusterSpec、job\_name、task\_index等信息传送给各container（PS或Worker），用户只需在TensorFlow分布式模式程序中，从环境变量中获取对应变量，从而完成ClusterSpec及role、index分配。例如：  

    import os
    import json
    cluster_def = json.loads(os.environ["TF_CLUSTER_DEF"])
    cluster = tf.train.ClusterSpec(cluster_def)
    job_name = os.environ["TF_ROLE"]
    task_index = int(os.environ["TF_INDEX"])

### 5. 作业提交后，出现报错信息：`java.lang.NoClassDefFoundError: org/apache/hadoop/mapred/JobConf`, 如何解决？   
默认 `yarn.application.classpath` 配置中未包含mapreduce相关的lib包，需要修改客户端的`yarn-site.xml`中添加，如：

    <property>
        <name>yarn.application.classpath</name>    
        <value>$HADOOP_CLIENT_CONF_DIR,$HADOOP_CONF_DIR,$HADOOP_COMMON_HOME/*,$HADOOP_COMMON_HOME/lib/*,$HADOOP_HDFS_HOME/*,$HADOOP_HDFS_HOME/lib/*,$HADOOP_YARN_HOME/*,$HADOOP_YARN_HOME/lib/*,$HADOOP_MAPRED_HOME/*,$HADOOP_MAPRED_HOME/lib/*</value>  
    </property>  


### 6. 示例中数据集来源于mnist  

### 7. LightGBM分布式作业如何获取指定机器数目和本地端口号？  
执行分布式LightGBM需要在用户程序的配置文件中指定机器数目和本地端口号，此处可以直接从环境变量中获取，因此用户需要在执行脚本里将对应参数写入到配置文件。注意，为避免多个container分配到同一台机器时会修改同份配置文件，需要复制conf文件到可执行脚本的当前目录，具体如下（详细代码可见$XLEARNING_HOME/examples/distLightGBM）：  

    cp train.conf train_real.conf
    chmod 777 train_real.conf
    echo "num_machines = $LIGHTGBM_NUM_MACHINE" >> train_real.conf
    echo "local_listen_port = $LIGHTGBM_LOCAL_LISTEN_PORT" >> train_real.conf
    ./LightGBM/lightgbm config=train_real.conf

   
此外用户还需要在程序配置文件中指定机器列表文件，XLearning命名为`lightGBMlist.txt`，会在每个worker的执行目录生成，用户在程序配置文件指定参数如下：  

    machine_list_file = lightGBMlist.txt
  

### 8. Tensorflow中，环境变量TF_CONFIG如何利用已知变量进行构建？
以Tensorflow Estimator分布式中，chief模式下的环境变量TF_CONFIG的构建为例（详细代码可见$XLEARNING_HOME/examples/tfEstimator），如下：  

    import os
    import json
    
    cluster = json.loads(os.environ["TF_CLUSTER_DEF"])
    task_index = int(os.environ["TF_INDEX"])
    task_type = os.environ["TF_ROLE"]

    # chief: worker 0 as chief, other worker index --
    tf_config = dict()
    worker_num = len(cluster["wroker"])
    if task_type == "ps":
	  tf_config["task"] = {"index":task_index, "type":task_type}
    else:
	  if taks_index == 0:
	    tf_config["task"] = {"index":0, "type":"chief"}
	  else:
		tf_config["task"] = {"index":task_index-1, "type":task_type}

    if worker_num == 1:
	  cluster["chief"] = cluster["worker"]
	  del cluster["worker"]
    else:
	  cluster["chief"] = [cluster["worker"][0]]
	  del cluster["worker"][0]

    tf_config["cluster"] = cluster
    os.environ["TF_CONFIG"] = json.dumps(tf_config)

由此，可利用Tensorflow分布式模式下，XLearning提供的环境变量 TF\_CLUSTER\_DEF 、 TF\_ROLE 、 TF\_INDEX 对应的来构建所需的环境变量TF_CONFIG。  


### 9. Hadoop2.6.4以下版本如何使用查看作业执行占用的CPU内存负载信息功能？
因CPU Metrix功能中加载所需js文件是基于Hadoop中WebApp的build方法实现的，Hadoop2.6.4以下版本无该方法，若需要查看CPU Metrix的相关信息，可通过如下操作进行：  
- 在集群的 `hadoop-yarn-common-xxx.jar` 包中，添加所需资源，具体操作：  
1） 解压 hadoop-yarn-common-xxx.jar；  
2） 将 XLearning 代码中的 `src\main\resources\xlWebApp` 文件夹拷贝到`hadoop-yarn-common-xxx.jar`解压后路径中的 `webapps/static` 目录下；  
3）重新压缩jar包；  
4）为方便，可替换集群中hadoop-yarn-common-xxx.jar包，无需重启集群。（也可通过提交参数--jars来传递该jar包，优先使用此包启动container）  
- XLearning JobHistory 中查看CPU内存负载占用信息[可选]  
因XLearning JobHistory中加载的hadoop-yarn-common-xxx.jar包为XLearning-dist解压后`$XLEARNING_HOME/lib`下的jar文件，可按上述方法将所需js文件加载入jar包进行替换后，再启动JobHistory服务。  


### 10. 如何配置作业失败重试时内存自动扩充比例？  
XLearning1.1版本中支持作业失败重试，并且重试后作业worker与ps所申请的内存会自动扩充。用户可通过调整配置项来进行自定义：  
- xlearning.app.max.attempts  
- xlearning.worker.mem.autoscale  
- xlearning.ps.mem.autoscale  
注意：作业失败重试时，客户端报出的AM连接失败信息可忽略。 


### 11. 作业提交后，出现报错信息：java.io.IOException: Cannot run program "tensorboard": error=2, No such file or directory, 如何解决？  
在XLearning客户端提交作业时，添加 --user-path "/root/anaconda2/lib/python2.7/site-packages/tensorboard" ，指定tensorboard路径。   

### 12.若存在用户自定义module于其他python文件中，如何处理？    
利用files参数，添加所需要的所有python文件，在调用其他自定义模块前，将python文件所在路径添加至系统路径，如：sys.path.append(os.getcwd())。 


