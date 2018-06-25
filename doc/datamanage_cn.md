##基于HDFS的统一数据管理

XLearning提供有基于HDFS的统一数据管理，概述说明见[README功能特性](../README_CN.md)部分，本文对该功能特性展开详细使用说明，方便大家理解应用。  

###输入数据读取方式  
####1. Download  
该模式下，遍历用户指定的HDFS路径下所有文件，并以**文件**为单位将输入数据平均分配给各Worker，Worker在执行程序启动前，将所分配到的数据文件从HDFS下载到本地指定路径。XLearning默认采用Download模式对输入数据进行处理。详细使用示例说明如下：  
XLearning作业提交脚本中，指定参数：  
    
     --input /tmp/input#data
     
在此参数示例中，Xlearning将遍历HDFS路径/tmp/input文件夹下的所有文件，根据Worker数目，将文件均匀分配给各Worker，各Worker下载所分配文件至./data（“#”后所指定名称）路径下。由此，用户可在程序中从本地（./data）操作输入文件。  
扩展使用示例：   

    a. 不同HDFS文件下载至同一本地路径，如 输入文件为/tmp/input1/file1、/tmp/input2/file2，均对应本地路径./data，  
    1) 将所需的不同HDFS文件以“,”分隔传入input参数，最后以"#"来拼接所指定的本地路径名  
       --input /tmp/input1/file1,/tmp/input2/file2#data \  
    2) 指定多个input参数，分别将HDFS文件对应本地目录  
       --input /tmp/input1/file1#data \  
       --input /tmp/input2/file2#data \  
    注意，根据Worker数目，若下载至同一本地目录下的HDFS文件存在重名现象，如：  
       --worker-num 1   
       --input /tmp/input1/file1,/tmp/input2/file1#data"  
    则会在Worker中下载不同HDFS目录下的"file1"文件下载至本地./data路径。该情况可通过设置提交参数"--isRenameInputFile true"，XLearning将根据当前时间对文件进行重命名。（在此情况下，若用户程序存在指定文件名进行文件读取操作，可能需要进行修改）  
       
    b. 多个输入文件路径，如 分别将HDFS路径/tmp/input1、/tmp/input、/tmp/input3下载至本地路径data1、data2、data3，可通过多个input参数进行指定，示例如下：  
      --input /tmp/input1#data1 \  
      --input /tmp/input2#data2 \  
      --input /tmp/input3#data3 \  

####2. Placeholder  
该模式与Download类似，不同之处在于Worker不会直接下载HDFS文件到本地指定路径，而是将所分配的HDFS文件列表通过环境变量`INPUT_FILE_LIST`传给Worker中的执行程序对应进程。执行程序从环境变量`os.environ["INPUT_FILE_LIST"]`中获取需要处理的文件列表，直接对HDFS文件进行读写等操作。该模式要求深度学习框架具备读取HDFS文件的功能，或借助第三方模块库如pydoop等。TensorFlow在0.10版本后已经支持直接操作HDFS文件。使用示例说明：  

    XLearning作业提交脚本中，指定参数：  
      --input /tmp/input#data \  
      --input-strategy PLACEHOLDER \  
    在此参数示例中，XLearning将遍历HDFS路径/tmp/input文件夹下的所有文件，根据Worker数目，将文件均匀分配给各Worker，各Worker将所分配的文件列表通过环境变量"INPUT_FILE_LIST"传给各执行程序，执行程序可依赖第三方库或框架自身来对HDFS文件直接操作。  
    注意：环境变量"INPUT_FILE_LIST"为json格式，其中，key为"input"参数所指定的本地路径，value为所分配的HDFS文件列表（list类型）  
    根据上述作业提交脚本，输入文件列表获取使用示例如下：   
      import os  
      import json  
      inputfile = json.loads(os.environ["INPUT_FILE_LIST"])  
      data_file = inputfile["data"]  
      
    注意，若输入文件列表太大容易造成执行命令参数过长导致作业失败。此处，若环境变量"INPUT_FILE_LIST"不存在，则说明已超出参数长度上限，XLearning会将环境变量"INPUT_FILE_LIST"应传入的内容写入本地文件"inputFileList.txt"中，用户可从该文件中读取输入文件列表，文件内容仍为json格式，如：  
      with open("inputFileList.txt") as f:  
        fileStr = f.readline()  
      inputfile = json.loads(fileStr)  

####3. InputFormat  
该模式下，Worker从标准输入方式读取所分配到的数据分片信息，使用示例说明：  

    XLearning作业提交脚本中，指定参数：  
      --input /tmp/input \  
      --input-strategy STREAM \  
    在此参数示例中，XLearning将HDFS路径/tmp/input文件夹下的所有文件数据进行分片并分发给各Worker。Worker通过管道将数据传给执行程序进程，执行程序可通过标准输入方式进行数据读取，例如： 
    def main():  
      #read data from stdin  
      for line in sys.stdin:  
        #write result to stdout  
        sys.stdout.write(line + '\n')  
    

###输出数据保存方式
####1. Upload  
XLearning默认采用Upload模式对输出数据进行保存至HDFS操作，示例说明如下：  

    XLearning作业提交脚本中，指定参数：
       --output /tmp/output#model \
    在此参数示例中，执行程序进程结束后，将本地./model下的所有文件，上传至HDFS路径/tmp/output/${containerID}下，注意，此处${containerID}为Worker所属ContainerID，由XLearning自动分配。若需要多个输出路径，可通过设置多个" --output"参数实现。

为方便用户在训练过程中随时将本地输出上传至HDFS，XLearning在作业执行Web界面提供输出结果中间保存机制。用户作业执行过程中，根据训练进度及设置，将结果保存在`output`参数的对应的本地路径下，在执行中途需要提前将本地输出传至对应HDFS路径时，可在作业执行ApplicationMaster页面点击`Save Model`按钮进行中间输出结果上传。上传成功后，界面显示当前已上传的中间结果列表。  

####2. OutputFormat  
在训练过程中，Worker根据指定的OutputFormat类，将结果输出至HDFS，使用示例可参见InputFormat部分给出的程序示例。  

