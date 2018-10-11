## Configuration

[**中文文档**](./configure_cn.md)

The default value of each configuration can be modified by setting the corresponding properties in the `$XLEARNING_HOME/conf/xlearning-site.xml` at the XLearning client or the parameter of `--conf` when submitting the application.    

### Application Configuration

Property Name | Default | Meaning  
---------------- | --------------- | ---------------  
xlearning.am.memory | 1024MB | amount of memory to use for the AM process  
xlearning.am.cores | 1 | number of cores to use for the AM process  
xlearning.worker.num | 1 | number of worker containers to use for the application  
xlearning.worker.memory | 1024MB | amount of memory to use for the worker process  
xlearning.worker.cores | 1 | number of cores to use for the worker process
xlearning.worker.gcores | 0 | number of gpu to use for the worker process
xlearning.ps.num | 0 | number of ps containers to use for the application  
xlearning.ps.memory | 1024MB | amount of memory to use for the ps process  
xlearning.ps.cores | 1 | number of cores to use for the ps process
xlearning.ps.gcores | 0 | number of gpu to use for the ps process
xlearning.app.queue | DEFAULT | the queue which application submitted to  
xlearning.app.priority | 3 | the priority of the application, divided into level 0 to 5, corresponding to DEFAULT, VERY\_LOW, LOW, NORMAL, HIGH, VERY\_HIGH  
xlearning.input.strategy | DOWNLOAD | loading strategy of input file, including DOWNLOAD, STREAM, PLACEHOLDER  
xlearning.inputfile.rename | false | whether to rename the download file in the DOWNLOAD strategy of input file  
xlearning.stream.epoch | 1 | the number of the input file loading in the STREAM strategy of input file  
xlearning.input.stream.shuffle | false | whether to shuffle the input splits in the STREAM strategy of input file  
xlearning.inputformat.class | org.apache.hadoop.mapred.TextInputFormat.class | which inputformat implementation to use in the STREAM strategy of input file   
xlearning.inputformat.cache | false | whether cache the inputformat file to local when the stream epoch longer than 1  
xlearning.inputformat.cachefile.name | inputformatCache.gz | the local cache file name for inputformat  
xlearning.inputformat.cachesize.limit | 100*1024 | the limit size of the local cache file (in MB)   
xlearning.output.local.dir | output | If the local output path is not specified, the local directory of the output file is the default value.  
xlearning.output.strategy | UPLOAD | loading strategy of output file, including DOWNLOAD, STREAM  
xlearning.outputformat.class | TextMultiOutputFormat.class | which outputformat implementation to use in the STREAM strategy of output file  
xlearning.interresult.dir | /interResult_ | specify the HDFS subdirectory that the intermediate output file upload to  
xlearning.interresult.upload.timeout | 30 * 60 * 1000 | upload timeout to save the intermediate output (in milliseconds)
xlearning.tf.evaluator | false | whether to set the last worker as evaluator of the distributed TensorFlow job type for the estimator api
xlearning.am.env.[EnvironmentVariableName] | (none) |  Add the environment variable specified by EnvironmentVariableName to the AM process. The user can specify multiple of these to set multiple environment variables.
xlearning.container.env.[EnvironmentVariableName] | (none) | Add the environment variable specified by EnvironmentVariableName to the Container process. The user can specify multiple of these to set multiple environment variables.



### Board Service Configuration  

Property Name | Default | Meaning  
---------------- | --------------- | ---------------  
xlearning.tf.board.enable | true | If set to false, Board service is not necessary  
xlearning.tf.board.worker.index | 0 | the index of the worker which start the service of Board  
xlearning.tf.board.log.dir | eventLog | the directory saving TensorBoard event log  
xlearning.tf.board.history.dir | /tmp/XLearning/eventLog | specify the HDFS path which the TensorBoard event log upload to  
xlearning.tf.board.reload.interval | 1 | how often the backend should load more data of event log (in seconds) for tensorboard  
xlearning.board.modelpb | "" | model proto in ONNX format for VisualDL  
xlearning.board.cache.timeout | 20 | memory cache timeout duration in seconds for VisualDL  
xlearning.tf.board.path | tensorboard | the path of the tensorboard  
xlearning.board.path | visualDL | the path of the visualDL  


### System Configuration

Property Name | Default | Meaning  
---------------- | --------------- | ---------------  
xlearning.container.extra.java.opts | "" | A string of extra JVM options to pass to ApplicationMaster to launch container  
xlearning.allocate.interval | 1000ms | interval between the AM get the container assigned state from RM  
xlearning.status.update.interval | 1000ms | interval between the AM report the state to RM  
xlearning.task.timeout | 5 * 60 * 1000 | communication timeout between the AM and container (in milliseconds) 
xlearning.task.timeout.check.interval | 3 * 1000 | how often the AM check the timeout of the container (in milliseconds)  
xlearning.localresource.timeout | 5 * 60 * 1000 | set the timeout of the download the localResources (in milliseconds)  
xlearning.messages.len.max | 1000 | Maximum size (in bytes) of message queue  
xlearning.execute.node.limit | 200 | Maximum number of nodes that application use  
xlearning.staging.dir | /tmp/XLearning/staging | HDFS directory that application local resources upload to  
xlearning.cleanup.enable | true | whether delete the resources after the application finished  
xlearning.container.maxFailures.rate | 0.5 | maximum percentage of the failure containers   
xlearning.download.file.retry | 3 | Maximum number of retries for the input file download when the strategy of input file is DOWNLOAD  
xlearning.download.file.thread.nums | 10 | number of download threads of the input file in the strategy of DOWNLOAD
xlearning.container.heartbeat.interval | 10 * 1000 | interval between each container to the AM (in milliseconds)  
xlearning.container.heartbeat.retry | 3 | Maximum number of retries for the container send the heartbeat to the AM  
xlearning.container.update.appstatus.interval | 3 * 1000 | how often the containers get the state of the application process (in milliseconds)  
xlearning.container.auto.create.output.dir | true | If set to true, the containers create the local output path automatically  
xlearning.log.pull.interval | 10000 | interval between the client get the log output of the AM (in milliseconds)  
xlearning.user.classpath.first | true |  whether user job jar should be the first one on class path or not.  
xlearning.worker.mem.autoscale | 0.5 | automatic memory scale ratio of worker when application retry after failed.   
xlearning.ps.mem.autoscale | 0.2 | automatic memory scale ratio of ps when application retry after failed.   
xlearning.app.max.attempts | 1 | the number of application attempts， default not retry after failed.   
xlearning.report.container.status | true | whether the client report the status of the container.  
xlearning.env.maxlength | 102400 | the maximum length of environment variable when container execute the user program.



### History Configuration  

Property Name | Default | Meaning   
---------------- | --------------- | ---------------  
xlearning.history.log.dir | /tmp/XLearning/history | the HDFS directory that saves the history log  
xlearning.history.log.delete-monitor-time-interval | 24 * 60 * 60 * 1000 | set the time interval by which the application history logs will be checked to clean (in milliseconds)  
xlearning.history.log.max-age-ms | 24 * 60 * 60 * 1000 | how long the history log can be saved (in milliseconds)  
xlearning.history.port | 10021 | port for the history service  
xlearning.history.address | 0.0.0.0:10021 | address for the history service  
xlearning.history.webapp.port | 19886 | port for the history http web service  
xlearning.history.webapp.address | 0.0.0.0:19886 | address for the history http web service  
xlearning.history.webapp.https.port | 19885 | port for the history https web service  
xlearning.history.webapp.https.address | 0.0.0.0:19885 | address for the history https web service  


