## FAQ

[**中文文档**](./faq_cn.md)

### 1. How to use the custom version framework or the different version of the cluster installation to execute the program?  
Specify the required files for the related version of framework and dependent libraries by the submit parameters such as `--file`,`--cacheFile` or `--cacheArchive`. Furthermore, setting the environment variables `PYTHONPATH` as `export PYTHONPATH=./:$PYTHONPATH` if necessary.

### 2. How to view the progress of the execution?
In order to view the progress of the execution both at the XLearning client and the application web interface, user need to print the progress to standard error as the format of `"report:progress:<float type>"` in the execution program.  

### 3. How to define the ClusterSpec in the distributed mode of TensorFlow application ?  
In the distributed mode of TensorFlow application, ClusterSpec is defined by setting the host and port of ps and worker preliminarily. XLearning implements the automatic construction of the ClusterSpec. User can get the information of ClusterSpec, job\_name, task\_index from the environment variables TF\_CLUSTER\_DEF, TF\_ROLE, TF\_INDEX, such as:  

    import os
    import json
    cluster_def = json.loads(os.environ["TF_CLUSTER_DEF"])
    cluster = tf.train.ClusterSpec(cluster_def)
    job_name = os.environ["TF_ROLE"]
    task_index = int(os.environ["TF_INDEX"])


### 4. Report the error：" java.lang.NoClassDefFoundError: org/apache/hadoop/mapred/JobConf" after submit the application.       
Default that the "yarn.application.classpath" setted in the "yarn-site.xml" not contains the related lib package about mapreduce，try to add the related lib path, such:    

    <property>
        <name>yarn.application.classpath</name>    
        <value>$HADOOP_CLIENT_CONF_DIR,$HADOOP_CONF_DIR,$HADOOP_COMMON_HOME/*,$HADOOP_COMMON_HOME/lib/*,$HADOOP_HDFS_HOME/*,$HADOOP_HDFS_HOME/lib/*,$HADOOP_YARN_HOME/*,$HADOOP_YARN_HOME/lib/*,$HADOOP_MAPRED_HOME/*,$HADOOP_MAPRED_HOME/lib/*</value>  
    </property>  


### 5. How to use the "--cacheArchive" to upload the python module package such as tensorflow？   
For example, if there is not the tensorflow module on the node of the cluster, user can set the module by the "--cacheArchive". More detail:  
- In the local path that tensorflow module installed, such "/usr/lib/python2.7/site-packages/tensorflow/"  
- under the directory of the module, package all files in the directory,like " tar -zcvf  tensorflow.tgz ./*"    
- upload the package to hdfs   
- add the cacheArchive on the submit script, such "--cacheArchive /tmp/tensorflow.tgz#tensorflow"   
- set the environment variable for program: "export PYTHONPATH=./:$PYTHONPATH"   


### 6. The mnist data set used in the example. 

### 7. Report the error：" java.io.IOException: Cannot run program "tensorboard": error=2, No such file or directory" after submit the application.       
When the XLearning client submits a job, the --user-path "/root/anaconda2/lib/python2.7/site-packages/tensorboard" is added to specify the tensorboard path.

