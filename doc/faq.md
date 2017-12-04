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

