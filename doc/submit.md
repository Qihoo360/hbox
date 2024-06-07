## Submit Parameter

[**中文文档**](./submit_cn.md)

Using the command `$HBOX_HOME/bin/hbox-submit [[--property value]...] command [argument...]` to submit the application to Cluster at the Hbox client.
Please see the example in the part of [README Quick Start](../README.md). The following is more details of the parameter.

Property Name | Meaning  
---------------- | ---------------  
app-name | application name  
app-type | application type, default as the "Hbox", can set as "TensorFlow", "Caffe" according to the deeplearning framework
input | input file path in the format of "the HDFS path"#"local path"  
output | output file path in the format of "the HDFS path"#"local path"  
files | the required local files of the application
cacheArchive | the required compressed files in the HDFS path  
cacheFile | the required files in the HDFS path  
user-path | the append for the environment variable $PATH  
jars | the required jar files  
user-classpath-first | whether user job jar should be the first one on class path or not, default as the configure of hbox.user.classpath.first  
conf | set the configuration  
driver-cores | number of cores to use for the AM process, default as the configure of hbox.driver.cores
driver-memory | amount of memory to use for the AM process (in MB)，default as the configure of hbox.driver.memory
ps-num | number of ps containers to use for the application, default as the configure of hbox.ps.num  
ps-cores | number of cores to use for the ps process, default as the configure of hbox.ps.cores  
ps-memory | amount of memory to use for the ps process (in MB), default as the configure of hbox.ps.memory  
worker-num | number of worker containers to use for the application, default as the configure of hbox.worker.num  
worker-cores | number of cores to use for the worker process, default as the configure of hbox.worker.cores  
worker-memory | amount of memory to use for the worker process(in MB), default as the configure of hbox.worker.memory  
chiefworker-memory | amount of memory for the chief worker, especially for the index 0 worker of the TensorFlow application, default as the worker-memory  
evaluatorworker-memory | amount of memory for the estimator worker, especially for the TensorFlow Estimator application, default as the worker-memory  
queue | the queue of application submitted to, default as the configure of hbox.app.queue  
priority | the priority of application, default as the configure of hbox.app.priority  
board-enable | whether to start the service of Board, default as the configure of hbox.tf.board.enable  
board-index | specify the index of worker which start the Board, default as the configure of hbox.tf.board.worker.index  
board-logdir | the directory save Board event log, default as the configure of hbox.tf.board.log.dir  
board-reloadinterval | how often the backend should load more data of event log for tensorboard, default as the configure of hbox.tf.board.reload.interval  
board-historydir | specify the HDFS path which the Board event log upload to, default as the configure of hbox.tf.board.history.dir  
board-modelpb | model proto in ONNX format for VisualDL, default as the configure of hbox.board.modelpb  
board-cacheTimeout | memory cache timeout duration in seconds for VisualDL，default as the configure of hbox.board.cache.timeout  
input-strategy | the strategy of the input file, default as the configure of hbox.input.strategy  
inRenameInputFile | whether to rename the download file when input-strategy is "DOWNLOAD", default as the configure of hbox.inputfile.rename  
stream-epoch | specify the epoch num of the input file read when input-strategy is "STREAM", default as the configure of hbox.stream.epoch  
inputformat | specify the class of the inputformat when input-strategy is "STREAM", default as the configure of hbox.inputformat.class  
inputformat-shuffle | whether to shuffle the input splits when input-strategy is "STREAM", default as the configure of hbox.input.stream.shuffle  
output-strategy | the strategy of the output file, default as the configure of hbox.output.strategy 
outputformat | specify the class of outputformat when output-strategy is "STREAM", default as the configure of hbox.outputformat.class
tf-evaluator | whether to set the last worker as evaluator of distributed TensorFlow job type, default as the configure of hbox.tf.evaluator
output-index | specify the index of the worker which to upload the output, default upload the output of all the workers.
archiveFiles | Location of local archive files will be uploaded to container and be decompressed. use comma as separator, # with alias name 
