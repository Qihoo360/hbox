## 系统配置参数

[**English Document**](./configure.md)

系统配置可在Hbox系统客户端`$HBOX_HOME/conf/hbox-site.xml`中添加配置项进行默认值修改或在作业提交时通过`--conf`参数进行修改。

### 应用配置

配置名称 | 默认值 | 含义  
---------------- | --------------- | ---------------  
hbox.driver.memory | 2048 | AM申请所需内存大小，单位为MB
hbox.driver.cores | 1 | AM申请所需CPU核数
hbox.worker.num | 1 | worker启动数目  
hbox.worker.memory | 1024 | worker申请使用内存大小，单位为MB  
hbox.worker.cores | 1 | worker申请使用CPU核数  
hbox.chief.worker.memory | 1024 | chief worker申请使用的内存大小，主要针对TensorFlow作业中index为0时的特殊worker，单位为MB，默认与worker memory一致  
hbox.evaluator.worker.memory | 1024 | evaluator worker申请使用的内存大小，主要针对TensorFlow estimator作业中evaluator角色的内存分配使用，单位为MB，默认与worker memory一致  
hbox.ps.num | 0 | ps启动数目，默认作业不使用ParameterServer机制  
hbox.ps.memory | 1024 | ps申请使用内存大小，默认单位为MB  
hbox.ps.cores | 1 | ps申请使用CPU核数  
hbox.app.queue | DEFAULT | 作业提交队列  
hbox.app.priority | 3 | 作业优先级，级别0-5，分别对应DEFAULT、VERY\_LOW、LOW、NORMAL、HIGH、VERY\_HIGH  
hbox.input.strategy | DOWNLOAD | 输入文件加载模式，目前主要有DOWNLOAD、STREAM、PLACEHOLDER  
hbox.inputfile.rename | false | 输入文件下载至本地是否需要重命名，该选项只用于输入文件加载模式为DOWNLOAD时  
hbox.stream.epoch | 1 | 输入文件加载次数，该选项只用于输入文件加载策略为STREAM时  
hbox.input.stream.shuffle | false | 输入文件是否采用shuffle模式，该选项只用于输入文件加载模式为STREAM时  
hbox.inputformat.class | org.apache.hadoop.mapred.TextInputFormat.class | STREAM模式下，输入文件inputformat类指定  
hbox.inputformat.cache | false | stream epoch大于1时，是否采用缓存至本地文件的操作  
hbox.inputformat.cachefile.name | inputformatCache.gz | inputformat缓存至本地的文件名称  
hbox.inputformat.cachesize.limit | 100*1024 | inputformat缓存于本地的文件大小上限，单位为MB  
hbox.output.local.dir | output | 输出文件本地默认路径，该选项只用于作业提交参数output未指定本地输出路径时  
hbox.output.strategy | UPLOAD | 输出文件加载策略，目前主要有DOWNLOAD、STREAM  
hbox.outputformat.class | TextMultiOutputFormat.class | STREAM模式下，输出文件outputformat类指定  
hbox.interresult.dir | /interResult_ | 指定模型中间结果上传至HDFS子路径  
hbox.interresult.upload.timeout | 30 * 60 * 1000 | 模型中间结果上传至HDFS超时时长设置，单位为毫秒  
hbox.interresult.save.inc | false | 模型中间结果是否增量上传，默认为全部上传    
hbox.tf.evaluator | false | TensorFlow类型分布式作业中，是否将最后一个worker视为evaluator角色，主要针对Estimator高级API  
hbox.tf.distribution.strategy | false | 是否使用TensorFLow分布式策略方法，默认为false  



### Board服务配置  

配置名称 | 默认值 | 含义  
---------------- | --------------- | ---------------  
hbox.tf.board.enable | true | Board服务是否开启  
hbox.tf.board.worker.index | 0 | 指定开启Board服务所在的worker index  
hbox.tf.board.log.dir | eventLog | 指定Board日志存放路径，默认为本地路径./eventLog  
hbox.tf.board.history.dir | /tmp/hbox/eventLog | 指定Board日志上传至HDFS路径
hbox.tf.board.reload.interval | 1 | 指定TensorBoard数据加载时间间隔，单位为秒  
hbox.board.modelpb | "" | 指定VisualDL加载的模型文件  
hbox.board.cache.timeout | 20 | 指定VisualDL缓存加载间隔，单位为秒  
hbox.tf.board.path | tensorboard | 指定TensorBoard服务路径  
hbox.board.path | visualDL | 指定VisualDL服务路径  



### 系统配置

配置名称 | 默认值 | 含义  
---------------- | --------------- | ---------------  
hbox.container.extra.java.opts | "" | container进程额外JVM参数  
hbox.allocate.interval | 1000 | AM获取RM分配container状态时间间隔，单位为毫秒  
hbox.status.update.interval | 1000 | AM向RM汇报状态时间间隔，单位为毫秒  
hbox.task.timeout | 5 * 60 * 1000 | container超时时长，单位为毫秒  
hbox.task.timeout.check.interval | 3 * 1000 | container超时检查时间间隔，单位为毫秒  
hbox.localresource.timeout | 5 * 60 * 1000 | container下载本地资源超时时长，单位为毫秒  
hbox.messages.len.max | 1000 | 消息队列大小限制，单位为字节  
hbox.execute.node.limit | 200 | 作业申请节点数目上限  
hbox.staging.dir | /tmp/hbox/staging | 作业本地资源上传至HDFS路径
hbox.cleanup.enable | true | 作业结束后，是否删除资源上传HDFS路径内容  
hbox.container.maxFailures.rate | 0.5 | 作业允许container失败比例上限  
hbox.download.file.retry | 3 | DOWNLOAD模式下，输入文件下载尝试次数  
hbox.download.file.thread.nums | 10 | DOWNLOAD模式下，输入文件下载线程数  
hbox.upload.output.thread.nums | 10 | UPLOAD模式下，输出文件上传线程数  
hbox.container.heartbeat.interval | 10 * 1000 | container向AM发送心跳时间间隔，单位为毫秒  
hbox.container.heartbeat.retry | 3 | container发送心跳尝试次数  
hbox.container.update.appstatus.interval | 3 * 1000 | container获取作业执行状态时间间隔，单位为毫秒  
hbox.container.auto.create.output.dir | true | container是否自动创建本地输出路径  
hbox.log.pull.interval | 10000 | client获取AM日志输出时间间隔，单位为毫秒  
hbox.user.classpath.first | true | 是否优先加载用户自定义jar包  
hbox.worker.mem.autoscale | 0.5 | 作业失败重试时，worker内存自动增长比例   
hbox.ps.mem.autoscale | 0.2 | 作业失败重试时，ps内存自动增长比例   
hbox.app.max.attempts | 1 | 作业执行次数，默认执行失败后不重试   
hbox.report.container.status | true | client端打印container运行状态信息  
hbox.env.maxlength | 102400 | container启动程序执行时，环境变量长度上限  
hbox.am.env.[EnvironmentVariableName] | (none) | 用户自定义am环境变量，用户可通过定义多项来设置多个环境变量  
hbox.container.env.[EnvironmentVariableName] | (none) | 用户自定义container环境变量，用户可通过定义多项来设置多个环境变量  
hbox.am.nodeLabelExpression | (none) | 指定调度AM的yarn节点标签表达  
hbox.worker.nodeLabelExpression | (none) | 指定调度worker的yarn节点标签表达  
hbox.ps.nodeLabelExpression | (none) | 指定调度ps的yarn节点标签表达  



### History配置  

配置名称 | 默认值 | 含义   
---------------- | --------------- | ---------------  
hbox.history.log.dir | /tmp/hbox/history | history日志存放所在hdfs地址
hbox.history.log.delete-monitor-time-interval | 24 * 60 * 60 * 1000 | history日志清理检测时间间隔，单位为毫秒  
hbox.history.log.max-age-ms | 24 * 60 * 60 * 1000 | history日志保存时长，单位为毫秒  
hbox.history.port | 10021 | history服务开放端口  
hbox.history.address | 0.0.0.0:10021 | history服务开放地址  
hbox.history.webapp.port | 19886 | history服务web应用开放端口  
hbox.history.webapp.address | 0.0.0.0:19886 | history服务web应用开放地址  
hbox.history.webapp.https.port | 19885 | history服务web应用https开放端口  
hbox.history.webapp.https.address | 0.0.0.0:19885 | history服务web应用https开放地址  


### MPI使用配置

配置名称 | 默认值 | 含义   
---------------- | --------------- | ---------------  
hbox.mpi.install.dir | /usr/local/openmpi | openmpi安装路径  
hbox.mpi.extra.ld.library.path | (none) | openmpi额外所需的lib包路径  
hbox.mpi.container.update.status.retry | 3 | 更新作业状态重试次数  


### Docker使用配置

配置名称 | 默认值 | 含义   
---------------- | --------------- | ---------------  
hbox.container.type | yarn | container运行类型  
hbox.docker.registry.host | (none) | docker register地址  
hbox.docker.registry.port | (none) | docker register端口  
hbox.docker.image | (none) | docker镜像名称  
hbox.docker.worker.dir | /work | docker container工作目录  
