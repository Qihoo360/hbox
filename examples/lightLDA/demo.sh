echo $LIGHTLDA_ROLE
if [ "$LIGHTLDA_ROLE" = "ps" ]; then
  lightLDA/multiverso/bin/multiverso_server -serverid $LIGHTLDA_RANK -workers $LIGHTLDA_WORKER_NUM -endpoint $LIGHTLDA_SERVER_ENDPOINT
  mv server*.model output
else
  mv data/nytimes.libsvm.* data/nytimes.libsvm
  lightLDA/bin/dump_binary data/nytimes.libsvm dict/nytimes.word_id.dict data 0
  lightLDA/bin/lightlda -server_file lightLDAEndPoints.txt -num_servers $LIGHTLDA_SERVER_NUM -num_vocabs 111400 -num_topics 1000 -num_iterations 1 -alpha 0.1 -beta 0.01 -mh_steps 2 -num_local_workers 1 -num_blocks 1 -max_num_document 300000 -input_dir data -data_capacity 800
  mv doc_topic.* output
  mv LightLDA.*.log output
fi
