from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import argparse
import numpy as np
import tensorflow as tf

FLAGS = None

# Data sets
IRIS_TRAINING = "iris_training.csv"
IRIS_TEST = "iris_test.csv"

import os
import json

cluster = json.loads(os.environ["TF_CLUSTER_DEF"])
task_index = int(os.environ["TF_INDEX"])
task_type = os.environ["TF_ROLE"]

tf_config = dict()
worker_num = len(cluster["worker"])
if task_type == "ps":
    tf_config["task"] = {"index":task_index, "type":task_type}
elif task_type == "worker":
    if task_index == 0:
        tf_config["task"] = {"index":0, "type":"chief"}
    else:
        tf_config["task"] = {"index":task_index-1, "type":task_type}
elif task_type == "evaluator":
    tf_config["task"] = {"index":task_index, "type":task_type}

if worker_num == 1:
    cluster["chief"] = cluster["worker"]
    del cluster["worker"]
else:
    cluster["chief"] = [cluster["worker"][0]]
    del cluster["worker"][0]

tf_config["cluster"] = cluster
os.environ["TF_CONFIG"] = json.dumps(tf_config)
print(json.loads(os.environ["TF_CONFIG"]))


def main(_):

    # Load datasets.
    training_set = tf.contrib.learn.datasets.base.load_csv_with_header(
        filename=FLAGS.data_path + "/" + IRIS_TRAINING,
        target_dtype=np.int,
        features_dtype=np.float32)

    test_set = tf.contrib.learn.datasets.base.load_csv_with_header(
        filename=FLAGS.data_path + "/" + IRIS_TEST,
        target_dtype=np.int,
        features_dtype=np.float32)

    # Define the training inputs
    train_input_fn = tf.estimator.inputs.numpy_input_fn(
        x={"x": np.array(training_set.data)},
        y=np.array(training_set.target),
        num_epochs=None,
        shuffle=True)

    # Define the test inputs
    test_input_fn = tf.estimator.inputs.numpy_input_fn(
        x={"x": np.array(test_set.data)},
        y=np.array(test_set.target),
        num_epochs=1,
        shuffle=False)

    # Specify that all features have real-value data
    feature_columns = [tf.feature_column.numeric_column("x", shape=[4])]

    # Build 3 layer DNN with 10, 20, 10 units respectively.
    classifier = tf.estimator.DNNClassifier(
        config=tf.estimator.RunConfig(
           model_dir=FLAGS.model_path,
            save_checkpoints_steps = 50,
            save_summary_steps = 50,
            keep_checkpoint_max=5,
            log_step_count_steps=50
        ),
        feature_columns=feature_columns,
        hidden_units=[10, 20, 10],
        n_classes=3)

    tf.logging.set_verbosity(tf.logging.INFO)

    train_spec = tf.estimator.TrainSpec(input_fn=train_input_fn, max_steps=FLAGS.max_steps)
    eval_spec = tf.estimator.EvalSpec(input_fn=test_input_fn, steps=10, start_delay_secs=150, throttle_secs=200)

    tf.estimator.train_and_evaluate(classifier, train_spec, eval_spec)

    # Evaluate accuracy.
    accuracy_score = classifier.evaluate(input_fn=test_input_fn)["accuracy"]
    print("\nTest Accuracy: {0:f}\n".format(accuracy_score))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.register("type", "bool", lambda v: v.lower() == "true")

    # Flags for defining the parameter of data path
    parser.add_argument(
        "--data_path",
        type=str,
        default="./",
        help="The path for data"
    )
    parser.add_argument(
        "--model_path",
        type=str,
        default="./my_model",
        help="The save path for model"
    )

    # Flags for defining the parameter of train
    parser.add_argument(
        "--learning_rate",
        type=float,
        default=0.001,
        help="learning rate of the train"
    )
    parser.add_argument(
        "--max_steps",
        type=int,
        default=1000,
        help="the max_steps of the train"
    )

    FLAGS, unparsed = parser.parse_known_args()
    tf.app.run(main=main)
