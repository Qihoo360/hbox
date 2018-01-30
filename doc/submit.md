## Submit Parameter

[**中文文档**](./submit_cn.md)

Using the command `$XLEARNING_HOME/bin/xl-submit` to submit the application to Cluster at the XLearning client. Please see the example in the part of [README Quick Start](../README.md). The following is more details of the parameter.

Property Name | Meaning  
---------------- | ---------------  
app-name | application name  
app-type | application type, default as the "XLearning", can set as "TensorFlow", "Caffe" according to the deeplearning framework
input | input file path in the format of "the HDFS path"#"local path"  
output | output file path in the format of "the HDFS path"#"local path"  
files | the required local files of the application
cacheArchive | the required compressed files in the HDFS path  
cacheFile | the required files in the HDFS path  
launch-cmd | execute command  
user-path | the append for the environment variable $PATH  
jars | the required jar files  
user-classpath-first | whether user's job jar should be the first one on class path or not, default as the configure of xlearning.user.classpath.first  
conf | set the configuration  
am-cores | number of cores to use for the AM process, default as the configure of xlearning.am.cores  
am-memory | amount of memory to use for the AM process (in MB)，default as the configure of xlearning.am.memory  
ps-num | number of ps containers to use for the application, default as the configure of xlearning.ps.num  
ps-cores | number of cores to use for the ps process, default as the configure of xlearning.ps.cores  
ps-memory | amount of memory to use for the ps process (in MB), default as the configure of xlearning.ps.memory  
worker-num | number of worker containers to use for the application, default as the configure of xlearning.worker.num  
worker-cores | number of cores to use for the worker process, default as the configure of xlearning.worker.cores  
worker-memory | amount of memory to use for the worker process(in MB), default as the configure of xlearning.worker.memory  
queue | the queue of application submitted to, default as the configure of xlearning.app.queue  
priority | the priority of application, default as the configure of xlearning.app.priority  
board-enable | whether to start the service of Board, default as the configure of xlearning.tf.board.enable  
board-index | specify the index of worker which start the Board, default as the configure of xlearning.tf.board.worker.index  
board-logdir | the directory save Board event log, default as the configure of xlearning.tf.board.log.dir  
board-reloadinterval | how often the backend should load more data of event log for tensorboard, default as the configure of xlearning.tf.board.reload.interval  
board-historydir | specify the HDFS path which the Board event log upload to, default as the configure of xlearning.tf.board.history.dir  
board-modelpb | model proto in ONNX format for VisualDL, default as the configure of xlearning.board.modelpb  
board-cacheTimeout | memory cache timeout duration in seconds for VisualDL，default as the configure of xlearning.board.cache.timeout  
input-strategy | the strategy of the input file, default as the configure of xlearning.input.strategy  
inRenameInputFile | whether to rename the download file when input-strategy is "DOWNLOAD", default as the configure of xlearning.inputfile.rename  
stream-epoch | specify the epoch num of the input file read when input-strategy is "STREAM", default as the configure of xlearning.stream.epoch  
inputformat | specify the class of the inputformat when input-strategy is "STREAM", default as the configure of xlearning.inputformat.class  
inputformat-shuffle | whether to shuffle the input splits when input-strategy is "STREAM", default as the configure of xlearning.input.stream.shuffle  
output-strategy | the strategy of the output file, default as the configure of xlearning.output.strategy 
outputformat | specify the class of outputformat when output-strategy is "STREAM", default as the configure of xlearning.outputformat.class  

