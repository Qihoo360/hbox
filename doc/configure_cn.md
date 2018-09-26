## 系统配置参数

[**English Document**](./configure.md)

系统配置可在XLearning系统客户端`$XLEARNING_HOME/conf/xlearning-site.xml`中添加配置项进行默认值修改或在作业提交时通过`--conf`参数进行修改。

### 应用配置

配置名称 | 默认值 | 含义  
---------------- | --------------- | ---------------  
xlearning.am.memory | 1024 | AM申请所需内存大小，单位为MB  
xlearning.am.cores | 1 | AM申请所需CPU核数  
xlearning.worker.num | 1 | worker启动数目  
xlearning.worker.memory | 1024 | worker申请使用内存大小，单位为MB  
xlearning.worker.cores | 1 | worker申请使用CPU核数  
xlearning.ps.num | 0 | ps启动数目，默认作业不使用ParameterServer机制  
xlearning.ps.memory | 1024 | ps申请使用内存大小，默认单位为MB  
xlearning.ps.cores | 1 | ps申请使用CPU核数  
xlearning.app.queue | DEFAULT | 作业提交队列  
xlearning.app.priority | 3 | 作业优先级，级别0-5，分别对应DEFAULT、VERY\_LOW、LOW、NORMAL、HIGH、VERY\_HIGH  
xlearning.input.strategy | DOWNLOAD | 输入文件加载模式，目前主要有DOWNLOAD、STREAM、PLACEHOLDER  
xlearning.inputfile.rename | false | 输入文件下载至本地是否需要重命名，该选项只用于输入文件加载模式为DOWNLOAD时  
xlearning.stream.epoch | 1 | 输入文件加载次数，该选项只用于输入文件加载策略为STREAM时  
xlearning.input.stream.shuffle | false | 输入文件是否采用shuffle模式，该选项只用于输入文件加载模式为STREAM时  
xlearning.inputformat.class | org.apache.hadoop.mapred.TextInputFormat.class | STREAM模式下，输入文件inputformat类指定  
xlearning.inputformat.cache | false | stream epoch大于1时，是否采用缓存至本地文件的操作  
xlearning.inputformat.cachefile.name | inputformatCache.gz | inputformat缓存至本地的文件名称  
xlearning.inputformat.cachesize.limit | 100*1024 | inputformat缓存于本地的文件大小上限，单位为MB  
xlearning.output.local.dir | output | 输出文件本地默认路径，该选项只用于作业提交参数output未指定本地输出路径时  
xlearning.output.strategy | UPLOAD | 输出文件加载策略，目前主要有DOWNLOAD、STREAM  
xlearning.outputformat.class | TextMultiOutputFormat.class | STREAM模式下，输出文件outputformat类指定  
xlearning.interresult.dir | /interResult_ | 指定模型中间结果上传至HDFS子路径  
xlearning.interresult.upload.timeout | 30 * 60 * 1000 | 模型中间结果上传至HDFS超时时长设置，单位为毫秒
xlearning.tf.evaluator | false | TensorFlow类型分布式作业中，是否将最后一个worker视为evaluator角色，主要针对Estimator高级API



### Board服务配置  

配置名称 | 默认值 | 含义  
---------------- | --------------- | ---------------  
xlearning.tf.board.enable | true | Board服务是否开启  
xlearning.tf.board.worker.index | 0 | 指定开启Board服务所在的worker index  
xlearning.tf.board.log.dir | eventLog | 指定Board日志存放路径，默认为本地路径./eventLog  
xlearning.tf.board.history.dir | /tmp/XLearning/eventLog | 指定Board日志上传至HDFS路径  
xlearning.tf.board.reload.interval | 1 | 指定TensorBoard数据加载时间间隔，单位为秒  
xlearning.board.modelpb | "" | 指定VisualDL加载的模型文件  
xlearning.board.cache.timeout | 20 | 指定VisualDL缓存加载间隔，单位为秒  
xlearning.tf.board.path | tensorboard | 指定TensorBoard服务路径  
xlearning.board.path | visualDL | 指定VisualDL服务路径  



### 系统配置

配置名称 | 默认值 | 含义  
---------------- | --------------- | ---------------  
xlearning.container.extra.java.opts | "" | container进程额外JVM参数  
xlearning.allocate.interval | 1000 | AM获取RM分配container状态时间间隔，单位为毫秒  
xlearning.status.update.interval | 1000 | AM向RM汇报状态时间间隔，单位为毫秒  
xlearning.task.timeout | 5 * 60 * 1000 | container超时时长，单位为毫秒  
xlearning.task.timeout.check.interval | 3 * 1000 | container超时检查时间间隔，单位为毫秒  
xlearning.localresource.timeout | 5 * 60 * 1000 | container下载本地资源超时时长，单位为毫秒  
xlearning.messages.len.max | 1000 | 消息队列大小限制，单位为字节  
xlearning.execute.node.limit | 200 | 作业申请节点数目上限  
xlearning.staging.dir | /tmp/XLearning/staging | 作业本地资源上传至HDFS路径  
xlearning.cleanup.enable | true | 作业结束后，是否删除资源上传HDFS路径内容  
xlearning.container.maxFailures.rate | 0.5 | 作业允许container失败比例上限  
xlearning.download.file.retry | 3 | DOWNLOAD模式下，输入文件下载尝试次数  
xlearning.download.file.thread.nums | 10 | DOWNLOAD模式下，输入文件下载线程数  
xlearning.container.heartbeat.interval | 10 * 1000 | container向AM发送心跳时间间隔，单位为毫秒  
xlearning.container.heartbeat.retry | 3 | container发送心跳尝试次数  
xlearning.container.update.appstatus.interval | 3 * 1000 | container获取作业执行状态时间间隔，单位为毫秒  
xlearning.container.auto.create.output.dir | true | container是否自动创建本地输出路径  
xlearning.log.pull.interval | 10000 | client获取AM日志输出时间间隔，单位为毫秒  
xlearning.user.classpath.first | true | 是否优先加载用户自定义jar包  
xlearning.worker.mem.autoscale | 0.5 | 作业失败重试时，worker内存自动增长比例   
xlearning.ps.mem.autoscale | 0.2 | 作业失败重试时，ps内存自动增长比例   
xlearning.app.max.attempts | 1 | 作业执行次数，默认执行失败后不重试   
xlearning.report.container.status | true | client端打印container运行状态信息  
xlearning.env.maxlength | 102400 | container启动程序执行时，环境变量长度上限



### History配置  

配置名称 | 默认值 | 含义   
---------------- | --------------- | ---------------  
xlearning.history.log.dir | /tmp/XLearning/history | history日志存放所在hdfs地址  
xlearning.history.log.delete-monitor-time-interval | 24 * 60 * 60 * 1000 | history日志清理检测时间间隔，单位为毫秒  
xlearning.history.log.max-age-ms | 24 * 60 * 60 * 1000 | history日志保存时长，单位为毫秒  
xlearning.history.port | 10021 | history服务开放端口  
xlearning.history.address | 0.0.0.0:10021 | history服务开放地址  
xlearning.history.webapp.port | 19886 | history服务web应用开放端口  
xlearning.history.webapp.address | 0.0.0.0:19886 | history服务web应用开放地址  
xlearning.history.webapp.https.port | 19885 | history服务web应用https开放端口  
xlearning.history.webapp.https.address | 0.0.0.0:19885 | history服务web应用https开放地址  


