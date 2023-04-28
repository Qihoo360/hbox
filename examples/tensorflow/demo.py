#!/usr/bin/python
# -*- coding:utf-8 -*-
import sys, os
print("lixiang pwd>>>", os.getcwd())
sys.path.append(os.getcwd())
sys.path.append("./anaconda3")
sys.path.append("./anaconda3/bin")
sys.path.append("./anaconda3/lib")
sys.path.append("./anaconda3/lib/python3.8/")
sys.path.append("./anaconda3/lib/python3.8/site-packages/")

print("lixiang ./>>>", os.listdir("./"))

import json
import time

import tensorflow as tf
import numpy as np
from dataDeal import oneHot
from dataDeal import trainData, get_train
#import args as argparse
import argparse
FLAGS = None

import os
import json
import argparse

import tensorflow as tf
from tensorflow.keras import datasets
from tensorflow.keras import layers, models
from tensorflow.keras import optimizers


def set_strategy():
    FLAGS.task_index = int(os.environ["TF_INDEX"])
    FLAGS.job_name = os.environ["TF_ROLE"]
    cluster = json.loads(os.environ["TF_CLUSTER_DEF"])
    print("cluster>>>", cluster)

    os.environ['GRPC_POLL_STRATEGY'] = "poll"
    os.environ["TF_CONFIG"] = json.dumps({
        'cluster': {
            'worker': cluster['worker'],
            'ps': cluster['ps']
        },
        'task': {'type': FLAGS.job_name, 'index': FLAGS.task_index}
    })
    print("TF_CONFIG>>>", os.environ["TF_CONFIG"])

    strategy = tf.distribute.experimental.MultiWorkerMirroredStrategy()

    return strategy


def build_model():
  model = tf.keras.Sequential([
      tf.keras.layers.Dense(32, activation='relu'),
      tf.keras.layers.Dense(32, activation='relu'),
      tf.keras.layers.Dense(1, activation='softmax')
  ])
  model.compile(optimizer=tf.keras.optimizers.Adam(0.001),
              loss=tf.losses.BinaryCrossentropy(),
              metrics=[tf.metrics.Accuracy(), tf.metrics.AUC()])
  return model

def main():
  FLAGS.task_index = int(os.environ["TF_INDEX"])
  FLAGS.job_name = os.environ["TF_ROLE"]
  cluster_def = json.loads(os.environ["TF_CLUSTER_DEF"])
  cluster = tf.train.ClusterSpec(cluster_def)

  print("ClusterSpec>>>:", cluster_def)
  print("current task id:", FLAGS.task_index, " role:", FLAGS.job_name)
  strategy = set_strategy()

  if FLAGS.job_name == 'ps':
    print("server>>>0")
    # server = tf.distribute.Server(cluster, job_name=FLAGS.job_name,task_index=FLAGS.task_index,config = tf.compat.v1.ConfigProto())
    # print("server def>>>1", server.server_def)
    # server.join()
  elif FLAGS.job_name == 'worker':
    dist_dataset = get_train(FLAGS.data_path, batch_size = FLAGS.batch_size)
    # dist_dataset = trainData(FLAGS.data_path, FLAGS.batch_size)
    
    with tf.device("/job:worker/task:%d"%(FLAGS.task_index)):
      with strategy.scope():
        # 模型的建立/编译需要在 `strategy.scope()` 内部。
        model = tf.keras.Sequential([
            tf.keras.layers.Dense(32, activation='relu'),
            tf.keras.layers.Dense(2, activation='softmax')
        ])
        model.compile(optimizer='adam',
                  loss=tf.losses.BinaryCrossentropy(),
                      metrics=[tf.metrics.Accuracy(), tf.metrics.AUC()])

      # Keras 的 `model.fit()` 以特定的时期数和每时期的步数训练模型。
      # 注意此处的数量仅用于演示目的，并不足以产生高质量的模型。
      for _ in range(3):
        for x, y in dist_dataset:
          model.fit(x, y)
      model.summary()

      
      print("TrainCompleted>>>", FLAGS.job_name, FLAGS.task_index)
      if(FLAGS.task_index == 0):
        print("saving model...", FLAGS.job_name, FLAGS.task_index)
        model.save(FLAGS.save_path+"/my_model.h5")
      print("trainDone>>>")   
    print(FLAGS.job_name, FLAGS.task_index, "weights>>>", model.weights)


if __name__ == "__main__":
  parser = argparse.ArgumentParser()
  parser.register("type", "bool", lambda v: v.lower() == "true")
  # Flags for defining the tf.train.ClusterSpec
  parser.add_argument(
    "--job_name",
    type=str,
    default="",
    help="One of 'ps', 'worker'"
  )
  # Flags for defining the tf.train.Server
  parser.add_argument(
    "--task_index",
    type=int,
    default=0,
    help="Index of task within the job"
  )
  # Flags for defining the parameter of data path
  parser.add_argument(
    "--data_path",
    type=str,
    default="",
    help="The path for train file"
  )
  parser.add_argument(
    "--save_path",
    type=str,
    default="",
    help="The save path for model"
  )
  parser.add_argument(
    "--log_dir",
    type=str,
    default="",
    help="The log path for model"
  )
  # Flags for defining the parameter of train
  parser.add_argument(
    "--learning_rate",
    type=float,
    default=0.001,
    help="learning rate of the train"
  )
  parser.add_argument(
    "--training_epochs",
    type=int,
    default=5,
    help="the epoch of the train"
  )
  parser.add_argument(
    "--batch_size",
    type=int,
    default=1200,
    help="The size of one batch"
  )

  FLAGS, unparsed = parser.parse_known_args()
  # tf.app.run(main=main)
  
  main()

  print("lixiang>>> train done")
