## Configuration

[**中文文档**](./configure_cn.md)

The default value of each configuration can be modified by setting the corresponding properties in the `$HBOX_HOME/conf/hbox-site.xml` at the Hbox client or the parameter of `--conf` when submitting the application.    

### Application Configuration

Property Name | Default | Meaning  
---------------- | --------------- | ---------------  
hbox.am.memory | 1024MB | amount of memory to use for the AM process  
hbox.am.cores | 1 | number of cores to use for the AM process  
hbox.worker.num | 1 | number of worker containers to use for the application  
hbox.worker.memory | 1024MB | amount of memory to use for the worker process  
hbox.worker.cores | 1 | number of cores to use for the worker process    
hbox.chief.worker.memory | 1024 | amount of memory for chief worker,especially for the index 0 worker of the TensorFlow application, default as the setting of the worker memory.   
hbox.evaluator.worker.memory | 1024 | amount of memory for evaluator worker, especially for the TensorFlow Estimator application, default as the setting of the worker memory.  
hbox.ps.num | 0 | number of ps containers to use for the application  
hbox.ps.memory | 1024MB | amount of memory to use for the ps process  
hbox.ps.cores | 1 | number of cores to use for the ps process   
hbox.app.queue | DEFAULT | the queue which application submitted to  
hbox.app.priority | 3 | the priority of the application, divided into level 0 to 5, corresponding to DEFAULT, VERY\_LOW, LOW, NORMAL, HIGH, VERY\_HIGH  
hbox.input.strategy | DOWNLOAD | loading strategy of input file, including DOWNLOAD, STREAM, PLACEHOLDER  
hbox.inputfile.rename | false | whether to rename the download file in the DOWNLOAD strategy of input file  
hbox.stream.epoch | 1 | the number of the input file loading in the STREAM strategy of input file  
hbox.input.stream.shuffle | false | whether to shuffle the input splits in the STREAM strategy of input file  
hbox.inputformat.class | org.apache.hadoop.mapred.TextInputFormat.class | which inputformat implementation to use in the STREAM strategy of input file   
hbox.inputformat.cache | false | whether cache the inputformat file to local when the stream epoch longer than 1  
hbox.inputformat.cachefile.name | inputformatCache.gz | the local cache file name for inputformat  
hbox.inputformat.cachesize.limit | 100*1024 | the limit size of the local cache file (in MB)   
hbox.output.local.dir | output | If the local output path is not specified, the local directory of the output file is the default value.  
hbox.output.strategy | UPLOAD | loading strategy of output file, including DOWNLOAD, STREAM  
hbox.outputformat.class | TextMultiOutputFormat.class | which outputformat implementation to use in the STREAM strategy of output file  
hbox.interresult.dir | /interResult_ | specify the HDFS subdirectory that the intermediate output file upload to  
hbox.interresult.upload.timeout | 30 * 60 * 1000 | upload timeout to save the intermediate output (in milliseconds)  
hbox.interresult.save.inc | false | increment upload the intermediate output file, default not (upload all output file each time)
hbox.tf.evaluator | false | whether to set the last worker as evaluator of the distributed TensorFlow job type for the estimator api  
hbox.tf.distribution.strategy | false | whether use the distribution strategy API for the TensorFlow, default as false  



### Board Service Configuration  

Property Name | Default | Meaning  
---------------- | --------------- | ---------------  
hbox.tf.board.enable | true | If set to false, Board service is not necessary  
hbox.tf.board.worker.index | 0 | the index of the worker which start the service of Board  
hbox.tf.board.log.dir | eventLog | the directory saving TensorBoard event log  
hbox.tf.board.history.dir | /tmp/Hbox/eventLog | specify the HDFS path which the TensorBoard event log upload to  
hbox.tf.board.reload.interval | 1 | how often the backend should load more data of event log (in seconds) for tensorboard  
hbox.board.modelpb | "" | model proto in ONNX format for VisualDL  
hbox.board.cache.timeout | 20 | memory cache timeout duration in seconds for VisualDL  
hbox.tf.board.path | tensorboard | the path of the tensorboard  
hbox.board.path | visualDL | the path of the visualDL  


### System Configuration

Property Name | Default | Meaning  
---------------- | --------------- | ---------------  
hbox.container.extra.java.opts | "" | A string of extra JVM options to pass to ApplicationMaster to launch container  
hbox.allocate.interval | 1000ms | interval between the AM get the container assigned state from RM  
hbox.status.update.interval | 1000ms | interval between the AM report the state to RM  
hbox.task.timeout | 5 * 60 * 1000 | communication timeout between the AM and container (in milliseconds) 
hbox.task.timeout.check.interval | 3 * 1000 | how often the AM check the timeout of the container (in milliseconds)  
hbox.localresource.timeout | 5 * 60 * 1000 | set the timeout of the download the localResources (in milliseconds)  
hbox.messages.len.max | 1000 | Maximum size (in bytes) of message queue  
hbox.execute.node.limit | 200 | Maximum number of nodes that application use  
hbox.staging.dir | /tmp/Hbox/staging | HDFS directory that application local resources upload to  
hbox.cleanup.enable | true | whether delete the resources after the application finished  
hbox.container.maxFailures.rate | 0.5 | maximum percentage of the failure containers   
hbox.download.file.retry | 3 | Maximum number of retries for the input file download when the strategy of input file is DOWNLOAD  
hbox.download.file.thread.nums | 10 | number of download threads of the input file in the strategy of DOWNLOAD  
hbox.upload.output.thread.nums | 10 | number of upload threads of the output file in the strategy of UPLOAD  
hbox.container.heartbeat.interval | 10 * 1000 | interval between each container to the AM (in milliseconds)  
hbox.container.heartbeat.retry | 3 | Maximum number of retries for the container send the heartbeat to the AM  
hbox.container.update.appstatus.interval | 3 * 1000 | how often the containers get the state of the application process (in milliseconds)  
hbox.container.auto.create.output.dir | true | If set to true, the containers create the local output path automatically  
hbox.log.pull.interval | 10000 | interval between the client get the log output of the AM (in milliseconds)  
hbox.user.classpath.first | true |  whether user job jar should be the first one on class path or not.  
hbox.worker.mem.autoscale | 0.5 | automatic memory scale ratio of worker when application retry after failed.   
hbox.ps.mem.autoscale | 0.2 | automatic memory scale ratio of ps when application retry after failed.   
hbox.app.max.attempts | 1 | the number of application attempts， default not retry after failed.   
hbox.report.container.status | true | whether the client report the status of the container.  
hbox.env.maxlength | 102400 | the maximum length of environment variable when container execute the user program.  
hbox.am.env.[EnvironmentVariableName] | (none) |  Add the environment variable specified by EnvironmentVariableName to the AM process. The user can specify multiple of these to set multiple environment variables.  
hbox.container.env.[EnvironmentVariableName] | (none) | Add the environment variable specified by EnvironmentVariableName to the Container process. The user can specify multiple of these to set multiple environment variables.  
hbox.am.nodeLabelExpression | (none) | A YARN node label expression that restricts the set of nodes AM will be scheduled on.  
hbox.worker.nodeLabelExpression | (none) | A YARN node label expression that restricts the set of nodes Worker will be scheduled on.  
hbox.ps.nodeLabelExpression | (none) | A YARN node label expression that restricts the set of nodes PS will be scheduled on.  



### History Configuration  

Property Name | Default | Meaning   
---------------- | --------------- | ---------------  
hbox.history.log.dir | /tmp/Hbox/history | the HDFS directory that saves the history log  
hbox.history.log.delete-monitor-time-interval | 24 * 60 * 60 * 1000 | set the time interval by which the application history logs will be checked to clean (in milliseconds)  
hbox.history.log.max-age-ms | 24 * 60 * 60 * 1000 | how long the history log can be saved (in milliseconds)  
hbox.history.port | 10021 | port for the history service  
hbox.history.address | 0.0.0.0:10021 | address for the history service  
hbox.history.webapp.port | 19886 | port for the history http web service  
hbox.history.webapp.address | 0.0.0.0:19886 | address for the history http web service  
hbox.history.webapp.https.port | 19885 | port for the history https web service  
hbox.history.webapp.https.address | 0.0.0.0:19885 | address for the history https web service  


### MPI Configuration

Property Name | Default | Meaning   
---------------- | --------------- | ---------------  
hbox.mpi.install.dir | /usr/local/openmpi | the installation path of the openmpi  
hbox.mpi.extra.ld.library.path | (none) | the extra library path that openmpi need  
hbox.mpi.container.update.status.retry | 3 | the retry times for the container status update  


### Docker Configuration

Property Name | Default | Meaning   
---------------- | --------------- | ---------------  
hbox.container.type | yarn | container running type  
hbox.docker.registry.host | (none) | docker register host  
hbox.docker.registry.port | (none) | docker register port  
hbox.docker.image | (none) | docker image name  
hbox.docker.worker.dir | /work | the work dir of the docker container  
