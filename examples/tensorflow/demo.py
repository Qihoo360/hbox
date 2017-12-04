import argparse
import sys
import os
import json
import numpy as np
import tensorflow as tf
import time

sys.path.append(os.getcwd())
from dataDeal import oneHot
from dataDeal import trainData

FLAGS = None

def main(_):
  # cluster specification
  FLAGS.task_index = int(os.environ["TF_INDEX"])
  FLAGS.job_name = os.environ["TF_ROLE"]
  cluster_def = json.loads(os.environ["TF_CLUSTER_DEF"])
  cluster = tf.train.ClusterSpec(cluster_def)

  print("ClusterSpec:", cluster_def)
  print("current task id:", FLAGS.task_index, " role:", FLAGS.job_name)
  
  gpu_options = tf.GPUOptions(allow_growth = True)
  server = tf.train.Server(cluster, job_name=FLAGS.job_name,task_index=FLAGS.task_index,config = tf.ConfigProto(gpu_options=gpu_options,allow_soft_placement = True))
  
  if FLAGS.job_name == "ps":
    server.join()
  elif FLAGS.job_name == "worker":
    # set the train parameters
    learning_rate = FLAGS.learning_rate
    training_epochs = FLAGS.training_epochs
    batch_size = FLAGS.batch_size
    iterData = trainData(FLAGS.data_path, batch_size)
    
    with tf.device(tf.train.replica_device_setter(worker_device=("/job:worker/task:%d"%(FLAGS.task_index)),cluster=cluster)):
      # count the number of updates
      global_step = tf.get_variable('global_step', [],initializer = tf.constant_initializer(0), trainable = False)
      # input 
      with tf.name_scope('input'):
        x = tf.placeholder(tf.float32, shape=[None, 100], name="x-input")
        y_ = tf.placeholder(tf.float32, shape=[None, 2], name="y-input")
      # model parameters
      tf.set_random_seed(1)
      with tf.name_scope("weights"):
        W1 = tf.Variable(tf.random_normal([100, 50]))
        W2 = tf.Variable(tf.random_normal([50, 2]))
      # bias
      with tf.name_scope("biases"):
        b1 = tf.Variable(tf.zeros([50]))
        b2 = tf.Variable(tf.zeros([2]))
      # implement model
      with tf.name_scope("softmax"):
        # y is our prediction
        z1 = tf.add(tf.matmul(x,W1),b1)
        a1 = tf.nn.softmax(z1)
        z2 = tf.add(tf.matmul(a1,W2),b2)
        y = tf.nn.softmax(z2)
      # specify cost function
      with tf.name_scope('cross_entropy'):
        # this is our cost
        cross_entropy = tf.reduce_mean(-tf.reduce_sum(y_ * tf.log(y), reduction_indices=[1]))
      # specify optimizer
      with tf.name_scope('train'):
        # optimizer is an "operation" which we can execute in a session
        grad_op = tf.train.GradientDescentOptimizer(learning_rate)
        train_op = grad_op.minimize(cross_entropy, global_step=global_step)
      # init_op
      tf.summary.scalar('cross_entropy', cross_entropy )
      merged = tf.summary.merge_all()
      init_op = tf.global_variables_initializer()
      saver = tf.train.Saver()
      print("Variables initialized ...")
    sv = tf.train.Supervisor(is_chief = (FLAGS.task_index == 0), global_step = global_step, init_op = init_op)
    with sv.prepare_or_wait_for_session(server.target, config = tf.ConfigProto(gpu_options=gpu_options, allow_soft_placement = True, log_device_placement = True)) as sess:
      # perform training cycles
      start_time = time.time()
      if(FLAGS.task_index == 0):
        train_writer = tf.summary.FileWriter(FLAGS.log_dir, sess.graph)
      for epoch in range(training_epochs):
        # number of batches in one epoch                
        sys.stderr.write("reporter progress:%0.4f\n"%(float(epoch)/(training_epochs)))
        totalStep = iterData.batchCount()
        for step in range(totalStep):
          iterator_curr = iterData.nextBatch()
          flag = 0
          for iter in iterator_curr:
            if 0 == flag:
                train_x = iter[1].reshape(1,100)
                train_y = oneHot(iter[0]).reshape(1,2)
            else:
                train_x = np.concatenate((train_x, iter[1].reshape(1,100)))
                train_y = np.concatenate((train_y, oneHot(iter[0]).reshape(1,2)))
            flag = 1
          _, summary, cost, gstep = sess.run(
                  [train_op, merged, cross_entropy, global_step],
                  feed_dict={x: train_x, y_: train_y})
          elapsed_time = time.time() - start_time
          start_time = time.time()
          if(FLAGS.task_index == 0):
            train_writer.add_summary(summary, gstep)
          print("Step: %d," % (gstep),
                " Epoch: %2d," % (epoch),                            
                " Cost: %.4f," % cost,
                " Time: %3.2fms" % float(elapsed_time*1000))
        sys.stderr.write("reporter progress:%0.4f\n"%(float(epoch+1)/(training_epochs)))
  
      print "Train Completed."
      if(FLAGS.task_index == 0):
        train_writer.close()
        print "saving model..."
        saver.save(sess, FLAGS.save_path+"/model.ckpt")
    print("done")       

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
  tf.app.run(main=main)
