#!/usr/bin/python
# -*- coding:utf-8 -*-

import tensorflow as tf
import numpy as np
import os

def get_train(data_path, batch_size=32):
  for s in os.listdir(data_path):
    with open(data_path+"/"+s, "r") as f:
        for line in f:
          y, x = line.strip("\n").split(" ")
          y = int(y)
          x = list(map(float, x.split(",")))
          x = tf.convert_to_tensor(x)
          x = tf.reshape(x, (1, -1))
          yield (x, tf.one_hot(y, 2))


class trainData:
  def __init__(self, fileStr, batch_size):
    self.fileStr = os.listdir(fileStr)
    self.batch_size = batch_size
    self.batch = []
    self.cache = []
    self.count_batch = 0
    self.flag = 0
    self.inc = -1
    self.iter_num = 0

    for s in self.fileStr:
      f = open(fileStr+"/"+s, "r")
      for line in f:
        iter = (line.split(" ")[0], (np.asarray(line.split(" ")[1].split(","), dtype=np.float32)))
        if self.iter_num < self.batch_size:
          self.batch.append(iter)
          self.iter_num += 1
        else:
          self.cache.append(self.batch)
          self.count_batch += 1
          self.batch = [iter]
          self.iter_num = 1 
  
    if self.batch:
      supplement_batch = self.cache[0]
      supplement_count = self.batch_size-len(self.batch)
      self.batch.extend(supplement_batch[:supplement_count])
      self.cache.append(self.batch)
      self.count_batch += 1

  def nextBatch(self):
    if(self.inc + 1 == self.count_batch):
      self.inc = -1
    self.inc += 1
    return self.cache[self.inc]
  
  def batchCount(self):
    return self.count_batch

def oneHot(sLabel):
    arr = np.zeros(2,np.float32)
    arr[int(''.join(sLabel))] = 1.0
    return arr
