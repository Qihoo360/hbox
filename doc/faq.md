## FAQ

[**中文文档**](./faq_cn.md)

### 1. How to use the custom version framework or the different version of the cluster installation to execute the program?  
Specify the required files for the related version of framework and dependent libraries by the submit parameters such as `--file`,`--cacheFile` or `--cacheArchive`. Furthermore, setting the environment variables `PYTHONPATH` as `export PYTHONPATH=./:$PYTHONPATH` if necessary.  
For example, if there is not the tensorflow module on the node of the cluster, user can set the module by the `--cacheArchive`. More detail:   
- In the local path that tensorflow module installed, such `/usr/lib/python2.7/site-packages/tensorflow/`  
- under the directory of the module, package all files in the directory,like ` tar -zcvf  tensorflow.tgz ./*`    
- upload the package to hdfs   
- add the cacheArchive on the submit script, such `--cacheArchive /tmp/tensorflow.tgz#tensorflow`   
- set the environment variable for program: `export PYTHONPATH=./:$PYTHONPATH`  

### 2. How to view the progress of the execution?
In order to view the progress of the execution both at the XLearning client and the application web interface, user need to print the progress to standard error as the format of `"report:progress:<float type>"` in the execution program.  

### 3. What distributed deep learning frameworks XLearning supports, how to distinguish between standalone mode ?    
XLearning support the distributed deep learning framworks such as TensorFlow, MXNet, XGBoost, LightGBM.  
- TensorFlow: Set the `--app-type` as `TensorFlow`, and distinguish stand-alone and distributed mode by the number of ps applied.   
- MXNet：Set the `--app-type` as `MXNet`, and distinguish stand-alone and distributed mode by the number of ps applied.   
- XGBoost： Set the `--app-type` as `distxgboost`.  
- LightGBM： Set the `--app-type` as `distlightgbm`.
- LightLDA： Set the `--app-type` as `lightlda`,  and distinguish stand-alone and distributed mode by the number of ps applied.
- XFlow： Set the `--app-type` as `XFlow`, and distinguish stand-alone and distributed mode by the number of ps applied.

### 4. How to define the ClusterSpec in the distributed mode of TensorFlow application ?  
In the distributed mode of TensorFlow application, ClusterSpec is defined by setting the host and port of ps and worker preliminarily. XLearning implements the automatic construction of the ClusterSpec. User can get the information of ClusterSpec, job\_name, task\_index from the environment variables TF\_CLUSTER\_DEF, TF\_ROLE, TF\_INDEX, such as:  

    import os
    import json
    cluster_def = json.loads(os.environ["TF_CLUSTER_DEF"])
    cluster = tf.train.ClusterSpec(cluster_def)
    job_name = os.environ["TF_ROLE"]
    task_index = int(os.environ["TF_INDEX"])


### 5. Report the error：`" java.lang.NoClassDefFoundError: org/apache/hadoop/mapred/JobConf"` after submit the application.       
Default that the `yarn.application.classpath` setted in the `yarn-site.xml` not contains the related lib package about mapreduce，try to add the related lib path, such:    

    <property>
        <name>yarn.application.classpath</name>    
        <value>$HADOOP_CLIENT_CONF_DIR,$HADOOP_CONF_DIR,$HADOOP_COMMON_HOME/*,$HADOOP_COMMON_HOME/lib/*,$HADOOP_HDFS_HOME/*,$HADOOP_HDFS_HOME/lib/*,$HADOOP_YARN_HOME/*,$HADOOP_YARN_HOME/lib/*,$HADOOP_MAPRED_HOME/*,$HADOOP_MAPRED_HOME/lib/*</value>  
    </property>  


### 6. The mnist data set used in the example.  

### 7. How to set the number of the machines and local port on the distribute LightGBM application ?  
User can get the information of the number of machines and local port from the environment variables and write into the configuration file on the distribute LightGBM application (More details in $XLEARNING_HOME/examples/distLightGBM ). Note that it is necessary to copy the configuration file at current directory to avoid the more containers modify the same file when they are at one machine, like :   

    cp train.conf train_real.conf
    chmod 777 train_real.conf
    echo "num_machines = $LIGHTGBM_NUM_MACHINE" >> train_real.conf
    echo "local_listen_port = $LIGHTGBM_LOCAL_LISTEN_PORT" >> train_real.conf
    ./LightGBM/lightgbm config=train_real.conf


Also, user need to set the machine list file at the configuration which XLearning named as `lightGBMlist.txt` generated at the executive directory of each worker, like :  

    machine_list_file = lightGBMlist.txt


### 8. How to set the environment varibale TF_CONFIG in the Tensorflow application ?  
Example of TF_CONFIG for chief training in the distributed mode of Tensorflow estimator application (More details in $XLEARNING_HOME/examples/tfEstimator):    

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
    elif task_type == "worker":
	  if taks_index == 0:
	    tf_config["task"] = {"index":0, "type":"chief"}
	  else:
		tf_config["task"] = {"index":task_index-1, "type":task_type}
	elif task_type == "evaluator":
    	  tf_config["task"] = {"index":task_index, "type":task_type}

    if worker_num == 1:
	  cluster["chief"] = cluster["worker"]
	  del cluster["worker"]
    else:
	  cluster["chief"] = [cluster["worker"][0]]
	  del cluster["worker"][0]

    tf_config["cluster"] = cluster
    os.environ["TF_CONFIG"] = json.dumps(tf_config)
  

### 9. How to display the cpu metrics when the hadoop version is lower than 2.6.4 ?   

Because of loading the required js files for CPU Metrix functionality is based on the WebApp build method which is not achieved in the hadoop version lower than 2.6.4, there is the other method to display the cpu metrics if necessary.   
- add the necessary resources to the package of `hadoop-yarn-common-xxx.jar` on the cluster, more details:  
1) unpackage the hadoop-yarn-common-xxx.jar  
2) copy the folder `src\main\resources\xlWebApp` at the source code of XLearning to the directory `webapps/static` which generated after unpackaging the `hadoop-yarn-common-xxx.jar`  
3) re-package hadoop-yarn-common-xxx.jar  
4) replace the hadoop-yarn-common-xxx.jar on the cluster without restart the nodemanager and resource manager. Also can use the `--jars` to load the jar when submit the application.  
- display the cpu matrix in the XLearning JobHistory [Optional]  
XLearning JobHistory is relay on the jars in the directory `$XLEARNING_HOME/lib` which generated after unpackaging the XLearning dist. Follow the above method to replace the hadoop-yarn-common-xxx.jar, then restart service.  


### 10. How to set the memory scale ratio when the application retry after failed ?  
XLearning1.1 support the application retry and memory auto scaled after failed by setting the configuration:  
- xlearning.app.max.attempts  
- xlearning.worker.mem.autoscale  
- xlearning.ps.mem.autoscale  
Note that the information of AM connected error which reported at the client when application retry can ignore. 

### 11. Report the error：" java.io.IOException: Cannot run program "tensorboard": error=2, No such file or directory" after submit the application.       
When the XLearning client submits a job, the --user-path "/root/anaconda2/lib/python2.7/site-packages/tensorboard" is added to specify the tensorboard path.

### 12. How to get the input file list for each worker container when setting the `--conf xlearning.input.strategy` or `--input-strategy` as `PLACEHOLDER` ?
With the input strategy setted as the `PLACEHOLDER`, worker containers get the assigned input file list to the program by the way of the environment `INPUT_FILE_LIST` as `json` format with the `key` of the input local path and the `value` of the list of the hdfs file name. However, there is the error when the length of the environment is too long to execute the user program. In this situation, the content of the environment `INPUT_FILE_LIST` would be written to the local file `inputFileList.txt` at the current path.
User can get the file list like this:

    import os
    import json
    if os.environ.has_key('INPUT_FILE_LIST') :
      inputfile = json.loads(os.environ["INPUT_FILE_LIST"])
      data_file = inputfile["data"]
    else :
      with open("inputFileList.txt") as f:
        fileStr = f.readline()
      inputfile = json.loads(fileStr)


### 13. How to use the custom module ?
Upload the files using the `--files` to each container. Load the related path to the system path, such as `sys.path.append(os.getcwd())` before calling the module.  

### 14. Recommended to directly operate the hdfs data for input and model output if use the TensorFlow Estimator advanced API.  
